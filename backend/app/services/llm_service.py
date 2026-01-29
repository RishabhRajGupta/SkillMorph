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
        

    def generate_smart_roadmap(self, goal_title: str, user_context: str = "", daily_minutes: int = 60) -> list:
        """
        Generates a complete, paced schedule.
        1. Asks LLM for granular topics + time estimates.
        2. Uses Python to pack them into days (Bin Packing).
        """
        
        # Step 1: Get Raw Topics from LLM
        prompt = f"""
        Act as an expert curriculum designer. Create a learning path for '{goal_title}'.
        Context: {user_context}
        
        Break this skill down into GRANULAR, bite-sized topics. 
        For EACH topic, provide:
        1. title: A short specific title (e.g. "React Hooks: useState")
        2. minutes: Estimated time for a beginner to grasp this concept and practice it.
        3. sub_tasks: 3-4 highly specific actionable steps (Read X, Build Y).
        
        CRITICAL INSTRUCTION: 
        - Do not think in "Days". Think in "Concepts".
        - Return a list of at least 15-20 concepts.
        - Output STRICT JSON only.
        
        JSON Format:
        [
          {{ "title": "Concept Name", "minutes": 45, "sub_tasks": ["Read docs", "Write code"] }},
          ...
        ]
        """
        
        response_text = self._generate_text_safe(prompt, default="[]")
        
        try:
            # Clean and Parse JSON
            clean_text = response_text.replace("```json", "").replace("```", "").strip()
            topics = json.loads(clean_text)
            
            # Step 2: Run the Bin Packing Scheduler
            return self._schedule_topics(topics, daily_minutes)
            
        except json.JSONDecodeError:
            print(f"❌ JSON Parse Error. Raw: {response_text}")
            return [] # Fail gracefully

    def _schedule_topics(self, topics: list, daily_limit: int) -> list:
        """
        Internal Scheduler Algorithm (Bin Packing).
        Groups topics into Days so that no day exceeds the daily_limit (mostly).
        """
        schedule = []
        current_day = {
            "day_number": 1, 
            "topics": [], 
            "minutes_used": 0,
            "sub_tasks": []
        }
        
        for topic in topics:
            t_min = topic.get("minutes", 30)
            
            # Check if this topic fits in the current day
            # (Allow overflow if the day is empty, so we don't get stuck on huge topics)
            if (current_day["minutes_used"] + t_min <= daily_limit) or (current_day["minutes_used"] == 0):
                # Add to current day
                current_day["topics"].append(topic["title"])
                current_day["sub_tasks"].extend(topic["sub_tasks"])
                current_day["minutes_used"] += t_min
            else:
                # Day is full. Save it and start a new one.
                schedule.append(current_day)
                current_day = {
                    "day_number": len(schedule) + 1,
                    "topics": [topic["title"]],
                    "minutes_used": t_min,
                    "sub_tasks": topic["sub_tasks"]
                }
        
        # Don't forget the last partial day
        if current_day["topics"]:
            schedule.append(current_day)
            
        return schedule

# Singleton Instance
llm_service = LLMService()