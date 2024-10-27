### Generador de respuestas

from langchain_core.output_parsers import StrOutputParser, JsonOutputParser
from langchain.prompts import PromptTemplate, ChatPromptTemplate
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage

from graph.helper_graph_state import HelperGraphState
from langchain.prompts.chat import (
    ChatPromptTemplate,
    SystemMessagePromptTemplate,
    HumanMessagePromptTemplate,
)


class ProposalAgent:
    def __init__(self, llm: BaseChatModel):
        self.llm = llm

        # Prompt
        system_template = """Actúa como un asistente especialista en sugerir respuestas a tickets. Tu objetivo principal es, dadas las templates del json proporcionado en la petición del usuario, generar un json con el siguiente contenido, añadiendo en content la sugerencia de respuesta a ticketSummary que es una posible respuesta a la consulta de ticketSummary, inspirándote en las templates o alguna de las templates si crees que pueden ser útiles teniendo en cuenta que no son una solución directa: 

{{
    "role": assistantHuman,
    "nextState": "escalar",
    "suggestion":true,
    "content":"Sugerencia de respuesta al contenido de ticketSummary, inspirándote en las templates o alguna de las templates si crees que pueden ser útiles teniendo en cuenta que no son una solución directa"
}}

las claves son:
- "role": siempre valor assistantHuman
- "suggestion": siempre con valor true
- "nextState": siempre con valor "escalar"
- "content": Sugerencia de respuesta al contenido de ticketSummary, inspirándote en las templates o alguna de las templates si crees que pueden ser útiles teniendo en cuenta que no son una solución directa.

Ejemplos delimitados por ---:
input:
{{
    "ticketSummary": "Quiero hacer una reserva de un free tour en Madrid, pero el sistema solo me permite reservar en Valencia.",
    "templates": [
        {{template": "Para realizar una reserva debes de acceder al apartado "Reservar free tour" y seleccionar aquella que más te guste."}},
        {{"template": "Para reservar un free tour en una localidad concreta, se deben usar los filtros del apartado "Filtros"}}
    ]
}}

response:
{{
    "role": assistantHuman,
    "nextState": "escalar",
    "suggestion":true,
    "content":"Para realizar una resera de un free tour en Madrid tal vez puedes revisar los filtros del apartado "Filtros" o revisar en el apartado "Reservar free tour" si encuentras algún free tour en Madrid. Si con esto no resuelves tu duda, vuelve a contactar con nosotros."
}}
---"""

        human_template = """{{
            "ticketSummary": {ticketSummary},
            "metadata": [
                    {{template: {templates}}}
                ]
        }}"""

        system_message = SystemMessagePromptTemplate.from_template(system_template)
        human_message = HumanMessagePromptTemplate.from_template(human_template)

        self.rag_prompt = ChatPromptTemplate.from_messages(
            [system_message, human_message]
        )

        self.rag_chain = (
            self.rag_prompt | self.llm | JsonOutputParser()
        )

    def run(self, state: HelperGraphState) -> str:
        """
        Generate answer

        Args:
            state (dict): The current graph state

        Returns:
            state (dict): New key added to state, generation, that contains LLM generation
        """
        print("---GENERATE---")
        ticketSummary = state["ticketSummary"]
        documents = state["documents"]
        templates = [doc.metadata["template"] for doc in documents]

        # RAG generation
        result = self.rag_chain.invoke(
            {"ticketSummary": ticketSummary, "templates": templates}
        )

        print(result)
        return result


# Generador de respuestas
