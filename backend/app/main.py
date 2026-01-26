import datetime # <--- 1. Clean Import (Keep only this)
from fastapi import FastAPI, HTTPException, BackgroundTasks, Header, Depends
from contextlib import asynccontextmanager
from pydantic import BaseModel
from typing import Optional

# App Imports
from app.core.config import settings
from app.services.neo4j_service import graph_db
from app.services.graph_crud import (
    create_goal_in_db, generate_timeline, get_all_goals, 
    get_goal_roadmap, mark_day_complete, update_day_content, 
    get_tasks_for_date, create_side_quest
)
from app.services.chat_session_service import (
    get_or_create_daily_session, 
    save_message_to_session, 
    get_session_history,
    get_user_sessions
)
from app.services.memory_service import memory_service
from app.services.llm_service import llm_service
from app.schemas.graph_models import GoalCreate
from app.agent.graph import agent_app
from langchain_core.messages import HumanMessage

# --- MODELS ---
class MemoryCreate(BaseModel):
    text: str

class ChatRequest(BaseModel):
    message: str
    is_voice_mode: bool = False
    # We remove user_id from here because we trust the Header now
    session_id: str = "default_session"

class TaskCreate(BaseModel):
    title: str
    date: str

# --- LIFECYCLE ---
@asynccontextmanager
async def lifespan(app: FastAPI):
    print("🚀 System Booting...")
    graph_db.connect()
    yield
    print("🛑 System Shutting Down...")
    graph_db.close()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    lifespan=lifespan
)

# --- AUTH DEPENDENCY ---
def get_current_user(x_user_id: str = Header(None)):
    """
    Reads the 'x-user-id' header sent by Android.
    If missing, falls back to test user (for testing in browser).
    """
    if not x_user_id:
        return "test_user_123" 
    return x_user_id

# --- ENDPOINTS ---

@app.get("/")
def home():
    return {"status": "alive", "system": "SkillMorph Brain"}

# 1. GOALS
@app.get("/goals")
def get_goals_endpoint(user_id: str = Depends(get_current_user)): # <--- UPDATED
    return get_all_goals(user_id)

@app.post("/goals")
def create_goal_endpoint(goal: GoalCreate, background_tasks: BackgroundTasks, user_id: str = Depends(get_current_user)): # <--- UPDATED
    result = create_goal_in_db(user_id, goal)
    goal_id = result["id"]

    # Generate Timeline Immediately (Sync) so user sees it
    generate_timeline(result["id"], datetime.date.today(), goal.days)
    
    background_tasks.add_task(run_content_generation, goal_id, 2)

    ai_message = (
        f"That sounds like a fantastic challenge! I've created the goal '{goal.title}' "
        f"for you. It's a {goal.days}-day journey, and I've already set up the first steps. "
        f"Good luck!"
    )
    return {"message": ai_message, "data": result}

@app.get("/goals/{goal_id}/roadmap")
def get_roadmap_endpoint(goal_id: str):
    data = get_goal_roadmap(goal_id)
    if not data:
        raise HTTPException(status_code=404, detail="Goal not found")
    return data

# 2. TASKS (Daily View)
@app.get("/tasks/today") 
def get_tasks_endpoint(date: str, user_id: str = Depends(get_current_user)): # <--- UPDATED
    today_str = datetime.date.today().isoformat()
    return get_tasks_for_date(user_id, date, today_str)

@app.post("/tasks")
def create_task_endpoint(task: TaskCreate, user_id: str = Depends(get_current_user)): # <--- UPDATED
    create_side_quest(user_id, task.title, task.date)
    return {"status": "created"}

@app.post("/tasks/{task_id}/complete")
def complete_side_quest_endpoint(task_id: str):
    query = "MATCH (t:Task {id: $task_id}) SET t.is_completed = true RETURN t.id"
    with graph_db.get_session() as session:
        session.run(query, task_id=task_id)
    return {"status": "completed"}

