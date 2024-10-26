from graph.helper_graph_state import HelperGraphState
from graph.qdrant_hybrid_retriever import QdrantHybridRetriever
from qdrant_client import QdrantClient
from langchain_mistralai import MistralAIEmbeddings
from langchain.schema import Document


class RetrieverAgent:
    def __init__(
        self,
        client: QdrantClient,
        embedding_model: MistralAIEmbeddings,
        collection_name: str = "documents",
        k: int = 5,
    ):
        # Crear el retriever personalizado
        self.custom_retriever = QdrantHybridRetriever(
            client=client,
            collection_name=collection_name,
            text_field="page_content",
            embeddings=embedding_model,
        )

    def _filtrar_por_score(
        self, documentos: list[Document], umbral: float
    ) -> list[Document]:
        """
        Filtra una lista de documentos retornando solo aquellos que tengan un score superior al umbral especificado.

        Args:
            documentos: Lista de documentos a filtrar
            umbral: Valor mínimo de score que deben tener los documentos

        Returns:
            Lista filtrada de documentos con score superior al umbral
        """
        return [doc for doc in documentos if doc.metadata.get("score", 0) >= umbral]

    def _ordenar_por_score(
        self,
        documentos: list[Document],
        descendente: bool = True
    ) -> list[Document]:
        """
        Ordena una lista de documentos según su score.

        Args:
            documentos: Lista de documentos a ordenar
            descendente: Si es True ordena de mayor a menor score, si es False de menor a mayor

        Returns:
            Lista de documentos ordenada por score
        """
        return sorted(
            documentos,
            key=lambda doc: doc.metadata.get("score", 0),
            reverse=descendente,
        )

    def run(self, state: HelperGraphState):
        """
        Retrieve documents

        Args:
            state (dict): The current graph state

        Returns:
            state (dict): New key added to state, documents, that contains retrieved documents
        """
        print("---RETRIEVE---")
        ticketSummary = state["ticketSummary"]

        # Retrieval
        documents = self.custom_retriever.invoke(ticketSummary)
        filtered_documents = self._filtrar_por_score(documents, 0.9)
        ordered_filtered_documents = self._ordenar_por_score(filtered_documents)
        return {"documents": ordered_filtered_documents, "ticketSummary": ticketSummary}
