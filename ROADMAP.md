# Roadmap de Desarrollo — Dashboard de Entrenamiento Personal

> Cada fase debe cerrar con la aplicación funcionando y sus pruebas en verde antes de avanzar a la siguiente. Cada fase crea sus propias pruebas; la Fase 8 las completa. Las referencias `§N` apuntan a `SPECS.md`.

## Fase 1 — Inicializar el proyecto (completada)
- Crear el proyecto con Spring Initializr (Java 21, Maven, `groupId` `com.example`, `artifactId` `training-dashboard`, paquete `com.example.trainingdashboard`) con las dependencias `web`, `data-jpa`, `validation` y `postgresql` (§5). Sin Lombok, Thymeleaf, DevTools ni Docker Compose.
- Crear la estructura de paquetes: `controller`, `service`, `repository`, `entity`, `dto`, `enums`, `exception`, `config`.
- Configurar `application.properties` y el perfil `test` según el §14: variables de entorno con valores por defecto locales, `DB_PASSWORD` sin valor por defecto y `spring.jpa.open-in-view=false`.
- Crear el bean `Clock` en `config` con la zona de `app.time-zone` (§9.1).
- Crear las bases `training_dashboard` y `training_dashboard_test`.
- Agregar `.gitignore` (`target/`, archivos del IDE) y verificar que la aplicación arranque.
- Primer test: el contexto carga contra la base de prueba.
- Auditar los nombres de propiedades contra los metadatos de los jars: Spring ignora en silencio las que no reconoce.

## Fase 2 — Modelo (completada)
- Crear el enum `WorkoutType` con la etiqueta en español y el factor `caloriesPerMinute` de cada valor (§8.1).
- Crear la entidad `Workout` según el §7: tabla `workouts`, columnas y restricciones `NOT NULL`, `@Enumerated(EnumType.STRING)`, sin `equals`, `hashCode` ni `toString`.
- Crear `WorkoutRepository` con `findByDateBetween` y `findAllByOrderByDateDescIdDesc`.
- Crear los DTO como `record`: `WorkoutRequest` (Bean Validation con los mensajes exactos del §12.2), `WorkoutResponse`, `WorkoutTypeResponse` y `ErrorResponse`.
- Tests: factor y etiqueta de cada tipo; integración que verifique el esquema real (tabla y columnas) y la consulta por rango con fechas inclusivas.

## Fase 3 — Registro y consulta (completada)
- Crear `WorkoutService`, `WorkoutController`, `WorkoutTypeController`, `WorkoutNotFoundException` y `GlobalExceptionHandler`.
- Rutas: `POST /api/workouts`, `GET /api/workouts`, `DELETE /api/workouts/{id}` y `GET /api/workout-types` (§11).
- Calcular las calorías en el Service; ignorar `calories` e `id` enviados por el cliente (§8.3).
- Devolver el cuerpo `ErrorResponse` del §12 con los mensajes exactos: validación por campo, tipo desconocido, duración con decimales o en texto, fecha mal formada, cuerpo ilegible, id no numérico, id inexistente y 500 genérico sin detalles.
- Listar ordenado por fecha descendente y luego por id descendente.
- Crear `JacksonConfig` en `config` con la lectura estricta del §10: `durationMinutes` solo entero JSON, `type` solo el código exacto como texto y `date` solo una cadena `AAAA-MM-DD` válida; la cadena vacía cuenta como ausente. Los valores por defecto de Jackson 3 aceptan en silencio `30.5`, `"30"`, `type: 0` y fechas numéricas.
- Registrar en el log del 500 el método, la ruta y la clase y los frames de cada causa, nunca los mensajes (§12.1).
- Tests: unitarios del Service (calorías de cada tipo con duraciones 1 y 1440), flujo con `MockMvc` para cada mensaje y código de estado (incluidas las duraciones 0 y 1441 y cada valor de lectura estricta) y una prueba del log del 500 que verifique que no filtra mensajes.

## Fase 4 — Dashboard semanal (completada)
- Crear `DashboardService` y `DashboardController` con `GET /api/dashboard/weekly`.
- Determinar la semana actual de lunes a domingo con el `Clock` inyectado, con fechas inclusivas (§9.2).
- Calcular las tres métricas en Java a partir de `findByDateBetween`, en un único método reutilizable (§9.4); una semana vacía devuelve ceros.
- Tests con `Clock.fixed`: lunes, mitad de semana y domingo; fechas en los bordes; cruce de mes y de año; fecha futura dentro de la semana actual; semana vacía.

## Fase 5 — Comparativa (completada)
- Crear `GET /api/dashboard/comparison` con la semana actual, la anterior y la diferencia `actual - anterior` (§9.3 y §9.5), reutilizando el método de métricas.
- Tests: diferencias positivas, negativas y cero; semana anterior vacía; los números del §15 (5 / 180 / 1200 contra 4 / 150 / 980).

