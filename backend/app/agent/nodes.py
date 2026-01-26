import json
import re
from datetime import date
import threading
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from app.services.llm_service import llm_service
from app.agent.state import AgentState
from app.services.graph_crud import create_goal_in_db, generate_timeline, update_day_content
from app.services.graph_crud import get_tasks_for_date, mark_day_complete
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

# 1. THE ROUTER NODE
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

    # 🔴 FIXED: Use .get_response() instead of .model.generate_content()
    response_text = llm_service.get_response(classification_prompt)
    
    intent = response_text.strip().upper()
    print(f"🧠 ROUTER: {intent}")
    
    if "GOAL" in intent: return {"next_step": "goal_node"}
    elif "TASK" in intent: return {"next_step": "task_node"}
    else: return {"next_step": "chat_node"}

# 2. THE CHAT NODE
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
    
    # 🔴 FIXED: Use .get_response()
    response_text = llm_service.get_response(full_prompt)
    
    return {"messages": [AIMessage(content=response_text)]}

# --- HELPER: ROBUST JSON EXTRACTOR ---
def extract_json(text: str):
    try:
        clean_text = text.replace("```json", "").replace("```", "").strip()
        return json.loads(clean_text)
    except:
        match = re.search(r"\{.*\}", text, re.DOTALL)
        if match:
            clean_text = match.group(0)
            return json.loads(clean_text)
        else:
            raise ValueError("No JSON found in response")

# 3. THE GOAL NODE
def goal_node(state: AgentState):
    print("🔨 Entering Goal Node...")
    last_message = state["messages"][-1].content
    user_id = state.get("user_id", "test_user_123")
    
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
    
    # 🔴 FIXED: Use .get_response()
    raw_response = llm_service.get_response(extraction_prompt)
    
    try:
        # 1. Robust Extraction
        data = extract_json(raw_response)
        print(f"   📊 Extracted: {data}")
        
        # STEP B: Save to Neo4j
        new_goal = GoalCreate(title=data["title"], category=data.get("category", "General"))
        db_result = create_goal_in_db(user_id, new_goal)
        
        if db_result:
            goal_id = db_result["id"]
            
            # STEP C: Generate Timeline
            days_count = generate_timeline(goal_id, date.today(), data.get("days", 30))
            
            # STEP D: JUST-IN-TIME GENERATION
            print("🧠 Generating Day 1 Content...")
            try:
                # This function already handles rotation internally, so it's safe!
                first_day_content = llm_service.generate_day_topic(data["title"], 1)
                
                update_day_content(
                    goal_id, 
                    1, 
                    first_day_content["topic"], 
                    first_day_content["sub_tasks"]
                )
                print(f"   ✅ Day 1 Ready: {first_day_content['topic']}")
                
                def generate_day_2():
                    print("   🧠 Thread: Generating Day 2...")
                    try:
                        content_d2 = llm_service.generate_day_topic(data["title"], 2)
                        update_day_content(goal_id, 2, content_d2["topic"], content_d2["sub_tasks"])
                        print("   ✅ Day 2 Ready (Background)")
                    except Exception as e:
                        print(f"   ❌ Day 2 Failed: {e}")

                threading.Thread(target=generate_day_2).start()
            except Exception as e:
                print(f"   ⚠️ Day 1 Generation Failed: {e}")

            response_text = f"✅ Goal Created: **{data['title']}**\n📅 Timeline: {days_count} days created.\n\nI have prepared Day 1 for you. Check the map!"
        else:
            response_text = "❌ Database Error: Could not save goal."

    except Exception as e:
        print(f"❌ Extraction Failed: {e}")
        response_text = "I understood you want a goal, but I couldn't extract the details. Try saying: 'I want to learn X in Y days'."

    return {"messages": [AIMessage(content=response_text)]}

# The Task Node (LangGraph)
def task_node(state: AgentState):
    print("✅ Entering Task Node...")
    
    # 1. Get User Input from State (NOT request)
    user_msg = state["messages"][-1].content.lower()
    user_id = state.get("user_id", "test_user_123")
    today_str = date.today().isoformat()
    
    response_text = "I couldn't match that to a specific task. Try saying 'I finished the Python task'."

    # --- 2. INTENT DETECTION ---
    trigger_words = ["finish", "complete", "done with", "marked off"]
    
    if any(word in user_msg for word in trigger_words):
        print(f"DEBUG: Checking for completion intent in: '{user_msg}'")
        
        # A. Fetch Today's Tasks
        # Note: We pass today_str twice (target_date and today_date) for safety
        active_tasks = get_tasks_for_date(user_id, today_str, today_str)
        
        # B. Fuzzy Match
        matched_task = None
        for task in active_tasks:
            # Check Goal Title (e.g. "Python")
            if task.get('goal_title', '').lower() in user_msg:
                matched_task = task
                break
            # Check Task Title (e.g. "Watch Video")
            if task.get('title', '').lower() in user_msg:
                matched_task = task
                break
        
        # C. Execute Action
        if matched_task:
            if matched_task['type'] == 'GOAL':
                # Extract IDs
                day_num = matched_task['day_number'] 
                goal_id = matched_task['goal_id']
                
                # UPDATE DATABASE
                mark_day_complete(goal_id, day_num)
                
                response_text = f"Fantastic! I've marked '{matched_task['title']}' as complete. Your progress has been updated!"
                
            elif matched_task['type'] == 'SIDE_QUEST':
                # Future: Add logic for side quests
                response_text = "I see you finished a side quest! (Side Quest logic coming soon)"
        else:
             response_text = "I see you finished something, but I'm not sure which task on your list it is. Could you be more specific?"

    else:
        # If router sent us here but user didn't say "finish", maybe they are asking about tasks.
        # Fallback to general chat or list tasks.
        active_tasks = get_tasks_for_date(user_id, today_str, today_str)
        task_list = ", ".join([t['title'] for t in active_tasks])
        response_text = f"You are currently working on: {task_list}. Which one are you focusing on?"

    # 3. Return in LangGraph Format
    return {"messages": [AIMessage(content=response_text)]}