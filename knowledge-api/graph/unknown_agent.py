### Generador de respuestas a tópicos desconocidos

from langchain_core.output_parsers import StrOutputParser, JsonOutputParser
from langchain.prompts import ChatPromptTemplate
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage

from graph.helper_graph_state import HelperGraphState
from langchain.prompts.chat import (
    ChatPromptTemplate,
    SystemMessagePromptTemplate,
    HumanMessagePromptTemplate
)


class UnknownAgent:
    def __init__(self, llm: BaseChatModel):
        self.llm = llm

        # Prompt
        system_template = """"Actúa como un asistente especialista en sugerir respuestas a tickets. Tu objetivo principal es, dado el contenido de la clave "ticketSummary" del json dado en la petición del usuario, generar un json con el siguiente contenido, añadiendo en content la sugerencia de respuesta a ticketSummary que le darías al agente humano: 

{{
    "role": assistantHuman,
    "nextState": "escalar",
    "suggestion":true|false,
    "content":"Sugerencia de respuesta al contenido de ticketSummary"
}}

las claves son:
- "role": siempre valor assistantHuman
- "suggestion": siempre con valor true
- "nextState": siempre con valor "escalar"
- "content": Sugerencia para humano de respuesta al contenido de ticketSummary

Ejemplos delimitados por ---:
---
input:
{{
    "ticketSummary":"Me gustaría saber el precio de los distintos free tours de cada país, ¿Es posible?", 
    "type": "desconocido",
    "response:""
}}

response:
{{
    "role": "assistantHuman",
    "nextState": "escalar",
    "suggestion":true,
    "content":"Muchas gracias por ponerte en contacto con nosotros. Vamos a tratar de resolver tu duda de la mejor forma posible. Para poder saber el precio de los distintos"
}}"""

        human_template = """"{{
            ticketSummary": "{ticketSummary}",
            "type": "{ticketType}",
            "response": ""
        }}"""

        system_message = SystemMessagePromptTemplate.from_template(system_template)
        human_message = HumanMessagePromptTemplate.from_template(human_template)

        self.rag_prompt = ChatPromptTemplate.from_messages(
            [
                system_message,
                human_message
            ]
        )

        self.rag_chain = self.rag_prompt | self.llm | JsonOutputParser()

    def run(self, state: HelperGraphState) -> str:
        """
        Generar respuesta para tema desconocido

        Args:
            state (dict): Estado actual del grafo

        Returns:
            state (dict): Nuevas claves añadidas al estado
        """
        print("---GENERATE---")
        ticketSummary = state["ticketSummary"]
        ticketType = state["ticketType"]

        # RAG generation
        result = self.rag_chain.invoke(
            {"ticketSummary": ticketSummary, "ticketType": ticketType}
        )

        print(result)
        return { "response": result }


# Generador de respuestas a tópicos desconocidos
