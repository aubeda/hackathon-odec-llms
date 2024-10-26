from fastapi import FastAPI, HTTPException
from langchain_mistralai import MistralAIEmbeddings
from pydantic import BaseModel, Field
from typing import List, Literal, Optional
from langchain.schema import Document
from fastapi.responses import JSONResponse
from fastapi.encoders import jsonable_encoder
from qdrant_client import QdrantClient
from graph.helper_graph import HelperGraph

from os import getenv
from dotenv import load_dotenv

app = FastAPI(title="Knowledge Retrieval API")

# Modelos Pydantic para validación de datos
class InboundTicketMessageRequest(BaseModel):
    summary: str
    type: Literal["reserva", "guia", "desconocido"]
    response: str

class InboundTicketMessageResponse(BaseModel):
    role: Literal["assistantAI", "asistantHuman"]
    suggestion: bool
    nextState: Literal["ESCALAR", "RESOLVER"]
    content: str

class QueryRequest(BaseModel):
    query: str
    filters: Optional[dict] = None
    top_k: Optional[int] = 5


class SaveRequest(BaseModel):
    data: dict
    options: Optional[dict] = None


class DocumentResponse(BaseModel):
    page_content: str
    metadata: dict


class QueryResponse(BaseModel):
    documents: List[DocumentResponse]
    total_found: int


# Clase para manejar la conversión de Document a formato JSON
class DocumentEncoder:
    @staticmethod
    def encode_document(doc: Document) -> dict:
        return {"page_content": doc.page_content, "metadata": doc.metadata}


# Inicialización del grafo de comportamiento
def init_helper_agent():
    mistral_api_key = getenv("MISTRAL_API_KEY")
    COLLECTION_NAME = "hackathon-gurusup"
    QDRANT_URL = getenv("QDRANT_URL")
    QDRANT_API_KEY = getenv("QDRANT_API_KEY")

    return HelperGraph(
        mistral_api_key=mistral_api_key,
        collection_name=COLLECTION_NAME,
        qdrant_url=QDRANT_URL,
        qdrant_api_key=QDRANT_API_KEY
    )

load_dotenv()

# Crear instancia del retriever
retriever = init_helper_agent()


@app.post("/inbound_ticket_message", response_model=InboundTicketMessageResponse)
async def inbound_ticket_message(request: InboundTicketMessageRequest):
    pass

@app.post("/query", response_model=QueryResponse)
async def query_documents(request: QueryRequest):
    try:
        # Realizar la consulta usando el retriever
        documents = retriever.invoke(request.query)

        # Convertir los documentos al formato de respuesta
        response_documents = [DocumentEncoder.encode_document(doc) for doc in documents]

        return JSONResponse(
            content={"documents": response_documents, "count": len(documents)}
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error processing query: {str(e)}")


@app.post("/save")
async def process_data(request: SaveRequest):
    try:
        # Aquí iría la lógica de procesamiento de datos
        # Este es un placeholder para tu lógica específica
        process_result = process_data_logic(request.data, request.options)

        return JSONResponse(
            status_code=200,
            content={
                "message": "Data processed successfully",
                "result": process_result,
            },
        )

    except ValueError as ve:
        return JSONResponse(status_code=400, content={"error": str(ve)})
    except Exception as e:
        return JSONResponse(
            status_code=500, content={"error": f"Internal server error: {str(e)}"}
        )


def process_data_logic(data: dict, options: Optional[dict] = None):
    # Implementa aquí tu lógica de procesamiento
    # Este es solo un placeholder
    return {"processed": True}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
