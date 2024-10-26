### Generador de respuestas

from langchain_core.output_parsers import StrOutputParser, JsonOutputParser
from langchain.prompts import ChatPromptTemplate
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_mistralai import ChatMistralAI

from graph.helper_graph_state import HelperGraphState
from langchain.schema import Document
from langchain.prompts.chat import (
    ChatPromptTemplate,
    SystemMessagePromptTemplate,
    HumanMessagePromptTemplate
)

class AnswerGeneratorAgent:
    def __init__(self, llm: BaseChatModel):
        # self.llm = ChatMistralAI(
        #     temperature=0,
        #     model_name="open-mistral-nemo",
        # )
        self.llm = llm

        # Prompt
        system_template = """Actúa como un agente especialista en generar respuestas a tickets. Tu objetivo principal es, dada la template del json dado en la petición del usuario, generar un json con el siguiente contenido, añadiendo en content la respuesta a ticketSummary siguiendo la template que es una posible respuesta a la consulta de ticketSummary con comentario introductorio: 

{{
    "role": "assistantIA",
    "nextState": "resolver",
    "suggestion":false,
    "content":"Sugerencia de respuesta al contenido de ticketSummary basándose en la solución de template con comentario introductorio."
}}

las claves son:
- "role": siempre valor assistantIA
- "suggestion": siempre con valor false
- "nextState": siempre con valor "resolver"
- "content": Sugerencia de respuesta al contenido de ticketSummary basándose en la solución de template con comentario introductorio.

Ejemplos delimitados por ---:
---
input1:
{{
    "ticketSummary": "Quiero realizar un recorrido de fotografía con un guía, pero no veo perfiles que ofrezcan este enfoque.",
    "metadata": {{
        "template": "Seleccione 'Fotografía' en los filtros. Si aún no aparece ninguna opción, contáctenos para coordinar con un guía especializado en fotografía."
    }}
}}
response1:
{{
    "role": assistantIA,
    "nextState": "resolver",
    "suggestion":false,
    content":"Para poder realizar un recorrdio de fotografía con guía debes de seleccionar el filtro 'Fotografía'. Si después de realizar este paso no logras encontrar un guía especializado en fotografía, no dudes en contactarnos."
}}

input2:
{{
    "ticketSummary": "No consigo encontrar la forma de buscar un guía que ofrezca un tour de fotografía",
    "metadata": {{
        "template": "Seleccione 'Fotografía' en los filtros. Si aún no aparece ninguna opción, contáctenos para coordinar con un guía especializado en fotografía."
    }}
}}
response2:
{{
    "role": assistantIA,
    "nextState": "resolver",
    "suggestion":false,
    "content":"Aquí tienes una sugerencia sobre una respuesta que puedes utilizar: Por favor, prueba a seleccionar 'Fotografía' en los filtros. Si después de realizar esto no has logrado encontrar la forma de buscar un guía que ofrezca un tour de fotografía no dudes en contactarnos."
}}
---"""

        # Corregido: aseguramos que los nombres de variables coincidan
        human_template = """{{
            "ticketSummary": {ticketSummary},
            "metadata": {{
                "template": {template}
            }}
        }}"""


        system_message = SystemMessagePromptTemplate.from_template(system_template)
        human_message = HumanMessagePromptTemplate.from_template(human_template)

        self.rag_prompt = ChatPromptTemplate.from_messages(
            [
                system_message,
                human_message
            ]
        )

        self.rag_chain = self.rag_prompt | self.llm | StrOutputParser() | JsonOutputParser()

    def run(self, state: HelperGraphState) -> str:
        """
        Generate answer

        Args:
            state (dict): The current graph state

        Returns:
            state (dict): New key added to state, generation, that contains LLM generation
        """
        print("---GENERATE---")
        answer_document = state["documents"][0]

        # RAG generation
        """ formatted_prompt = self.rag_prompt.format_messages(
            template= answer_document.metadata["template"],
            ticketSummary= answer_document.page_content,
        )"""
        # response = self.rag_chain.invoke(formatted_prompt);

        response = self.rag_chain.invoke({
            "ticketSummary": answer_document.page_content,
            "template": answer_document.metadata["template"]
        })
        print(response)
        return { "response": response }


# Generador de respuestas
