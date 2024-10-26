from graph.helper_graph_state import HelperGraphState
from graph.qdrant_hybrid_retriever import QdrantHybridRetriever
from qdrant_client import QdrantClient
from langchain_mistralai import MistralAIEmbeddings


class ManualRetrieverAgent:
    def __init__(self, client: QdrantClient, embedding_model: MistralAIEmbeddings, collection_name: str = "documents", k: int = 5):
        # Crear el retriever personalizado
        self.custom_retriever = QdrantHybridRetriever(
            client=client,
            collection_name=collection_name,
            text_field="page_content",
            embeddings=embedding_model
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
        question = state["question"]

        # Retrieval
        documents = self.custom_retriever.invoke(question)
        return {"documents": documents, "question": question}
