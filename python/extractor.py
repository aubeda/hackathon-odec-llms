import os
from langchain.document_loaders import UnstructuredHTMLLoader

def extract_text_from_html(file_path):
    """Extrae texto de un archivo HTML dado utilizando LangChain."""
    loader = UnstructuredHTMLLoader(file_path)
    document = loader.load()  # Carga el contenido del archivo HTML
    return document[0].page_content  # Retorna el contenido extraído

def save_extracted_text(text, output_file):
    """Guarda el texto extraído en un archivo."""
    with open(output_file, 'w', encoding='utf-8') as file:
        file.write(text)

def extract_texts_from_directory(directory):
    """Extrae texto de todos los archivos HTML en un directorio dado utilizando LangChain."""
    extracted_texts = {}

    for filename in os.listdir(directory):
        if filename.endswith('.html'):
            file_path = os.path.join(directory, filename)
            print(f"Extrayendo texto de: {file_path}")
            text = extract_text_from_html(file_path)
            extracted_texts[filename] = text  # Guarda el texto extraído con el nombre del archivo

            # Opcional: guardar cada texto en un archivo separado
            output_file = os.path.join(directory, f"{filename.replace('.html', '')}.txt")
            save_extracted_text(text, output_file)

    return extracted_texts

if __name__ == "__main__":
    directory = "data"  # Directorio donde se encuentran los archivos HTML
    extracted_texts = extract_texts_from_directory(directory)

    # Opcional: imprimir el texto extraído de cada archivo
    for filename, text in extracted_texts.items():
        print(f"\nTexto extraído de {filename}:\n{text[:500]}...")  # Muestra solo los primeros 500 caracteres
