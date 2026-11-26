# Maestranza CHOKO

Sistema interno de gestión y seguimiento de órdenes de trabajo. El incremento I1 permite al personal iniciar sesión, registrar clientes y buscarlos por nombre desde el navegador. El nombre es obligatorio y el teléfono es opcional.

En I2, el enlace **Ver órdenes** de cada cliente permite consultar sus trabajos y abrir una orden con componente, servicios solicitados y fechas. Las órdenes se numeran automáticamente y su registro requiere el rol `OPERADOR`.

En I3, **Ver seguimiento** abre los repuestos y el historial de una orden. El operador registra cantidades, cambios de estado y avisos realizados fuera del sistema por llamada o WhatsApp. El historial conserva el usuario de sesión y la fecha de registro. La fecha del aviso identifica cuándo se contactó al cliente.

El flujo inicial propuesto es Recibida → En proceso → Lista para entrega → Entregada. Cada orden empieza sin seguimiento registrado, incluidas las anteriores a I3. El primer cambio de estado lo confirma el operador.

## Ejecutar en laboratorio

1. Instalar Java 21, Maven 3.9 y PostgreSQL 17. Crear una base de laboratorio y aplicar `db/001_clientes.sql`, `db/002_ordenes.sql` y `db/003_seguimiento.sql` en ese orden, cada uno una sola vez con el usuario que utilizará la aplicación.
   Para actualizar una base existente, aplicar solo los scripts posteriores al último incremento instalado.
2. Configurar `CHOKO_DB_URL` (por ejemplo, `jdbc:postgresql://localhost:5432/choko`), `CHOKO_DB_USER` y `CHOKO_DB_PASSWORD`.
3. Definir `CHOKO_WEB_USER` y `CHOKO_WEB_PASSWORD` con las credenciales del operador de laboratorio. La cuenta inicial se configura mediante el entorno.
4. Ejecutar `mvn spring-boot:run`, abrir http://localhost:8080/clientes e iniciar sesión.

## Pruebas

Para ejecutar `mvn verify`, apuntar las variables de conexión a una base de pruebas con el esquema aplicado. GitHub Actions prepara PostgreSQL, ejecuta las pruebas de dominio, persistencia y acceso web, y empaqueta el JAR.

## Laboratorio con Docker Compose

En Ubuntu con Docker y Compose, definir las variables `CHOKO_DB_PASSWORD`, `CHOKO_WEB_USER` y `CHOKO_WEB_PASSWORD` en el entorno o en un archivo local `.env`. Ejecutar `docker compose up -d --build` desde la raíz del repositorio y abrir http://localhost:8080/clientes. Desde otro equipo se puede acceder mediante un túnel SSH al puerto 8080 del servidor.

PostgreSQL inicializa los scripts de `db/` por orden de nombre en el primer arranque con un volumen vacío. Para un volumen existente, aplicar las migraciones pendientes según el incremento instalado. `docker compose down` detiene el laboratorio y conserva el volumen de datos. Al ejecutar `docker compose up -d` se recuperan los registros.

El workflow de laboratorio despliega los dos servicios en Ubuntu, ejecuta la demostración de I1–I3 en Chrome y comprueba que el historial ficticio se conserva después de recrear los contenedores.
