from langchain_core.tools import tool
from app.services.graph_crud import (
    create_goal_in_db, create_smart_timeline, get_tasks_for_date, 
    mark_day_complete, update_day_content, create_side_quest,
    delete_task_from_db, delete_goal_from_db, get_all_goals,
    get_goal_roadmap, mark_side_quest_complete
)
from app.services.memory_service import memory_service  # Ensure this service exists
from app.schemas.graph_models import GoalCreate
from app.services.llm_service import llm_service
from datetime import date
import threading
import json

# --- EXISTING TOOLS (Keep create_new_goal & get_todays_tasks) ---

@tool
def create_new_goal(user_id: str, title: str, category: str, context: str = ""):
    """
    Creates a smart learning roadmap.
    Use this when the user wants to learn something.
    
    Args:
        title: The name of the skill (e.g., "Python", "Public Speaking").
        category: The domain (e.g., "Coding", "Soft Skills").
        context: Optional details about why they want to learn it (e.g., "for an interview").
    """
    print(f"🛠️ TOOL: Creating Smart Goal '{title}'...")
    
    # 1. Create the Goal Node (Meta Data)
    # We pass 30 as a dummy value for 'days' just to satisfy the Schema, 
    # but the AI will decide the real length.
    new_goal = GoalCreate(title=title, category=category, days=30)
    result = create_goal_in_db(user_id, new_goal)
    
    if not result:
        return "Error: Could not create goal in database."

    goal_id = result["id"]

    # 2. Generate the Smart Schedule (The Brain)
    # This gets the full list of topics, subtasks, and bins them into days.
    # We assume a standard 60-minute daily pace for now.
    schedule = llm_service.generate_smart_roadmap(
        goal_title=title, 
        user_context=context, 
        daily_minutes=60
    )

    if not schedule:
        return f"Error: I couldn't generate a roadmap for '{title}'. Please try again."

    # 3. Save the Timeline (The Database)
    # This creates all the nodes with specific subtasks immediately.
    total_days = create_smart_timeline(goal_id, schedule, date.today())

    return (
        f"Goal '{title}' created successfully! "
        f"Based on the complexity, I have designed a {total_days}-day roadmap for you. "
        f"Check your Metro Map to see the tasks."
    )

@tool
def create_todo_item(user_id: str, title: str, due_date: str = None):
    """
    Creates a single one-off task (Side Quest).
    Use this for simple chores like 'Buy milk', 'Call mom', 'Email boss'.
    
    IMPORTANT: 'due_date' MUST be in 'YYYY-MM-DD' format (e.g., '2026-01-30').
    If the user says "next Friday" or "Jan 30", YOU must calculate the date.
    If no date is specified, use today's date.
    """
    if not due_date:
        due_date = date.today().isoformat()
        
    # Optional: Safety check to prevent "January 30th" from slipping through
    try:
        # This checks if it's valid ISO format
        date.fromisoformat(due_date)
    except ValueError:
        # Fallback: If LLM messed up, default to today or log error
        print(f"⚠️ Tool Error: Invalid date format '{due_date}'. Defaulting to today.")
        due_date = date.today().isoformat()
        
    create_side_quest(user_id, title, due_date)
    return f"Added task '{title}' to your list for {due_date}."

# --- NEW: SMARTER TOOLS ---

@tool
def get_task_list_json(user_id: str, target_date: str = None):
    """
    INTERNAL USE. Returns the raw JSON list of tasks for a SPECIFIC DATE.
    
    ARGS:
    - target_date: (Optional) YYYY-MM-DD. 
      - If user says "delete task on Jan 30th", pass '2026-01-30'.
      - If user says "delete my task" (implied today), pass None (defaults to today).
    """
    if not target_date:
        today_str = date.today().isoformat()
        target_date = today_str # Default to today
    else:
        # Pass today's date as the 3rd arg so Pacing Logic (shifting) still works correctly
        today_str = date.today().isoformat()

    # We use the same 'get_tasks_for_date' function, but now we can look at the future
    tasks = get_tasks_for_date(user_id, target_date, today_str)
    
    if not tasks:
        return json.dumps([])

    # Return structured list so Agent can find the ID
    return json.dumps([{
        "id": t['id'], 
        "title": t['title'], 
        "goal": t.get('goal_title', 'Side Quest'),
        "type": t['type'],
        "goal_id": t.get('goal_id'),       
        "day_number": t.get('day_number'), 
        "scheduled_date": target_date 
    } for t in tasks])

@tool
def confirm_task_completion(user_id: str, task_id: str, task_type: str, goal_id: str = None, day_number: int = None):
    """
    Marks a specific task as complete. 
    """
    if task_type == 'GOAL':
        if not goal_id or not day_number:
            return "Error: Goal ID and Day Number required for Goal Tasks."
            
        # 1. Mark Database as Complete
        mark_day_complete(goal_id, day_number)
        
        # 2. 🔴 NEW: Trigger Rolling Generation (Background Thread)
        # Just like the API endpoint, we generate content for (Current Day + 2)
        def run_rolling_gen():
            next_day_target = day_number + 2
            print(f"🧵 Tool: Triggering generation for Day {next_day_target}...")
            
            try:
                # We need the goal title to generate context
                goal_data = get_goal_roadmap(goal_id)
                if not goal_data: 
                    return

                title = goal_data["title"]
                
                # Generate content using LLM
                content = llm_service.generate_day_topic(title, next_day_target)
                
                # Save to DB
                update_day_content(goal_id, next_day_target, content["topic"], content["sub_tasks"])
                print(f"✅ Tool: Day {next_day_target} content ready.")
            except Exception as e:
                print(f"❌ Tool Gen Error: {e}")

        # Start non-blocking thread
        threading.Thread(target=run_rolling_gen).start()

        return "Goal task marked complete! I've also started preparing the content for upcoming days."
        
    elif task_type == 'SIDE_QUEST':
        mark_side_quest_complete(task_id) 
        return "Side quest marked complete."
        
    return "Error: Unknown task type."

@tool
def save_memory_note(user_id: str, content: str):
    """
    Saves important user details to long-term memory.
    Use this when the user tells you about their preferences, job, or life events.
    Example: "I am vegan", "My dog's name is Rex".
    """
    # Assuming you have a memory service
    memory_service.save_memory(content)
    return "Memory saved."


@tool
def get_active_goals_json(user_id: str):
    """
    INTERNAL USE. Returns a JSON list of all active goals with their IDs.
    Use this to find the 'goal_id' when the user wants to delete or modify a goal.
    """
    goals = get_all_goals(user_id)
    # Simplify for the LLM (save tokens)
    simple_list = [{"title": g["title"], "id": g["id"]} for g in goals]
    return json.dumps(simple_list)

@tool
def delete_specific_goal(user_id: str, goal_id: str):
    """
    Permanently deletes a Goal and all its history.
    IMPORTANT: You must first call 'get_active_goals_json' to find the correct goal_id.
    """
    delete_goal_from_db(user_id, goal_id)
    return "Goal deleted successfully."

@tool
def delete_specific_task(user_id: str, task_id: str):
    """
    Permanently deletes a Side Quest (To-Do item).
    IMPORTANT: You must first call 'get_task_list_json' to find the correct task_id.
    """
    delete_task_from_db(user_id, task_id)
    return "Task deleted successfully."

# Updated Tool List
ALL_TOOLS = [
    create_new_goal, 
    create_todo_item, 
    get_task_list_json, # Replaces get_todays_tasks for internal logic
    confirm_task_completion,
    save_memory_note,
    get_active_goals_json,
    delete_specific_goal,
    delete_specific_task
]