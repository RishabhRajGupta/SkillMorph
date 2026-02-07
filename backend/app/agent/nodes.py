from datetime import date
import datetime
import json
import random
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.messages import SystemMessage, ToolMessage, AIMessage
from app.agent.tools import ALL_TOOLS  # Ensure this imports your tools
from app.agent.state import AgentState
from app.core.config import settings

# --- HELPER: KEY ROTATION LOGIC ---
def get_llm_response_with_retry(messages, tools):
    """
    Tries to invoke the LLM using random keys. 
    If a key hits a Rate Limit (429), it instantly tries the next one.
    """
    # 1. Get keys and shuffle
    # Assuming settings.api_key_list is your list from config
    # If it's a comma-separated string, ensure you split it: settings.GEMINI_API_KEYS.split(",")
    keys = settings.api_key_list.copy() 
    random.shuffle(keys)

    last_error = None

    for api_key in keys:
        try:
            # 2. Instantiate LangChain Wrapper for THIS key
            # Note: 'gemini-2.5-flash' might not be standard yet. 
            # Usually it is 'gemini-1.5-flash' or 'gemini-2.0-flash-exp'.
            # Keeping your requested version string:
            llm = ChatGoogleGenerativeAI(
                model="gemini-2.5-flash", 
                google_api_key=api_key,
                temperature=0.3
            )
            
            # 3. Bind Tools
            llm_with_tools = llm.bind_tools(tools)
            
            # 4. Invoke
            response = llm_with_tools.invoke(messages)
            return response

        except Exception as e:
            error_str = str(e)
            if "429" in error_str or "ResourceExhausted" in error_str:
                print(f"⚠️ Key ...{api_key[-4:]} hit Rate Limit. Switching...")
                last_error = e
                continue # Try next key
            else:
                # If it's a logic error (not rate limit), raise it immediately
                print(f"❌ LLM Error: {e}")
                raise e
    
    # If we run out of keys
    raise Exception(f"All API keys exhausted. Last error: {last_error}")

# --- 1. THE AGENT NODE ---
def agent_node(state: AgentState):
    print("🤖 Agent Node Thinking...")
    messages = state["messages"]
    user_id = state.get("user_id", "test_user_123")
    
    system_prompt = f"""
    You are SkillMorph, an intelligent productivity agent companion and expert tutor.
    Current User ID: {user_id}
    Current Date: {date.today().isoformat()}  <-- GIVE IT TODAY'S DATE
    
    CAPABILITIES & PROTOCOLS:
    
    1. **MANAGING TASKS (COMPLETION):**
        Standard:
           i. On Completing a task
           - If the user says "I bought milk" or "Finished the python video":
           - FIRST, call `get_task_list_json` to see what is on the list.
           - SECOND, analyze the list to find the best semantic match (e.g., matching "bought milk" to "Buy Milk").
           - THIRD, call `confirm_task_completion` with the ID from the list.
           ii. Deleting
            - "Delete the milk task" -> `get_task_list_json` -> `delete_specific_task`
        Future: "I finished tomorrow's game dev task" -> 
         1. Calculate date: Tomorrow is { (date.today() + datetime.timedelta(days=1)).isoformat() }.
         2. Call `get_task_list_json(target_date='YYYY-MM-DD')`.
         3. The list will contain the 'day_number' and 'goal_id'.
         4. Call `confirm_task_completion` using those specific values from the list.
       
    2. **CREATING TASKS:**
       - "I want to learn X" -> `create_new_goal`
       - "Remind me to buy milk" -> `create_todo_item`
       - "Remind me to buy milk on Jan 30th" -> `create_todo_item(title="Buy milk", due_date="2026-01-30")`
       - ALWAYS convert relative dates (tomorrow, next week) to YYYY-MM-DD.
       
    3. 2. **MANAGING GOALS:**
       - "Create a goal to learn Python" -> `create_new_goal`
       - "Delete my Python goal" -> 
           1. Call `get_active_goals_json` to find the ID for "Python".
           2. Call `delete_specific_goal` with that ID.

    4. **MEMORY:**
       - If the user shares a personal fact ("I hate broccoli"), use `save_memory_note`.
       
    4. **GENERAL:**
       - Be concise.
       - ALWAYS pass the 'user_id' provided above.

    IMPORTANT:
    - Never guess IDs. Always fetch the list first.
    - ALWAYS pass the 'user_id'.
    - Dates must ALWAYS be YYYY-MM-DD.
    """
    
    # Prepend System Message to history
    # We create a temporary list so we don't mess up the actual state history permanently
    full_context = [SystemMessage(content=system_prompt)] + messages

    # 2. Call LLM with Rotation Logic
    try:
        response = get_llm_response_with_retry(full_context, ALL_TOOLS)
        return {"messages": [response]}
    except Exception as e:
        return {"messages": [AIMessage(content="I'm sorry, my brain is overloaded right now (Rate Limit). Please try again in a moment.")]}


# --- 2. THE TOOL NODE ---
def tool_node(state: AgentState):
    """
    Executes the tool calls requested by the Agent.
    """
    print("🔧 Tool Node Executing...")
    messages = state["messages"]
    last_message = messages[-1]

    # If no tool calls, this node shouldn't have been hit, but safe check:
    if not last_message.tool_calls:
        return {"messages": []}

    results = []
    
    # Iterate over all tool calls (Agent might want to do 2 things at once)
    for tool_call in last_message.tool_calls:
        action_name = tool_call["name"]
        tool_args = tool_call["args"]
        call_id = tool_call["id"]
        
        print(f"  👉 Calling: {action_name} with {tool_args}")

        # Find the matching tool object
        selected_tool = next((t for t in ALL_TOOLS if t.name == action_name), None)
        
        if selected_tool:
            try:
                # Execute the tool
                # Note: We rely on the Agent passing the user_id in 'tool_args'
                output = selected_tool.invoke(tool_args)
            except Exception as e:
                output = f"Error executing tool: {str(e)}"
        else:
            output = f"Error: Tool '{action_name}' not found."

        # Create the ToolMessage result
        results.append(ToolMessage(
            tool_call_id=call_id,
            name=action_name,
            content=str(output)
        ))

    return {"messages": results}