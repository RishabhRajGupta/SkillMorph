from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from app.agent.state import AgentState
from app.agent.nodes import router_node, chat_node, goal_node, task_node

# 1. Initialize Graph & Memory
workflow = StateGraph(AgentState)
memory = MemorySaver()

# 2. Add Nodes
workflow.add_node("router", router_node)
workflow.add_node("chat_node", chat_node)
workflow.add_node("goal_node", goal_node)
workflow.add_node("task_node", task_node)

# 3. Define Entry Point (Start at Router)
workflow.set_entry_point("router")

# 4. Define Conditional Edges (Router logic)
def route_decision(state):
    return state["next_step"]

workflow.add_conditional_edges(
    "router",
    route_decision,
    {
        "chat_node": "chat_node",
        "goal_node": "goal_node",
        "task_node": "task_node"
    }
)

# 5. Define End Edges (All experts finish the turn)
workflow.add_edge("chat_node", END)
workflow.add_edge("goal_node", END)
workflow.add_edge("task_node", END)

# 6. Compile
agent_app = workflow.compile(checkpointer=memory)