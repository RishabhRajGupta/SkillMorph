import sys
import os
sys.path.append(os.getcwd())

from app.agent.graph import agent_app
from langchain_core.messages import HumanMessage

def test_brain(user_input):
    print(f"\n👤 User: {user_input}")
    
    # Run the graph
    inputs = {"messages": [HumanMessage(content=user_input)]}
    result = agent_app.invoke(inputs)
    
    # Print the last message from the AI
    last_msg = result["messages"][-1]
    print(f"🤖 Agent: {last_msg[1] if isinstance(last_msg, tuple) else last_msg.content}")

if __name__ == "__main__":
    # Test 1: Chat
    test_brain("Hello, who are you?")
    
    # Test 2: Goal
    test_brain("I want to learn backend engineering in 30 days.")
    
    # Test 3: Task
    test_brain("I finished the tutorial video.")