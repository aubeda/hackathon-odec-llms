### Router
from typing import Literal
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field
from helper_graph_state import HelperGraphState
from langchain_core.language_models.chat_models import BaseChatModel

# Data model
class RouteAgentQuery(BaseModel):
    """Enruta la petición del usuario al especialista más apropiado."""

    datasource: Literal["topic1", "topic2", "desconocido"] = Field(
        ...,
        description="Encamina la petición del usuario al gestor más apropiado.",
    )


class RouterAgent:
    def __init__(self, llm: BaseChatModel):
        self.llm = llm

        # Prompt
        system = """You are an expert at routing a user question to a vectorstore or web search.
        The vectorstore contains documents related to agents, prompt engineering, and adversarial attacks.
        Use the vectorstore for questions on these topics. Otherwise, use web-search."""

        self.route_prompt = ChatPromptTemplate.from_messages(
            [
                ("system", system),
                ("human", "{question}"),
            ]
        )

        self.question_router = self.route_prompt | self.llm.with_structured_output(RouteAgentQuery)


    def run(self, state: HelperGraphState) -> Literal["topic1", "topic2", "desconocido"]:
        return "topic1"


# Router