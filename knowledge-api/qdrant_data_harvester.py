import json
from pathlib import Path
from langchain_mistralai import MistralAIEmbeddings
from langchain_core.documents import Document
from langchain_qdrant import Qdrant
from typing import List, Dict, Optional
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams
import uuid
import os

class QdrantDataHarvester:
    def __init__(
        self,
        file_path: str,
        mistral_api_key: str,
        collection_name: str = "hackathon-gurusup",
        qdrant_url: Optional[str] = None,
        qdrant_api_key: Optional[str] = None,
        qdrant_path: Optional[str] = None,
    ):
        """
        Inicializa el recolector de datos con Qdrant como vector store.

        Args:
            file_path: Ruta al archivo de texto
            mistral_api_key: API key de Groq
            collection_name: Nombre de la colección en Qdrant
            qdrant_url: URL del servidor Qdrant (para uso remoto)
            qdrant_api_key: API key de Qdrant (para uso remoto)
            qdrant_path: Ruta local para Qdrant (para uso local)
        """
        self.collection_name = collection_name

        # Configurar embeddings
        self.embeddings = MistralAIEmbeddings(
            model="mistral-embed",
            mistral_api_key=mistral_api_key
        )

        # Configurar cliente Qdrant
        if qdrant_url and qdrant_api_key:
            # Modo remoto
            self.qdrant_client = QdrantClient(url=qdrant_url, api_key=qdrant_api_key)
        else:
            # Modo local
            self.qdrant_path = qdrant_path or "./qdrant_db"
            os.makedirs(self.qdrant_path, exist_ok=True)
            self.qdrant_client = QdrantClient(path=self.qdrant_path)

        # Inicializar Qdrant y cargar documentos
        self._initialize_qdrant()
        self.vector_store = Qdrant(
            client=self.qdrant_client,
            collection_name=self.collection_name,
            embeddings=self.embeddings,
        )

    def _initialize_qdrant(self):
        """
        Inicializa la colección en Qdrant si no existe.
        """
        try:
            # Verificar si la colección existe
            collections = self.qdrant_client.get_collections()
            collection_names = [
                collection.name for collection in collections.collections
            ]

            if self.collection_name not in collection_names:
                # Crear nueva colección
                self.qdrant_client.create_collection(
                    collection_name=self.collection_name,
                    vectors_config=VectorParams(
                        size=1024,
                        distance=Distance.COSINE,
                    ),
                )
            else:
                print(f"Usando colección existente: '{self.collection_name}'")

        except Exception as e:
            raise Exception(f"Error al inicializar Qdrant: {str(e)}")

    def load_documents(self, file_path: str):
        """Recolectar datos desde fichero json."""
        txt_path = Path(file_path)

        # Verificar que el fichero existe
        if not txt_path.exists():
            raise ValueError(
                f"El fichero {file_path} no existe"
            )

        def load_content(file_path):
            with open(file_path, "r", encoding="utf-8") as file:
                content = json.load(file)
            return content

        threads = load_content(file_path)

        documents = []
        for idx, thread in enumerate(threads):
            try:
                # Crear documento con metadatos
                doc = Document(
                    page_content = thread["ticketSummary"], # Contenido
                    metadata = thread["metadata"], # Metadatos
                )
                documents.append(doc)

            except Exception as e:
                print(f"Error al procesar el archivo {idx}: {str(e)}")

        """Añadir documentos a la base vectorial."""
        uuids = [str(uuid.uuid4()) for _ in range(len(documents))]
        self.vector_store.add_documents(documents=documents, ids=uuids)

        if documents:
            print(f"Se han indexado {len(documents)} documentos correctamente")
            return documents
        else:
            print("No se encontraron documentos para indexar")
            return None

