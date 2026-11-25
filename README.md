# Maestranza CHOKO

Sistema interno de gestión y seguimiento de órdenes de trabajo. El incremento I1 permite al personal iniciar sesión, registrar clientes y buscarlos por nombre desde el navegador. El nombre es obligatorio y el teléfono es opcional.

En I2, el enlace **Ver órdenes** de cada cliente permite consultar sus trabajos y abrir una orden con componente, servicios solicitados y fechas. Las órdenes se numeran automáticamente y su registro requiere el rol `OPERADOR`.

## Ejecutar en laboratorio

1. Instalar Java 21, Maven 3.9 y PostgreSQL 17. Crear una base de laboratorio y aplicar `db/001_clientes.sql` y luego `db/002_ordenes.sql`, cada uno una sola vez con el usuario que utilizará la aplicación.
   Si la base ya contiene el esquema I1, aplicar únicamente `db/002_ordenes.sql`.
2. Configurar `CHOKO_DB_URL` (por ejemplo, `jdbc:postgresql://localhost:5432/choko`), `CHOKO_DB_USER` y `CHOKO_DB_PASSWORD`.
3. Definir `CHOKO_WEB_USER` y `CHOKO_WEB_PASSWORD` con las credenciales del operador de laboratorio. La cuenta inicial se configura mediante el entorno.
4. Ejecutar `mvn spring-boot:run`, abrir http://localhost:8080/clientes e iniciar sesión.

## Pruebas

Para ejecutar `mvn verify`, apuntar las variables de conexión a una base de pruebas con el esquema aplicado. GitHub Actions prepara PostgreSQL, ejecuta las pruebas de dominio, persistencia y acceso web, y empaqueta el JAR.
