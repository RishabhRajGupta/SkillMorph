import os
import random
from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator

class Settings(BaseSettings):
    PROJECT_NAME: str = "SkillMorph"
    VERSION: str = "0.1.0"
    
    # We make this Optional for today. 
    # In Phase 2, we will remove "Optional" to force you to have it.
    GEMINI_API_KEYS: str
    
    # DB keys are Optional because we haven't built them yet
    NEO4J_URI: str 
    NEO4J_USER: str
    NEO4J_PASSWORD: str
    
    QDRANT_URL: str
    QDRANT_API_KEY: str

    model_config = SettingsConfigDict(env_file=".env", case_sensitive=True, extra="ignore")

    @property
    def api_key_list(self) -> List[str]:
        """Returns a list of Gemini API keys split by commas."""
        if not self.GEMINI_API_KEYS:
            return []
        return [key.strip() for key in self.GEMINI_API_KEYS.split(",") if key.strip()]

    # Randomly select a Gemini API key from the list
    def get_random_key(self) -> str:
        keys = self.api_key_list
        if not keys:
            raise ValueError("No Gemini API keys available in .env file!.")
        return random.choice(keys)
    
settings = Settings()