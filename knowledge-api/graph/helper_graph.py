from os import getenv
from typing import Literal

from langchain_mistralai import ChatMistralAI, MistralAIEmbeddings
from graph.answer_generator_agent import AnswerGeneratorAgent
from graph.retriever_agent import RetrieverAgent
from graph.router_agent import RouterAgent
from graph.proposal_agent import ProposalAgent
from graph.reserve_agent import ReserveAgent
from graph.unknown_agent import UnknownAgent
from graph.manual_retriever_agent import ManualRetrieverAgent

from qdrant_client import QdrantClient
from qdrant_client.http.models import Distance, VectorParams
from langgraph.checkpoint.sqlite import SqliteSaver
import sqlite3

from graph.helper_graph_state import HelperGraphState
from langgraph.graph import StateGraph, START, END
from langchain.schema import Document


class HelperGraph:
    def __init__(
        self,
        mistral_api_key: str = getenv("MISTRAL_API_KEY"),
        collection_name: str = "hackathon-gurusup",
        qdrant_url=getenv("QDRANT_URL"),
        qdrant_api_key=getenv("QDRANT_API_KEY"),
        llm_model: str = "open-mistral-nemo",
    ):

        # Configurar embeddings
        self.embedding_model = MistralAIEmbeddings(
            model="mistral-embed", mistral_api_key=mistral_api_key
        )

        # Configurar cliente Qdrant
        self.qdrant_client = QdrantClient(url=qdrant_url, api_key=qdrant_api_key)
        self.collection_name = collection_name

        # Initialize LLM
        self.llm = ChatMistralAI(
            temperature=0,
            model_name=llm_model,
        )

        # Crea la colección de Qdrant si no existe
        self._initialize_collection()

        # Configura la persistencia del gestor de la memoria de estados
        def from_conn_stringx(
            cls,
            conn_string: str,
        ) -> "SqliteSaver":
            return SqliteSaver(
                conn=sqlite3.connect(conn_string, check_same_thread=False)
            )

        SqliteSaver.from_conn_stringx = classmethod(from_conn_stringx)

        # Gestor de memoria de estados
        self.memory = SqliteSaver.from_conn_stringx(":memory:")

        # Nodo de triaje de peticiones
        router_node = RouterAgent(llm=self.llm)

        # Agente de recuperación de documentos
        retriever_node = RetrieverAgent(
            client=self.qdrant_client,
            embedding_model=self.embedding_model,
            collection_name=self.collection_name,
        )

        # Agente de recuperación de documentos del manual
        # manual_retriever_node = ManualRetrieverAgent(
        #     client=self.qdrant_client,
        #     embedding_model=self.embedding_model,
        #     collection_name=self.collection_name,
        # )

        # Agente generador de respuestas
        answer_generator_node = AnswerGeneratorAgent(llm=self.llm)

        # Agente topic Propuestas
        proposal_node = ProposalAgent(llm=self.llm)

        # Agente topic Reservas
        # reserve_node = ReserveAgent(llm=self.llm)

        # Agente topic Desconocidos
        unknown_node = UnknownAgent(llm=self.llm)

        # Instancia del grafo de comportamiento
        graph = StateGraph(HelperGraphState)

        # Definición de nodos
        graph.add_node("proposal", proposal_node.run)  # Agente topic Propuestas
        graph.add_node("unknown", unknown_node.run)  # Agente topic Desconocidos
        graph.add_node("retrieve", retriever_node.run)  # Recuperador
        # graph.add_node(
        #     "manual_retrieve", manual_retriever_node.run
        # )  # Recuperador documentos manual
        graph.add_node("answer", answer_generator_node.run)  # Generador de respuestas

        # Construcción del grafo
        graph.add_conditional_edges(
            START,
            router_node.run,
            {
                "reserva": "retrieve",
                "guia": "retrieve",
                "desconocido": "unknown",
            },
        )

        graph.add_conditional_edges(
            "retrieve",
            self.__get_retrieve_next,
            {
                "not_sure": "proposal",
                "confident": "answer",
            },
        )

        graph.add_edge("proposal", END)
        graph.add_edge("unknown", END)
        graph.add_edge("answer", END)

        self.config = {"configurable": {"thread_id": "2"}}
        self.graph = graph.compile(checkpointer=self.memory)

    def __get_retrieve_next(
        self, state: HelperGraphState
    ) -> Literal["not_sure", "confident"]:
        documents = state["documents"]
        if len(documents) > 0:
            return "confident"
        else:
            return "not_sure"

    def _initialize_collection(self):
        """Inicializa la coleccion de Qdrant en caso de que no exista."""
        collections = self.qdrant_client.get_collections()
        collection_names = [collection.name for collection in collections.collections]

        if self.collection_name not in collection_names:
            print(f"Nueva colección creada: {self.collection_name}")
            self.qdrant_client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(size=1536, distance=Distance.COSINE),
            )
        else:
            print(f"Colección {self.collection_name} existente")

    def start(self, question: str):
        result = self.graph.invoke({"question": question}, self.config)
        if result is None:
            values = self.graph.get_state(self.config).values
            last_state = next(iter(values))
            return values[last_state]
        return result

    def resume(self, new_values: dict):
        values = self.graph.get_state(self.config).values
        last_state = next(iter(values))
        values[last_state].update(new_values)
        self.graph.update_state(self.config, values[last_state])
        result = self.graph.invoke(None, self.config, output_keys=last_state)
        if result is None:
            values = self.graph.get_state(self.config).values
            last_state = next(iter(values))
            return self.graph.get_state(self.config).values[last_state]
        return result

    def invoke(self, initial_state, config={}):
        return self.graph.invoke(initial_state, config=config | self.config)
