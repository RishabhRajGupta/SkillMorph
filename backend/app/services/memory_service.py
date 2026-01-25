from qdrant_client import QdrantClient
from qdrant_client.models import PointStruct, VectorParams, Distance
from app.core.config import settings
from app.services.llm_service import llm_service
import uuid

class MemoryService:
    def __init__(self):
        # SAFETY CHECK: Ensure Qdrant URL exists
        if not settings.QDRANT_URL:
            print("⚠️ MemoryService Disabled: QDRANT_URL not found in .env")
            self.client = None
            return

        self.client = QdrantClient(
            url=settings.QDRANT_URL,
            api_key=settings.QDRANT_API_KEY,
            timeout=60.0
        )
        self.collection_name = "user_memories"
        self._ensure_collection_exists()

    def _ensure_collection_exists(self):
        if not self.client: return
        
        try:
            if not self.client.collection_exists(self.collection_name):
                print("🧠 Memory: Creating new Qdrant collection...")
                self.client.create_collection(
                    collection_name=self.collection_name,
                    # 768 is correct for 'text-embedding-004'
                    vectors_config=VectorParams(size=768, distance=Distance.COSINE)
                )
        except Exception as e:
            print(f"❌ Qdrant Connection Error: {e}")

    def save_memory(self, text: str):
        if not self.client: return {"status": "error", "reason": "No DB Connection"}

        # 1. Summarize
        summary = llm_service.summarize_memory(text)
        if not summary:
            return {"status": "skipped", "reason": "Summarization failed"}

        # 2. Vectorize
        vector = llm_service.get_embedding(summary)
        if not vector:
            return {"status": "skipped", "reason": "Embedding failed"}

        # 3. Store in Qdrant
        try:
            self.client.upsert(
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
        except Exception as e:
            print(f"❌ Memory Save Failed: {e}")
            return {"status": "error", "reason": str(e)}

    def search_memory(self, query: str, limit: int = 3):
        if not self.client: return []

        vector = llm_service.get_embedding(query)
        if not vector: return []

        try:
            results = self.client.search(
                collection_name=self.collection_name,
                query_vector=vector,
                limit=limit
            )
            return [hit.payload["summary"] for hit in results]
        except Exception as e:
            print(f"❌ Memory Search Failed: {e}")
            return []

memory_service = MemoryService()