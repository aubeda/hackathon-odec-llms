# downloader.py

import os
import requests
from bs4 import BeautifulSoup

# Esta función descarga el contenido de la URL dada
def download_documentation(url):
    try:
        response = requests.get(url)
        response.raise_for_status()  # Verifica si la solicitud fue exitosa
        return BeautifulSoup(response.text, 'html.parser')  # Retorna el contenido como objeto BeautifulSoup
    except requests.RequestException as e:
        print(f"Error al descargar la página: {e}")
        return None

# Esta función guarda el contenido en un archivo
def save_to_file(content, filename, directory):
    os.makedirs(directory, exist_ok=True)  # Crea el directorio si no existe
    filepath = os.path.join(directory, filename)
    with open(filepath, 'w', encoding='utf-8') as file:
        file.write(content)

# Esta función extrae enlaces de la página usando BeautifulSoup
def extract_links(soup, base_url):
    links = set()  # Usamos un set para evitar enlaces duplicados
    for a in soup.find_all('a', href=True):
        link = a['href']
        if link.startswith('/'):
            link = base_url + link  # Aseguramos que el enlace sea absoluto
        if link.startswith(base_url):
            links.add(link)  # Solo añadimos enlaces que pertenecen al mismo dominio
    return links

# Esta función descarga toda la documentación a partir de una URL inicial
def download_all_documentation(start_url, base_url, directory):
    visited = set()  # Conjunto para rastrear páginas visitadas
    to_visit = [start_url]  # Cola de URLs por visitar

    while to_visit:
        url = to_visit.pop(0)
        if url in visited:
            continue

        print(f"Descargando: {url}")
        soup = download_documentation(url)
        if soup:
            # Se forma el nombre del archivo a partir de la URL
            filename = url.replace(base_url, "").replace("/", "_") + ".html"
            save_to_file(soup.prettify(), filename, directory)
            visited.add(url)

            links = extract_links(soup, base_url)  # Extraemos los enlaces
            to_visit.extend(link for link in links if link not in visited and link not in to_visit)

if __name__ == "__main__":
    start_url = "https://help.codegpt.co/en/"  # Reemplaza con la URL de la documentación
    base_url = "https://help.codegpt.co"  # Reemplaza con la URL base de la documentación
    directory = "data"  # Directorio donde se guardarán los archivos
    download_all_documentation(start_url, base_url, directory)
