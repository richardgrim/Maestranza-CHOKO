"""Demostración reproducible del incremento I1 con datos ficticios."""
import base64
import os
from pathlib import Path

from playwright.sync_api import expect, sync_playwright


def demostrar():
    salida = Path("evidencias")
    salida.mkdir(exist_ok=True)
    figuras = []

    with sync_playwright() as motor:
        navegador = motor.chromium.launch(channel="chrome", headless=True)
        pagina = navegador.new_page(viewport={"width": 1280, "height": 800})
        inicio = "http://127.0.0.1:8080"
        nombre = "Rectificadora de prueba I1"
        telefono = "+591 00000000"

        def capturar(archivo, titulo, descripcion):
            imagen = pagina.screenshot(path=salida / archivo)
            datos = base64.b64encode(imagen).decode("ascii")
            figuras.append(f'<section><h2>{titulo}</h2><p>{descripcion}</p>'
                           f'<img src="data:image/png;base64,{datos}"></section>')

        pagina.goto(inicio + "/clientes")
        expect(pagina).to_have_url(inicio + "/login")
        capturar("01-acceso.png", "1. Acceso al sistema",
                 "Al abrir Clientes sin una sesión activa, la aplicación solicita las credenciales.")
        pagina.locator("[name=username]").fill(os.environ["CHOKO_WEB_USER"])
        pagina.locator("[name=password]").fill(os.environ["CHOKO_WEB_PASSWORD"])
        pagina.locator("button[type=submit]").click()
        expect(pagina.get_by_role("heading", name="Clientes")).to_be_visible()
        pagina.get_by_role("link", name="Registrar cliente").click()
        pagina.get_by_label("Nombre", exact=True).fill(nombre)
        pagina.get_by_label("Teléfono (opcional)").fill(telefono)
        capturar("02-registro.png", "2. Registro de un cliente",
                 "El operador completa el nombre y el teléfono de un cliente ficticio.")
        pagina.get_by_role("button", name="Guardar cliente").click()
        expect(pagina.get_by_role("status")).to_have_text("Cliente registrado.")
        expect(pagina.get_by_role("cell", name=nombre)).to_be_visible()
        capturar("03-confirmacion.png", "3. Confirmación del registro",
                 "El sistema confirma el alta y muestra los datos guardados en el listado.")
        pagina.get_by_label("Buscar por nombre").fill("rectificadora")
        pagina.get_by_role("button", name="Buscar", exact=True).click()
        expect(pagina.get_by_role("cell", name=nombre)).to_be_visible()
        expect(pagina.get_by_role("cell", name=telefono, exact=True)).to_be_visible()
        capturar("04-busqueda.png", "4. Consulta por nombre",
                 "La búsqueda parcial en minúsculas recupera el cliente registrado.")
        pagina.get_by_role("button", name="Cerrar sesión").click()
        pagina.goto(inicio + "/clientes")
        expect(pagina).to_have_url(inicio + "/login")
        capturar("05-cierre.png", "5. Cierre de sesión",
                 "Después de salir, volver a abrir Clientes exige autenticarse otra vez.")
        informe = navegador.new_page()
        informe.set_content('<meta charset="utf-8"><style>@page{size:A4 landscape;margin:12mm}'
                            'body{font:15px Arial;color:#173033}section{break-after:page}'
                            'section:last-child{break-after:auto}h2{margin:0}img{display:block;'
                            'max-width:100%;max-height:145mm;border:1px solid #aaa}</style>'
                            + ''.join(figuras))
        informe.pdf(path=salida / "Maestranza-CHOKO-I1.pdf", prefer_css_page_size=True)
        navegador.close()


if __name__ == "__main__":
    demostrar()
