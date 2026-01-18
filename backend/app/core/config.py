from typing import Optional
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "SkillMorph"
    VERSION: str = "0.1.0"
    
    # We make this Optional for today. 
    # In Phase 2, we will remove "Optional" to force you to have it.
    GEMINI_API_KEY: str
    
    # DB keys are Optional because we haven't built them yet
    NEO4J_URI: str 
    NEO4J_USER: str
    NEO4J_PASSWORD: str
    
    QDRANT_URL: str
    QDRANT_API_KEY: str

    model_config = SettingsConfigDict(env_file=".env", case_sensitive=True)

settings = Settings()