## Fase 6 — Frontend
- Crear `index.html`, `dashboard.html`, `comparison.html`, `styles.css` y los módulos de `js/` del §13, sin librerías ni CDN.
- Registro: selector cargado desde `/api/workout-types`, formulario, errores por campo, mensajes del §12.4 y tabla con eliminación y confirmación. La duración se envía como número entero JSON, no como texto (§10).
- Dashboard y comparativa con los formatos del §13.4 (fechas `DD/MM/AAAA`, indicadores `▲`, `▼` y `=`).
- `/` responde 200 (health check).
- Verificar en el navegador con `PORT=8081`: flujos felices, errores, estados vacíos con una base temporal y vista de 375 px.
- Tests: `GET /` y las páginas estáticas responden 200.

## Fase 7 — Datos de demo
- Crear `DemoDataLoader` con el perfil `demo` según el §15: revisión al arrancar y una vez al día (00:05 en la zona de `app.time-zone`) que reemplaza todos los datos cuando la semana actual no tiene entrenamientos; carga a través de `WorkoutService`, en una sola transacción y con fechas relativas al `Clock`.
- La revisión nunca detiene la aplicación: ante una falla se registra a `ERROR` sin credenciales, se conservan los datos anteriores y se reintenta en la siguiente revisión. No se ejecuta de forma concurrente.
- Declarar el loader como no perezoso (`@Lazy(false)`): con `SPRING_MAIN_LAZY_INITIALIZATION=true` en Render, un bean perezoso podría no registrar su tarea programada.
- Los datos deben producir exactamente 5 / 180 / 1200 en la semana actual y 4 / 150 / 980 en la anterior.
- Preparar los cinco escenarios de demo del §15.1.
- Tests: totales exactos con `Clock.fixed`; no toca los datos si la semana actual tiene entrenamientos; reemplaza datos viejos y tabla vacía; carga todo o nada (una falla conserva los datos anteriores y no detiene el arranque); la tarea programada queda registrada con la inicialización perezosa activada; arranque sobre una base recién creada.

## Fase 8 — Pruebas y pulido
- Completar los tres niveles de pruebas del §16 y revisar que cubran los casos prioritarios.
- Comprobar que cada prueba de integración falla si falta lo que protege (mutaciones puntuales tras el commit).
- Revisar validaciones, navegación y accesibilidad básica (etiquetas asociadas, contraste, foco); mejorar la presentación visual sin sobrecargar el proyecto.
- Verificar que ningún error muestre trazas, SQL ni mensajes de librerías.
- Auditoría de consistencia entre `SPECS.md` y el código (solo lectura).

## Fase 9 — Documentación y portafolio
- Completar el README: guardar en `docs/` las capturas y el GIF con los nombres exactos que ya referencia (minúsculas y extensión exacta), grabados con los datos de demo recién cargados.
- Verificar que no haya credenciales en el repositorio ni en su historial, y que `.opencode` solo contenga `commands/fase.md`.
- Revisar que ninguna captura muestre datos personales.
- Revisar la consistencia entre `SPECS.md`, el README y la implementación final.
- Preparar, fuera del repositorio, la explicación de las decisiones técnicas para entrevistas: semana derivada con `Clock` y zona configurable, calorías en el backend, DTO como `record`, formato único de errores, y por qué Render exige Docker.

## Fase 10 — Despliegue en Render + Neon
- Desplegar solo cuando el proyecto esté terminado y listo para mostrarse.
- Usar una cuenta de Render propia para este proyecto. Si Render pide una tarjeta de verificación al crearla, avisar antes de seguir.
- Crear el `Dockerfile` y el `.dockerignore` del §17.3: es el único uso permitido de Docker.
- Crear el proyecto en Neon (Ohio), con conexión directa, y comprobar la conexión con `psql`. Verificar el plan gratuito vigente.
- Arrancar el JAR localmente contra Neon con el perfil `demo`: crea el esquema y carga los datos.
- Crear el Web Service en Render (Docker, `main`, Ohio, Free, health check `/`) con las variables del §17.4. Las variables secretas no se pegan en el chat.
- Con el esquema creado, agregar `SPRING_MAIN_LAZY_INITIALIZATION=true` y `SPRING_JPA_HIBERNATE_DDL_AUTO=none`.
- Configurar un monitor (UptimeRobot) que visite `/` con un intervalo menor a 15 minutos.
- Recorrer los cinco escenarios de demo contra la URL pública, incluida la vista móvil.
- Poner la URL de la demo en el README, agregar descripción y temas en GitHub y hacer público el repositorio; comprobar con `gh repo view --json visibility`.

---

**Principios a mantener durante todo el desarrollo:**
- Simplicidad y buenas prácticas por encima de tecnología innecesaria.
- Lógica de negocio únicamente en los Services.
- La semana se deriva de las fechas con un `Clock` inyectado, nunca de un valor guardado.
- Las calorías las calcula solo el backend.
- Credenciales fuera del repositorio.
- Despliegue solo al final y solo con datos de demo.