# 3. COMPLETION & BACKGROUND GEN
# Define the Background Job Function
def run_content_generation(goal_id: str, day_number: int):
    print(f"🧠 BACKGROUND JOB: Generating content for Goal {goal_id}, Day {day_number}...")
    goal_data = get_goal_roadmap(goal_id)
    if not goal_data: return
    
    try:
        content = llm_service.generate_day_topic(goal_data["title"], day_number)
        update_day_content(goal_id, day_number, content["topic"], content["sub_tasks"])
        print(f"   ✅ SUCCESS: Day {day_number} content saved!")
    except Exception as e:
        print(f"   ❌ AI Generation Failed: {e}")

@app.post("/goals/{goal_id}/days/{day_number}/complete")
def complete_day_endpoint(
    goal_id: str, 
    day_number: int, 
    background_tasks: BackgroundTasks
):

    # 2. Mark current day done (Pass today_str!)
    new_progress = mark_day_complete(goal_id, day_number)
    
    # 3. Trigger AI for the NEXT day
    background_tasks.add_task(run_content_generation, goal_id, day_number + 2)
    
    return {"status": "success", "new_progress": new_progress}

# 4. MEMORY & CHAT
@app.post("/memory/")
def save_memory_endpoint(memory: MemoryCreate):
    result = memory_service.save_memory(memory.text)
    return result

@app.get("/memory/search")
def search_memory_endpoint(query: str):
    results = memory_service.search_memory(query)
    return {"matches": results}

@app.post("/agent/chat")
async def chat_endpoint(request: ChatRequest, user_id: str = Depends(get_current_user)):
    try:
        # 1. GET TODAY'S SESSION (Virtual Date Logic)
        if request.session_id == "default_session" or not request.session_id:
             session_data = get_or_create_daily_session(user_id)
             session_id = session_data["session_id"]
        else:
             session_id = request.session_id
             
        # 2. SAVE USER MESSAGE
        save_message_to_session(session_id, "user", request.message)

        # 3. RUN AGENT
        inputs = {
            "messages": [HumanMessage(content=request.message)],
            "is_voice_mode": request.is_voice_mode,
            "user_id": user_id 
        }
        config = {"configurable": {"thread_id": session_id}}

        result = await agent_app.ainvoke(inputs, config=config)
        
        # 4. SAVE AI RESPONSE (Sanitized)
        last_msg = result["messages"][-1]
        raw_content = last_msg.content
        
        # 🔴 FIX: Sanitize content to ensure it is a simple String for Neo4j
        response_text = ""
        
        if isinstance(raw_content, str):
            response_text = raw_content
        elif isinstance(raw_content, list):
            # Handle list of content parts (e.g. text + citations)
            texts = []
            for part in raw_content:
                if isinstance(part, dict) and "text" in part:
                    texts.append(part["text"])
                elif isinstance(part, str):
                    texts.append(part)
                else:
                    texts.append(str(part))
            response_text = " ".join(texts)
        elif isinstance(raw_content, dict):
             # Handle the specific Map error you saw
             response_text = raw_content.get("text", str(raw_content))
        else:
            # Final fallback
            response_text = str(raw_content)

        # Now save the clean string, preventing the Neo4j TypeError
        save_message_to_session(session_id, "ai", response_text)

        return {
            "response": response_text,
            "mode": "voice" if request.is_voice_mode else "text",
            "session_id": session_id 
        }

    except Exception as e:
        print(f"❌ Agent Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    
# --- 1. START TODAY'S SESSION (Used by AgentViewModel init) ---
@app.post("/chat/sessions/today") 
def get_daily_session_endpoint(user_id: str = Depends(get_current_user)):
    """
    Checks the clock (3:30 AM rule).
    Returns the Session ID for 'Virtual Today'.
    """
    return get_or_create_daily_session(user_id)

# --- 2. GET MESSAGES (Used by init & sidebar clicks) ---
@app.get("/chat/sessions/{session_id}/history")
def get_session_history_endpoint(session_id: str):
    """
    Returns the list of chat messages for a specific session.
    """
    print("inside session: " + session_id)
    return get_session_history(session_id)

# --- 3. GET SIDEBAR LIST (Used by Sidebar) ---
@app.get("/chat/sessions")
def get_sessions_list_endpoint(user_id: str = Depends(get_current_user)):
    """
    Returns a list of ALL past sessions (Date + Title).
    """
    return get_user_sessions(user_id)
