# AGENTS.md

Instrucciones para agentes de IA y para quien contribuya a **Dashboard de Entrenamiento Personal**. Este archivo apunta a los demás documentos en lugar de copiarlos: ante cualquier duda, el documento enlazado manda.

---

## Orden de lectura y precedencia

Leer en este orden antes de escribir código:

1. `SPECS.md`: qué se construye y qué queda fuera. Es la fuente de verdad.
2. `CODESTYLE.md`: cómo se escribe el código.
3. `ROADMAP.md`: en qué orden se construye.
4. `README.md`: vista externa del proyecto terminado.

Si dos documentos se contradicen, gana el de menor número. Si el SPECS parece incompleto o ambiguo, señalarlo y preguntar; no desviarse en silencio.

---

## Proyecto y comandos

API REST monolítica en Java 21 con Spring Boot, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL y Maven, más un frontend estático de HTML, CSS y JavaScript vanilla servido por la misma aplicación. Paquete base: `com.example.trainingdashboard`. Sin Lombok.

```bash
./mvnw spring-boot:run
./mvnw test
```

Variables de entorno: `DB_PASSWORD` (obligatoria, sin valor por defecto), `DB_URL`, `DB_USERNAME`, `PORT`, `APP_TIME_ZONE` y `TEST_DB_URL` (opcionales, con valores por defecto para entorno local; SPECS §14). `./mvnw test` necesita una base PostgreSQL local llamada `training_dashboard_test`. Para cargar los datos de demo: `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run`.

---

## Reglas del proyecto

Detalle en el SPECS; estas son las que no se negocian.

- Arquitectura por capas: `Controller` → `Service` → `Repository`. La lógica de negocio vive solo en los Services. Los controllers reciben, validan y devuelven DTOs.
- Las peticiones y respuestas de la API son DTO `record`. Las entidades JPA nunca salen del Service ni llegan al JSON (SPECS §6).
- Las calorías las calcula solo el backend: `durationMinutes × caloriesPerMinute` del `WorkoutType`, que guarda el factor y la etiqueta en español. El cliente no las envía y un `calories` o `id` recibido se ignora (SPECS §8).
- La semana se deriva de las fechas y no se guarda: lunes a domingo, fechas inclusivas, calculada en el backend (SPECS §9). El frontend nunca envía `startDate` ni `endDate`.
- La fecha actual sale siempre de un `Clock` inyectado, con la zona de `app.time-zone`. Nunca `LocalDate.now()` sin reloj en la lógica de negocio.
- El dashboard y la comparativa usan el mismo método para calcular las métricas de un período. Las diferencias son `actual - anterior`, absolutas y sin porcentajes.
- `durationMinutes` es un entero de 1 a 1440; el tipo es uno de los cinco códigos del enum; la fecha es obligatoria. No se rechazan fechas futuras (SPECS §9.6 y §10).
- Los totales de las métricas son `long`.
- Solo existe `DELETE /api/workouts/{id}` (borrado duro). No hay `PUT` ni edición.
- Todo error de la API usa el cuerpo `ErrorResponse` (`message` y `fieldErrors`) con los mensajes exactos del SPECS §12. Un error no previsto devuelve el 500 genérico, sin trazas, SQL ni mensajes de librerías.
- JSON en `camelCase`, rutas en inglés y en plural, y valores controlados como enums (`RUNNING`, `CYCLING`, `SWIMMING`, `WEIGHTS`, `WALKING`).
- Los mensajes y textos que ve la persona usuaria están en español (SPECS §12 y §13).
- El frontend es JavaScript vanilla sin librerías ni CDN; los datos de la API se insertan con `textContent` (SPECS §13).
- No hay autenticación ni autorización. No agregarlas.
- El esquema se gestiona con `ddl-auto=update` en desarrollo y demo, `create-drop` en pruebas y `none` en Render (SPECS §14).
- Las pruebas de integración usan la base PostgreSQL local `training_dashboard_test` con el perfil `test`. Sin H2 ni Testcontainers (SPECS §16).
- Los datos de demo se cargan y se recargan solos con el perfil `demo` cuando la semana actual queda vacía, con fechas relativas al `Clock`. La revisión nunca detiene la aplicación (SPECS §15).
- Credenciales y secretos nunca se versionan; la configuración entra por variables de entorno.
- Sin Lombok ni DevTools.

---

## Alcance cerrado

No agregar nada de la lista de exclusiones del SPECS §3: autenticación, usuarios, roles, edición de entrenamientos, paginación, porcentajes, métricas adicionales, gráficos, frameworks o librerías de JavaScript, Thymeleaf, migraciones, microservicios, Redis, CI/CD ni infraestructura como código. Tampoco dependencias fuera del stack del SPECS. Si algo parece necesario, preguntar primero.

El despliegue en Render + Neon es la Fase 10. Render no ejecuta Java de forma nativa, así que Docker se permite solo para ese fin: un único `Dockerfile` y su `.dockerignore` en la raíz, creados en la Fase 10 (SPECS §17). No hay `docker-compose` ni Docker en local. En Render solo se usan datos de demo, porque la aplicación no tiene autenticación.

---

## Estilo de código

`CODESTYLE.md` aplica completo. Resumen de lo que más se rompe:

- Todo el código está en inglés: identificadores (incluidos los nombres del dominio: `Workout`, `WorkoutType`), tablas, rutas, nombres de archivos, comentarios, commits, logs y tests. El vocabulario completo está en el SPECS §21.
- El español se usa solo en lo que ve la persona usuaria: textos de la interfaz, mensajes de validación y de error de la API y mensajes de excepciones de negocio.
- Sin separadores decorativos, sin comentarios que repiten el código, sin código comentado.
- Commits convencionales en inglés, por ejemplo `feat(workout): reject non-integer durations`.
- Logging con SLF4J, nunca `System.out`, y nunca se registran credenciales de la base de datos.

---

## Forma de trabajo

- Seguir el `ROADMAP.md` fase por fase. No empezar una fase sin cerrar la anterior.
- Cerrar una fase significa que la aplicación arranca, `./mvnw test` está en verde y los tests de esa fase existen. Después, agregar `(completada)` al título de la fase en `ROADMAP.md`.
- Cambios pequeños y enfocados: una fase, un tema por commit.
- Ante una ambigüedad entre lo pedido y el SPECS, preguntar en lugar de asumir.

---

## Definición de terminado

- [ ] Compila y `./mvnw test` pasa
- [ ] La aplicación arranca y el flujo afectado funciona en el navegador o con `curl`
- [ ] Respeta las reglas del proyecto y el CODESTYLE
- [ ] Sin credenciales, sin `System.out`, sin código comentado
- [ ] Mensajes de commit en inglés y convencionales
- [ ] Fase marcada en `ROADMAP.md`
- [ ] `SPECS.md` y `README.md` actualizados si cambió el comportamiento
