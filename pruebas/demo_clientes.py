"""Demostración reproducible del flujo de clientes y órdenes con datos ficticios."""
import base64
import os
from pathlib import Path

from playwright.sync_api import expect, sync_playwright


from functools import partial


def capturar(pagina, salida, figuras, archivo, titulo, descripcion):
    imagen = pagina.screenshot(path=salida / archivo, full_page=True)
    datos = base64.b64encode(imagen).decode("ascii")
    figuras.append(f'<section><h2>{titulo}</h2><p>{descripcion}</p>'
                   f'<img src="data:image/png;base64,{datos}"></section>')

def generar_pdf(navegador, figuras, destino):
    informe = navegador.new_page()
    informe.set_content('<meta charset="utf-8"><style>@page{size:A4 landscape;margin:12mm}'
                        'body{font:15px Arial;color:#173033}section{break-after:page}'
                        'section:last-child{break-after:auto}h2{margin:0}img{display:block;'
                        'max-width:100%;max-height:145mm;border:1px solid #aaa}</style>'
                        + ''.join(figuras))
    informe.pdf(path=destino, prefer_css_page_size=True)
    informe.close()


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

        capturar_i1 = partial(capturar, pagina, salida, figuras)

        pagina.goto(inicio + "/clientes")
        expect(pagina).to_have_url(inicio + "/login")
        capturar_i1("01-acceso.png", "1. Acceso al sistema",
                 "Al abrir Clientes sin una sesión activa, la aplicación solicita las credenciales.")
        pagina.locator("[name=username]").fill(os.environ["CHOKO_WEB_USER"])
        pagina.locator("[name=password]").fill(os.environ["CHOKO_WEB_PASSWORD"])
        pagina.locator("button[type=submit]").click()
        expect(pagina.get_by_role("heading", name="Clientes")).to_be_visible()
        pagina.get_by_role("link", name="Registrar cliente").click()
        pagina.get_by_label("Nombre", exact=True).fill(nombre)
        pagina.get_by_label("Teléfono (opcional)").fill(telefono)
        capturar_i1("02-registro.png", "2. Registro de un cliente",
                 "El operador completa el nombre y el teléfono de un cliente ficticio.")
        pagina.get_by_role("button", name="Guardar cliente").click()
        expect(pagina.get_by_role("status")).to_have_text("Cliente registrado.")
        expect(pagina.get_by_role("cell", name=nombre)).to_be_visible()
        capturar_i1("03-confirmacion.png", "3. Confirmación del registro",
                 "El sistema confirma el alta y muestra los datos guardados en el listado.")
        pagina.get_by_label("Buscar por nombre").fill("rectificadora")
        pagina.get_by_role("button", name="Buscar", exact=True).click()
        expect(pagina.get_by_role("cell", name=nombre)).to_be_visible()
        expect(pagina.get_by_role("cell", name=telefono, exact=True)).to_be_visible()
        capturar_i1("04-busqueda.png", "4. Consulta por nombre",
                 "La búsqueda parcial en minúsculas recupera el cliente registrado.")
        pagina.get_by_role("button", name="Cerrar sesión").click()
        pagina.goto(inicio + "/clientes")
        expect(pagina).to_have_url(inicio + "/login")
        capturar_i1("05-cierre.png", "5. Cierre de sesión",
                 "Después de salir, volver a abrir Clientes exige autenticarse otra vez.")
        generar_pdf(navegador, figuras, salida / "Maestranza-CHOKO-I1.pdf")
        pagina.locator("[name=username]").fill(os.environ["CHOKO_WEB_USER"])
        pagina.locator("[name=password]").fill(os.environ["CHOKO_WEB_PASSWORD"])
        pagina.locator("button[type=submit]").click()
        demostrar_ordenes(pagina, navegador, nombre)
        navegador.close()


def demostrar_ordenes(pagina, navegador, nombre):
    salida = Path("evidencias-ordenes")
    salida.mkdir(exist_ok=True)
    figuras = []
    captura = partial(capturar, pagina, salida, figuras)
    fila = pagina.get_by_role("row").filter(has_text=nombre)
    fila.get_by_role("link", name="Ver órdenes").click()
    expect(pagina.get_by_role("heading", name=nombre, exact=True)).to_be_visible()
    expect(pagina.locator("article")).to_have_count(0)
    captura("01-cliente.png", "1. Órdenes del cliente",
            "El operador abre el listado del cliente ficticio antes de registrar su primera orden.")
    pagina.get_by_role("link", name="Registrar orden").click()
    pagina.get_by_label("Componente recibido").fill("Culata")
    pagina.get_by_label("Servicios solicitados").fill("Rectificar y verificar planitud")
    pagina.get_by_label("Fecha de recepción").fill("2026-11-25")
    pagina.get_by_label("Entrega estimada").fill("2026-11-24")
    guardar = pagina.get_by_role("button", name="Guardar orden")
    guardar.click()
    expect(pagina.get_by_role("alert")).to_contain_text("igual o posterior")
    captura("02-validacion.png", "2. Validación de fechas",
            "El formulario muestra un error al intentar guardar una entrega anterior a la recepción.")
    pagina.get_by_label("Entrega estimada").fill("2026-11-27")
    expect(pagina.get_by_label("Componente recibido")).to_have_value("Culata")
    captura("03-correccion.png", "3. Corrección del formulario",
            "El operador corrige la entrega al 27 de noviembre. Los datos del trabajo se conservan.")
    guardar.click()
    expect(pagina.get_by_role("status")).to_have_text("Orden registrada.")
    orden = pagina.locator("article")
    expect(orden).to_have_count(1)
    expect(orden).to_contain_text("Culata")
    captura("04-alta.png", "4. Alta confirmada",
            "La aplicación confirma el registro y muestra una única orden numerada para el cliente.")
    pagina.get_by_role("link", name="Volver a clientes").click()
    fila.get_by_role("link", name="Ver órdenes").click()
    expect(pagina.get_by_role("heading", name=nombre, exact=True)).to_be_visible()
    expect(orden).to_have_count(1)
    expect(orden).to_contain_text("Rectificar y verificar planitud")
    expect(orden.locator("time")).to_have_text(["2026-11-25", "2026-11-27"])
    captura("05-consulta.png", "5. Consulta posterior",
            "Al volver a abrir las órdenes del cliente se recuperan el servicio y las fechas guardadas.")
    generar_pdf(navegador, figuras, salida / "Maestranza-CHOKO-I2.pdf")


if __name__ == "__main__":
    demostrar()
