import google.generativeai as genai
from config import settings
import time
import random

def get_gemini_response(prompt: str) -> str:
    """
    Tries to get a response using random keys. 
    If one fails due to Rate Limit, it immediately tries another. on 429 errors.
    """
    # Get list of keys
    keys = settings.api_key_list.copy()
    random.shuffle(keys) # Shuffle

    for api_key in keys:
        try:
            genai.configure(api_key=api_key)

            model = genai.GenerativeModel('gemini-2.5-flash')

            response = model.generate_content(prompt)
            return response.text
        
        except Exception as e:
            # Check for rate limit
            error_msg = str(e)
            if "429" in error_msg or "ResourceExhausted" in error_msg:
                print(f"Key {api_key[:5]}... hit limit. Switching keys...")
                continue # Try the next key in the loop
            else:
                raise e # If bad real error
            
    raise Exception("All API keys are currently rate-linmited. Please try agian later.")