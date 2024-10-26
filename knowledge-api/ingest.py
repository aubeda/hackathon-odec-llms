import os

from qdrant_data_harvester import QdrantDataHarvester

def main():
    """
    Recolector de datos con Qdrant.
    """
    # Configuración
    FILE_PATH = "threads.json"
    mistral_api_key = os.getenv("MISTRAL_API_KEY")
    COLLECTION_NAME = "hackathon-gurusup"

    # Para uso remoto de Qdrant:
    QDRANT_URL = os.getenv("QDRANT_URL")
    QDRANT_API_KEY = os.getenv("QDRANT_API_KEY")

    try:
        # Crear el recolector
        harvester = QdrantDataHarvester(
            file_path=FILE_PATH,
            mistral_api_key=mistral_api_key,
            collection_name=COLLECTION_NAME,
            qdrant_url=QDRANT_URL,
            qdrant_api_key=QDRANT_API_KEY
        )

        # Recolectar datos e introducirlos en la base vectorial
        harvester.load_documents(FILE_PATH)

    except Exception as e:
        print(f"Error: {str(e)}")


if __name__ == "__main__":
    main()
