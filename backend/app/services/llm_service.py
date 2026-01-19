import google.generativeai as genai
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from app.core.config import settings

# 1. Configure the Stable SDK
genai.configure(api_key=settings.GEMINI_API_KEY)

class LLMService:
    def __init__(self):
        # We use LangChain for embeddings because it handles the complex math for us
        self.embeddings = GoogleGenerativeAIEmbeddings(
            model="models/text-embedding-004",
            google_api_key=settings.GEMINI_API_KEY
        )
        # We use the standard client for chat
        self.model = genai.GenerativeModel('gemini-2.5-flash')

    def get_embedding(self, text: str) -> list[float]:
        """Turns text into a vector."""
        return self.embeddings.embed_query(text)

    def summarize_memory(self, text: str) -> str:
        """Compresses user chatter into a concise fact."""
        prompt = f"""
        Extract the core fact about the user from this text. 
        Keep it under 10 words. 
        Text: "{text}"
        Fact:
        """
        try:
            response = self.model.generate_content(prompt)
            return response.text.strip()
        except Exception as e:
            print(f"❌ Gemini Error: {e}")
            return "Could not summarize."
        
    def generate_day_topic(self, goal_title: str, day_number: int, context: str = "") -> dict:
        """
        Generates a specific title and subtasks for a single day.
        """
        prompt = f"""
        You are a syllabus generator. Create a lesson plan for Day {day_number} of the goal: '{goal_title}'.
        Previous context: {context}
        
        OUTPUT JSON ONLY:
        {{
            "topic": "Short Title (e.g. Variables & Data Types)",
            "sub_tasks": ["Read X", "Practice Y", "Build Z"]
        }}
        """
        try:
            # We use the raw generation (assuming you have a method for it, or use the existing one)
            response = self.model.generate_content(prompt)
            # Simple cleanup of markdown
            clean_text = response.text.replace("```json", "").replace("```", "").strip()
            import json
            return json.loads(clean_text)
        except Exception as e:
            print(f"⚠️ LLM Generation Failed: {e}")
            return {
                "topic": f"Day {day_number}: Learning Session",
                "sub_tasks": ["Study the material", "Practice", "Review"]
            }
        
             
llm_service = LLMService()