from qdrant_client import QdrantClient
from qdrant_client.models import PointStruct, VectorParams, Distance
from app.core.config import settings
from app.services.llm_service import llm_service
import uuid

class MemoryService:
    def __init__(self):
        # Connect to Qdrant Cloud
        self.client = QdrantClient(
            url=settings.QDRANT_URL,
            api_key=settings.QDRANT_API_KEY
        )
        self.collection_name = "user_memories"
        self._ensure_collection_exists()

    def _ensure_collection_exists(self):
        """Creates the vector collection if it doesn't exist."""
        if not self.client.collection_exists(self.collection_name):
            print("🧠 Memory: Creating new Qdrant collection...")
            self.client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(size=768, distance=Distance.COSINE)
            )

    def save_memory(self, text: str):
        """The Full Pipeline: Summarize -> Vectorize -> Store"""
        # 1. Summarize
        summary = llm_service.summarize_memory(text)
        
        # 2. Vectorize
        vector = llm_service.get_embedding(summary)
        
        # 3. Store in Qdrant
        operation_info = self.client.upsert(
            collection_name=self.collection_name,
            points=[
                PointStruct(
                    id=str(uuid.uuid4()),
                    vector=vector,
                    payload={"original_text": text, "summary": summary}
                )
            ]
        )
        return {"status": "saved", "summary": summary}

    def search_memory(self, query: str, limit: int = 3):
        """Recall memories relevant to the query."""
        vector = llm_service.get_embedding(query)
        results = self.client.search(
            collection_name=self.collection_name,
            query_vector=vector,
            limit=limit
        )
        return [hit.payload["summary"] for hit in results]

memory_service = MemoryService()