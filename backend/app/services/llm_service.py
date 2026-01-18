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

llm_service = LLMService()