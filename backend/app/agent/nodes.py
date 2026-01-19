import json
import re
from datetime import date
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from app.services.llm_service import llm_service
from app.agent.state import AgentState
from app.services.graph_crud import create_goal_in_db, generate_timeline, update_day_content
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

# --- HELPER: ROBUST JSON EXTRACTOR ---
def extract_json(text: str):
    """
    Finds a JSON object inside a string using Regex.
    Solves the issue where Gemini says "Here is the JSON: {...}"
    """
    try:
        # 1. Try standard parsing first
        # Clean potential markdown wrappers
        clean_text = text.replace("```json", "").replace("```", "").strip()
        return json.loads(clean_text)
    except:
        # 2. Use Regex to find the first '{' and the last '}'
        match = re.search(r"\{.*\}", text, re.DOTALL)
        if match:
            clean_text = match.group(0)
            return json.loads(clean_text)
        else:
            raise ValueError("No JSON found in response")

# 3. THE GOAL NODE (The Builder)
def goal_node(state: AgentState):
    print("🔨 Entering Goal Node...")
    last_message = state["messages"][-1].content
    user_id = "test_user_123" 
    
    # STEP A: Extract Data using Gemini
    extraction_prompt = f"""
    Extract goal details from this text: "{last_message}"
    
    Return VALID JSON only:
    {{
        "title": "Short title (e.g. Learn Python)",
        "category": "One word category",
        "days": 30
    }}
    """
    raw_response = llm_service.model.generate_content(extraction_prompt).text
    
    try:
        # 1. Robust Extraction
        data = extract_json(raw_response)
        print(f"   📊 Extracted: {data}")
        
        # STEP B: Save to Neo4j
        new_goal = GoalCreate(title=data["title"], category=data.get("category", "General"))
        db_result = create_goal_in_db(user_id, new_goal)
        
        if db_result:
            goal_id = db_result["id"]
            
            # STEP C: Generate Timeline (Skeleton)
            # Creates 30 nodes with "Pending Generation"
            days_count = generate_timeline(goal_id, date.today(), data.get("days", 30))
            
            # 🚀 STEP D: JUST-IN-TIME GENERATION (Day 1)
            # Immediately fill in Day 1 so the user doesn't see an empty map
            print("🧠 Generating Day 1 Content...")
            try:
                first_day_content = llm_service.generate_day_topic(data["title"], 1)
                
                update_day_content(
                    goal_id, 
                    1, 
                    first_day_content["topic"], 
                    first_day_content["sub_tasks"]
                )
                print(f"   ✅ Day 1 Ready: {first_day_content['topic']}")
            except Exception as e:
                print(f"   ⚠️ Day 1 Generation Failed (Background job will handle it later): {e}")

            response_text = f"✅ Goal Created: **{data['title']}**\n📅 Timeline: {days_count} days created.\n\nI have prepared Day 1 for you. Check the map!"
        else:
            response_text = "❌ Database Error: Could not save goal."

    except Exception as e:
        print(f"❌ Extraction Failed: {e}")
        response_text = "I understood you want a goal, but I couldn't extract the details. Try saying: 'I want to learn X in Y days'."

    return {"messages": [AIMessage(content=response_text)]}

# 4. THE TASK NODE (Simple Mock for now)
def task_node(state: AgentState):
    return {"messages": [AIMessage(content="I've noted your task update! (Feature coming in Phase 4)")]}