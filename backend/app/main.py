from fastapi import FastAPI, HTTPException, BackgroundTasks
from contextlib import asynccontextmanager
from app.core.config import settings
from app.services.neo4j_service import graph_db
from app.services.graph_crud import create_goal_in_db
from app.schemas.graph_models import GoalCreate
from datetime import date
from pydantic import BaseModel # Import this to define request body
from app.services.graph_crud import generate_timeline
from app.services.memory_service import memory_service
from app.agent.graph import agent_app
from langchain_core.messages import HumanMessage
from app.services.graph_crud import get_goal_roadmap, get_all_goals
from app.services.graph_crud import mark_day_complete, update_day_content, get_goal_roadmap
from app.services.graph_crud import get_tasks_for_date, create_side_quest
from app.services.llm_service import llm_service

class MemoryCreate(BaseModel):
    text: str

class ChatRequest(BaseModel):
    message: str
    is_voice_mode: bool = False
    user_id: str = "test_user_123" #Placeholder for now
    session_id: str = "default_session" #Placeholder for now
    
@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Connect to Neo4j
    print("🚀 System Booting...")
    graph_db.connect()
    yield
    # Shutdown: Close Neo4j connection
    print("🛑 System Shutting Down...")
    graph_db.close()


# Initialize App with settings from config.py
app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    lifespan=lifespan
)

@app.get("/")
async def health_check():
    """
    Root endpoint to test if the server is running.
    """
    return {
        "status": "alive",
        "system": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "mode": "Tier 1 Architecture",
        "database": "Neo4j Connected" if graph_db.driver else "Neo4j Not Connected"
    }

@app.post("/goals/")
def create_goal_endpoint(goal: GoalCreate):
    # For now, we hardcode a user Id since we don't have login yet
    user_id = "test_user_123"
    result = create_goal_in_db(user_id, goal)
    return {"message": "Goal Created", "data": result}

@app.post("/goals/{goal_id}/timeline")
def create_timeline_endpoint(goal_id: str, days: int):
    # Generates a timeline starting today
    count = generate_timeline(goal_id, date.today(), days)
    return {"message": "Timeline Generated", "days_created": count}

@app.post("/memory/")
def save_memory_endpoint(memory: MemoryCreate):
    result = memory_service.save_memory(memory.text)
    return result

@app.get("/memory/search")
def search_memory_endpoint(query: str):
    results = memory_service.search_memory(query)
    return {"matches": results}

@app.post("/agent/chat")
async def chat_endpoint(request: ChatRequest):
    """
    The main entry point for the Android App.
    It passes the user's message into the LangGraph Agent
    """
    try:
        #Prepare the input for the Graph
        inputs = {
            "messages": [HumanMessage(content=request.message)],
            "is_voice_mode": request.is_voice_mode
        }
        config = {"configurable": {"thread_id": request.session_id}}

        # Run the Agent (Thinks, Decides, Acts)
        result = await agent_app.ainvoke(inputs, config=config)

        # Extract the final response from the AI
        last_msg = result["messages"][-1]
        response_text = last_msg.content

        return {
            "response": response_text,
            "mode": "voice" if request.is_voice_mode else "text"
        }
    
    except Exception as e:
        print(f"❌ Agent Error: {e}")
        raise HTTPException(status_code=500, detail="Agent processing failed: str{e}")
    
@app.get("/")
def home():
    return {"status": "alive", "system": "SkillMorph Brain"}
    

# Get All goals (For the Goals List Screen)
@app.get("/goals")
def get_goals_endpoint(user_id: str = "test_user_123"):
    return get_all_goals(user_id)

# Get Specific Map (Metro Map)
@app.get("/goals/{goal_id}/roadmap")
def get_roadmap_endpoint(goal_id: str):
    data = get_goal_roadmap(goal_id)
    if not data:
        raise HTTPException(status_code=404, detail="Goal not found")
    return data

@app.post("/goals/{goal_id}/days/{day_number}/complete")
def complete_day_endpoint(
    goal_id: str, 
    day_number: int, 
    background_tasks: BackgroundTasks # <--- CRITICAL INJECTION
):
    # 1. Mark current day as done (Instant UI update)
    new_progress = mark_day_complete(goal_id, day_number)
    
    # 2. Trigger AI for the NEXT day (Day X + 1)
    # This runs AFTER the response is sent, so the app doesn't freeze.
    next_day = day_number + 1
    background_tasks.add_task(run_content_generation, goal_id, next_day)
    
    return {"status": "success", "new_progress": new_progress}

# Define the Background Job
def run_content_generation(goal_id: str, day_number: int):
    print(f"🧠 BACKGROUND JOB: Generating content for Goal {goal_id}, Day {day_number}...")
    
    # 1. Get Goal Title (to give context to AI)
    goal_data = get_goal_roadmap(goal_id)
    if not goal_data: 
        print("   ❌ Goal not found, aborting generation.")
        return
    
    title = goal_data["title"]
    
    # 2. Ask AI to write the lesson plan
    try:
        content = llm_service.generate_day_topic(title, day_number)
        
        # 3. Save to Neo4j (Overwrite "Pending Generation")
        update_day_content(goal_id, day_number, content["topic"], content["sub_tasks"])
        print(f"   ✅ SUCCESS: Day {day_number} content saved!")
        
    except Exception as e:
        print(f"   ❌ AI Generation Failed: {e}")

# Update the Complete Endpoint
@app.post("/goals/{goal_id}/days/{day_number}/complete")
def complete_day_endpoint(goal_id: str, day_number: int, background_tasks: BackgroundTasks):
    # A. Mark current day done & unlock next
    new_progress = mark_day_complete(goal_id, day_number)
    
    # B. Trigger AI to write content for the NEXT day (day_number + 1)
    # The user won't see this immediately, but by the time they click "Next", it will be there.
    background_tasks.add_task(run_content_generation, goal_id, day_number + 1)
    
    return {"status": "success", "new_progress": new_progress}

# Tasks
@app.get("/tasks/today")
def get_tasks_endpoint(date: str, user_id: str = "test_user_123"):
    return get_tasks_for_date(user_id, date)

class TaskCreate(BaseModel):
    title: str
    date: str

@app.post("/tasks")
def create_task_endpoint(task: TaskCreate, user_id: str = "test_user_123"):
    create_side_quest(user_id, task.title, task.date)
    return {"status": "created"}

@app.post("/tasks/{task_id}/complete")
def complete_side_quest_endpoint(task_id: str):
    query = """
    MATCH (t:Task {id: $task_id})
    SET t.is_completed = true
    RETURN t.id
    """
    with graph_db.get_session() as session:
        session.run(query, task_id=task_id)
    return {"status": "completed"}