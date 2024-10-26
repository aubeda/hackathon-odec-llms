### Generador de respuestas

from langchain_core.output_parsers import StrOutputParser
from langchain.prompts import ChatPromptTemplate
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage

from helper_graph_state import HelperGraphState


class ProposalAgent:
    def __init__(self, llm: BaseChatModel):
        self.llm = llm

        # Prompt
        system = """Eres un experto en análisis estadístico y entiendes a la perfección las tablas en formato markdown.
        Debes utilizar las tablas de contexto para responder la pregunta al final.
        Si no sabes la respuesta, simplemente di que no lo sabes, no intentes inventar una respuesta.
        Debes escoger la información de la tabla que contenga la variable más relevante para responder la pregunta.
        Ante dos variables igualmente relevantes, escoge la tabla que contenga la variable que aparece primero en la pregunta."""

        human = """Utiliza las siguientes tablas en formato HTML como contexto para responder la pregunta al final. Si no sabes la respuesta, simplemente di que no lo sabes, no intentes inventar una respuesta.
        Las tablas de contexto contienen en filas, las variables del estudio y después de cada variable aparecen sus categorias.
        Las variables que tienen categorias no contienen datos en la fila de la variable, solamente sus categorias.
        Una categoría sin variable en la primera de las filas de la tabla indíca el marginal de la tabla, la base, el total.

        Debes escoger la información de la tabla que contenga la variable más relevante para responder la pregunta.
        Ante dos variables igualmente relevantes, escoge la tabla que contenga la variable que aparece primero en la pregunta.

        Contexto: {context}

        Pregunta: {question}

        Respuesta:"""

        self.rag_prompt = ChatPromptTemplate.from_messages(
            [
                ("system", system),
                ("human", human),
            ]
        )

        self.rag_chain = self.rag_prompt | self.llm | StrOutputParser()

    def run(self, state: HelperGraphState) -> str:
        """
        Generate answer

        Args:
            state (dict): The current graph state

        Returns:
            state (dict): New key added to state, generation, that contains LLM generation
        """
        print("---GENERATE---")
        question = state["question"]
        documents = state["documents"]

        # RAG generation
        generation = self.rag_chain.invoke({"context": documents[0].metadata["table_content"], "question": question})
        result = {"documents": documents, "question": question, "generation": generation, "messages": state["messages"].append(AIMessage(content=generation))}
        # return {"documents": documents, "question": question, "generation": generation, "messages": state["messages"].append(AIMessage(content=generation))}
        print(result)
        return result


# Generador de respuestas
