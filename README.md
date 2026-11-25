# Maestranza CHOKO

Primer paso del incremento I1 de la monograf�a. El m�dulo de clientes permite registrar y buscar por nombre desde la capa de persistencia. La interfaz web y la autenticaci�n quedan para el siguiente paso.

## Ejecutar las pruebas

Se requiere Java 21, Maven 3.9 y una base PostgreSQL de pruebas vac�a. Aplicar db/001_clientes.sql a esa base y configurar CHOKO_DB_URL (URL JDBC), CHOKO_DB_USER y CHOKO_DB_PASSWORD. Ejecutar `mvn verify`. La prueba de integraci�n revierte sus datos al finalizar. El workflow de GitHub Actions prepara una base temporal con PostgreSQL 17.

Como decisi�n inicial, el modelo exige un nombre y admite un tel�fono opcional. Los l�mites son 150 y 40 caracteres respectivamente, sujetos a validaci�n con el taller.
