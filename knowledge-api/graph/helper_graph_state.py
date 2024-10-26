from typing import List

from typing_extensions import TypedDict


class HelperGraphState(TypedDict):
    """
    Estado del grafo.

    Attributes:
        question: pregunta
        generation: respuesta LLM
        documents: lista de documentos
    """

    question: str
    generation: str
    documents: List[str]