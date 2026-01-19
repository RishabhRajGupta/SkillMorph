import sys
import os

# Add current folder to path so we can import app settings
sys.path.append(os.getcwd())

try:
    from google import genai
    from app.core.config import settings
    print("✅ Library imported successfully.")
except ImportError as e:
    print(f"❌ CRITICAL: Could not import google.genai. Did you run 'pip install google-genai'?")
    print(e)
    sys.exit(1)

def test_gemini():
    print("------------------------------------------------")
    print("🧠 DEBUGGING GEMINI (New SDK)")
    print(f"🔑 API Key: {settings.GEMINI_API_KEY[:5]}... (Checked)")

    try:
        # 1. Setup Client
        client = genai.Client(api_key=settings.GEMINI_API_KEY)
        
        # 2. Test Generation (Summarization)
        print("\n1. Testing Text Generation...")
        response = client.models.generate_content(
            model="gemini-2.5-flash", # <--- UPDATE THIS LINE
            contents="Say 'Hello System' if you can hear me."
        )

        # 3. Test Embeddings (Vector Memory)
        print("\n2. Testing Embeddings...")
        embed_response = client.models.embed_content(
            model="text-embedding-004",
            contents="Test vector"
        )
        # In the new SDK, sometimes the structure is slightly different.
        # We test if we can access the values.
        vector = embed_response.embeddings[0].values
        print(f"   📐 Vector generated! Length: {len(vector)}")
        
        print("\n✅ SUCCESS: llm_service logic is correct.")

    except Exception as e:
        print("\n❌ FAILURE: The new SDK crashed.")
        print(f"Error Type: {type(e).__name__}")
        print(f"Error Message: {e}")

if __name__ == "__main__":
    test_gemini()