import json
from datetime import date
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from app.services.llm_service import llm_service
from app.agent.state import AgentState
from app.services.graph_crud import create_goal_in_db, generate_timeline
from app.schemas.graph_models import GoalCreate

# --- HELPER: SYSTEM PROMPTS ---
def get_system_prompt(is_voice: bool):
    if is_voice:
        return """
        You are SkillMorph, a voice assistant.
        OUTPUT FORMAT: You must reply in JSON with two fields:
        {
            "spoken_text": "Short, conversational summary (max 2 sentences) for TTS.",
            "display_text": "Detailed markdown version for the screen."
        }
        DO NOT output raw markdown. ONLY valid JSON.
        """
    else:
        return """
        You are SkillMorph, an expert AI coach.
        Use Markdown formatting (bolding, lists).
        Be encouraging but concise.
        """

# 1. THE ROUTER NODE (Kept the same)
def router_node(state: AgentState):
    messages = state["messages"]
    last_message = messages[-1].content
    
    classification_prompt = f"""
    Classify the following user input into exactly one of these categories:
    - 'GOAL_CREATION': User wants to start a new project, skill, or habit. Only opt this when you feel that this is needed. You can also ask the user if they want a goal to be created for that. 
    - 'TASK_UPDATE': User is reporting they finished something or asking about tasks.
    - 'GENERAL_CHAT': Everything else (greetings, questions, random thoughts).
    
    User Input: "{last_message}"
    
    Reply ONLY with the category name.
    """

    response = llm_service.model.generate_content(classification_prompt)
    intent = response.text.strip().upper()
    print(f"🧠 ROUTER: {intent}")
    
    if "GOAL" in intent: return {"next_step": "goal_node"}
    elif "TASK" in intent: return {"next_step": "task_node"}
    else: return {"next_step": "chat_node"}

# 2. THE CHAT NODE (General Conversation)
def chat_node(state: AgentState):
    print("✨ Entering Chat Node...")
    messages = state["messages"]
    is_voice = state.get("is_voice_mode", False)
    
    # 1. Get History (Last 5 messages to save tokens)
    history = [m.content for m in messages[-5:]]
    
    # 2. Build Prompt
    system_instruction = get_system_prompt(is_voice)
    full_prompt = f"""
    {system_instruction}
    
    CONVERSATION HISTORY:
    {history}
    
    Reply to the user:
    """
    
    # 3. Generate
    response = llm_service.model.generate_content(full_prompt)
    return {"messages": [AIMessage(content=response.text)]}

# 3. THE GOAL NODE (The Builder)
def goal_node(state: AgentState):
    print("🔨 Entering Goal Node...")
    last_message = state["messages"][-1].content
    user_id = "test_user_123" # Hardcoded for now
    
    # STEP A: Extract Data using Gemini
    extraction_prompt = f"""
    Extract goal details from this text: "{last_message}"
    
    Return VALID JSON only:
    {{
        "title": "Short title (e.g. Learn Python)",
        "category": "One word category",
        "days": 30 (Default to 30 if not specified)
    }}
    """
    raw_json = llm_service.model.generate_content(extraction_prompt).text
    
    # Clean up JSON (Gemini sometimes adds ```json ... ``` wrappers)
    clean_json = raw_json.replace("```json", "").replace("```", "").strip()
    
    try:
        data = json.loads(clean_json)
        print(f"   📊 Extracted: {data}")
        
        # STEP B: Save to Neo4j
        new_goal = GoalCreate(title=data["title"], category=data["category"])
        db_result = create_goal_in_db(user_id, new_goal)
        
        if db_result:
            goal_id = db_result["id"]
            
            # STEP C: Generate Timeline
            days_count = generate_timeline(goal_id, date.today(), data["days"])
            
            response_text = f"✅ Goal Created: **{data['title']}**\n📅 Timeline: Generated {days_count} daily steps."
        else:
            response_text = "❌ Database Error: Could not save goal."

    except Exception as e:
        print(f"❌ Extraction Failed: {e}")
        response_text = "I understood you want a goal, but I couldn't extract the details. Try saying: 'I want to learn X in Y days'."

    return {"messages": [AIMessage(content=response_text)]}

# 4. THE TASK NODE (Simple Mock for now)
def task_node(state: AgentState):
    return {"messages": [AIMessage(content="I've noted your task update! (Feature coming in Phase 4)")]}