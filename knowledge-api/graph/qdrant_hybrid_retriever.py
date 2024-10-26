from typing import List, Optional, Dict, Any
from langchain.schema import BaseRetriever, Document
from langchain.embeddings.base import Embeddings
from qdrant_client import QdrantClient
from qdrant_client.http.models import Filter, FieldCondition, MatchValue
from pydantic import BaseModel, Field
import numpy as np


class QdrantHybridRetriever(BaseRetriever, BaseModel):
    """Retriever que combina búsqueda semántica y por keywords en Qdrant."""

    client: QdrantClient = Field(description="Cliente de Qdrant")
    collection_name: str = Field(description="Nombre de la colección")
    embeddings: Embeddings = Field(description="Modelo de embeddings")
    text_field: str = Field(default="text", description="Campo que contiene el texto")
    metadata_field: str = Field(
        default="metadata", description="Campo que contiene los metadatos"
    )
    hybrid_weight: float = Field(
        default=0.5,
        ge=0.0,
        le=1.0,
        description="Peso para combinar búsqueda semántica y keywords (0-1)",
    )
    k: int = Field(default=5, gt=0, description="Número de documentos a recuperar")

    class Config:
        arbitrary_types_allowed = True

    def _get_relevant_documents(
        self,
        query: str,
        *,
        run_manager: Optional[Any] = None,
        **kwargs: Any,
    ) -> List[Document]:
        # Obtener embedding de la query
        query_embedding = self.embeddings.embed_query(query)

        # Construir filtro si existe en kwargs
        search_filter = None
        if "filters" in kwargs and kwargs["filters"]:
            filter_conditions = []
            for field, value in kwargs["filters"].items():
                if isinstance(value, (list, tuple)):
                    condition = FieldCondition(
                        key=f"{self.metadata_field}.{field}",
                        match=MatchValue(any=value),
                    )
                else:
                    condition = FieldCondition(
                        key=f"{self.metadata_field}.{field}",
                        match=MatchValue(value=value),
                    )
                filter_conditions.append(condition)
            search_filter = Filter(must=filter_conditions)

        # Realizar búsqueda híbrida
        results = self.client.search(
            collection_name=self.collection_name,
            query_vector=query_embedding,
            query_filter=search_filter,
            with_payload=True,
            limit=self.k,
            search_params={
                "exact": False,
                "hnsw_ef": 128
            },
        )

        # Convertir resultados a documentos
        documents = []
        for res in results:
            doc = Document(
                page_content=res.payload.get(self.text_field, ""),
                metadata=res.payload.get(self.metadata_field, {}) | { "score": res.score },
            )
            documents.append(doc)

        return documents

    async def _aget_relevant_documents(
        self,
        query: str,
        *,
        run_manager: Optional[Any] = None,
        **kwargs: Any,
    ) -> List[Document]:
        # Implementación asíncrona
        search_filter = None
        if "filters" in kwargs and kwargs["filters"]:
            search_filter = self._build_filter(kwargs["filters"])

        documents = await self.client.asearch(
            collection_name=self.collection_name,
            query_vector=self.embeddings.embed_query(query),
            query_filter=search_filter,
            with_payload=True,
            limit=self.k,
            search_params={
                "exact": False,
                "hnsw_ef": 128,
                "words_weight": 1 - self.hybrid_weight,
                "vector_weight": self.hybrid_weight,
            },
        )

        return [
            Document(
                page_content=doc.payload.get(self.text_field, ""),
                metadata=doc.payload.get(self.metadata_field, {}),
            )
            for doc in documents
        ]

    def _build_filter(self, filters: Dict[str, Any]) -> Filter:
        """Construye el filtro de Qdrant a partir de un diccionario."""
        conditions = []
        for field, value in filters.items():
            if isinstance(value, (list, tuple)):
                condition = FieldCondition(
                    key=f"{self.metadata_field}.{field}", match=MatchValue(any=value)
                )
            else:
                condition = FieldCondition(
                    key=f"{self.metadata_field}.{field}", match=MatchValue(value=value)
                )
            conditions.append(condition)
        return Filter(must=conditions)

