import os
import sys
# Add backend to path
sys.path.append(os.getcwd())

import google.generativeai as genai
from app.core.config import settings

def find_models():
    print(f"🔑 Checking API Key: {settings.GEMINI_API_KEY[:5]}...")
    
    try:
        genai.configure(api_key=settings.GEMINI_API_KEY)
        
        print("\n📋 Fetching available models...")
        models = list(genai.list_models())
        
        print("\n✅ MODELS YOU CAN USE:")
        found_any = False
        for m in models:
            # We look for models that can generate text (Chat)
            if 'generateContent' in m.supported_generation_methods:
                print(f"   * {m.name}")
                found_any = True
        
        if not found_any:
            print("❌ No text generation models found. Check your API Key permissions?")
            
    except Exception as e:
        print(f"❌ CRITICAL ERROR: {e}")

if __name__ == "__main__":
    find_models()