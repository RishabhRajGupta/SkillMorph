import google.generativeai as genai
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from app.core.config import settings
import random
import json
import time

class LLMService:
    def __init__(self):
        # 1. Load keys safely
        # We read the raw string from your existing settings
        raw_keys = getattr(settings, "GEMINI_API_KEYS", "")
        
        if not raw_keys:
            print("❌ CRITICAL ERROR: GEMINI_API_KEYS is missing in .env or config.py")
            self.keys = []
        else:
            # Handle comma-separated string
            self.keys = [k.strip() for k in raw_keys.split(",") if k.strip()]
            
        print(f"✅ LLMService loaded {len(self.keys)} API key(s).")

    def _generate_text_safe(self, prompt: str, default: str) -> str:
        """
        Internal helper: Tries to generate text using random keys.
        Retries automatically if a key hits the Rate Limit (429).
        """
        keys_to_try = self.keys.copy()
        random.shuffle(keys_to_try)

        for api_key in keys_to_try:
            try:
                # Configure specifically for this request
                genai.configure(api_key=api_key)
                model = genai.GenerativeModel('gemini-2.5-flash')
                response = model.generate_content(prompt)
                return response.text.strip()
            except Exception as e:
                error_str = str(e)
                if "429" in error_str or "ResourceExhausted" in error_str:
                    print(f"⚠️ Rate Limit on key ...{api_key[-4:]}. Switching keys...")
                    continue # Try next key
                else:
                    print(f"❌ Generation Error: {e}")
                    # If it's a logic error (not network/limit), stop trying
                    break 
        
        return default

    def get_response(self, prompt: str) -> str:
        """
        Public method for Chat/Agent to get a simple text response.
        """
        return self._generate_text_safe(prompt, default="I am sorry, I cannot reply right now.")

    def get_embedding(self, text: str) -> list[float]:
        """Get embeddings with simple retry logic."""
        keys_to_try = self.keys.copy()
        random.shuffle(keys_to_try)

        for api_key in keys_to_try:
            try:
                # Re-initialize for this specific key
                embedder = GoogleGenerativeAIEmbeddings(
                    model="models/text-embedding-004",
                    google_api_key=api_key
                )
                return embedder.embed_query(text)
            except Exception as e:
                print(f"⚠️ Embedding failed with key ...{api_key[-4:]}: {e}")
                continue 
        
        print("❌ All keys failed for embedding.")
        return []

    def summarize_memory(self, text: str) -> str:
        prompt = f"Summarize this user info in under 10 words: {text}"
        return self._generate_text_safe(prompt, default="User info processed.")

    def generate_day_topic(self, goal_title: str, day_number: int, context: str = "") -> dict:
        prompt = f"""
        Create a lesson plan for Day {day_number} of '{goal_title}'.
        Context: {context}
        OUTPUT JSON: {{"topic": "Title", "sub_tasks": ["Task1", "Task2"]}}
        """
        
        response_text = self._generate_text_safe(prompt, default="")
        
        try:
            clean_text = response_text.replace("```json", "").replace("```", "").strip()
            return json.loads(clean_text)
        except:
            print(f"⚠️ Failed to parse JSON. Raw text: {response_text}")
            return {
                "topic": f"Day {day_number}: {goal_title}",
                "sub_tasks": ["Research topic", "Practice basics", "Review notes"]
            }

# Singleton Instance
llm_service = LLMService()