from fastapi import FastAPI, HTTPException
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

class MemoryCreate(BaseModel):
    text: str

class ChatRequest(BaseModel):
    message: str
    is_voice_mode: bool = False
    uesr_id: str = "test_user_123" #Placeholder for now

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

        # Run the Agent (Thinks, Decides, Acts)
        result = await agent_app.ainvoke(inputs)

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
    