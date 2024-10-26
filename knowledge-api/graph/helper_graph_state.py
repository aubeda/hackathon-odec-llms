from typing import List, Literal

from typing_extensions import TypedDict


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
    documents: List[str]