from typing import List, Literal

from typing_extensions import TypedDict
from langchain.schema import Document

class HelperGraphState(TypedDict):
    """
    Estado del grafo.

    Attributes:
        question: pregunta
        generation: respuesta LLM
        documents: lista de documentos
    """

    ticketSummary: str
    ticketType: Literal["reserva", "guia", "desconocido"]
    generation: str
    documents: List[Document]
    response: str