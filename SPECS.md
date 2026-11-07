# SPECS.md — Dashboard de Entrenamiento Personal

Fuente de verdad del proyecto. Los demás documentos apuntan a este. Los nombres del dominio (tablas, rutas, clases, enums) van en inglés; el glosario está en el §21.

---

## 1. Descripción del proyecto

### 1.1 Nombre

**Dashboard de Entrenamiento Personal**

### 1.2 Objetivo

MVP para un gimnasio que permite registrar entrenamientos y consultar métricas semanales de forma sencilla. Tiene tres funcionalidades principales:

1. Registrar un entrenamiento.
2. Consultar las métricas de la semana actual.
3. Comparar la semana actual con la semana anterior.

Es un ejercicio práctico de **Java + Spring Boot + API REST + PostgreSQL** y un proyecto de portafolio. Se mantiene deliberadamente pequeño y sin funcionalidades comerciales adicionales.

---

## 2. Alcance incluido

- Registro de entrenamientos: tipo, duración en minutos y fecha.
- Cálculo automático de las calorías estimadas en el backend.
- Persistencia en PostgreSQL.
- Consulta y eliminación de entrenamientos.
- Métricas de la semana actual.
- Comparación de la semana actual con la semana anterior.
- Interfaz web sencilla (tres páginas estáticas) que consume la API REST.
- Datos de demo y despliegue de la demo en Render (Fase 10 del ROADMAP).

---

## 3. Fuera de alcance

El MVP no incluye:

- Autenticación, registro de usuarios, roles, permisos ni JWT.
- Rutinas, ejercicios individuales, series, repeticiones, peso corporal ni frecuencia cardíaca.
- Integración con dispositivos externos o smartwatches.
- Metas personales, notificaciones, exportación ni historial avanzado.
- Edición de entrenamientos (no hay `PUT`; ver §19).
- Paginación, porcentajes de progreso y métricas adicionales a las tres del §9.4.
- Gráficos, frameworks o librerías de JavaScript, CDN, Thymeleaf.
- Lombok y DevTools.
- Microservicios, Redis, mensajería, CQRS, eventos o arquitectura hexagonal.
- Migraciones de base de datos (Flyway, Liquibase), H2 y Testcontainers.
- Docker, salvo el único `Dockerfile` de la raíz (con su `.dockerignore`) que Render necesita para construir la imagen (§17). No hay `docker-compose` ni se usa Docker en local.
- CI/CD, infraestructura como código y AWS.
- El despliegue no forma parte del alcance funcional: es la última fase del ROADMAP y no cambia el comportamiento de la aplicación.

---

## 4. Flujo funcional general

```text
Persona usuaria
  │
  ├── 1. Abrir la página de registro (/)
  │       ├── Seleccionar tipo (lista desde GET /api/workout-types)
  │       ├── Ingresar duración
  │       └── Seleccionar fecha
  │
  ├── 2. Enviar el entrenamiento (POST /api/workouts)
  │       ├── Validar datos
  │       ├── Obtener el factor calórico del tipo
  │       ├── Calcular calorías
  │       └── Guardar
  │
  ├── 3. Ver o eliminar entrenamientos (GET y DELETE /api/workouts)
  │
  ├── 4. Abrir el dashboard (GET /api/dashboard/weekly)
  │       ├── Determinar la semana actual
  │       ├── Consultar los entrenamientos del período
  │       └── Calcular las métricas
  │
  └── 5. Abrir la comparativa (GET /api/dashboard/comparison)
          ├── Determinar la semana actual y la anterior
          ├── Calcular las métricas de ambas
          └── Calcular las diferencias
```

---

## 5. Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 LTS |
| Framework | Spring Boot 4.1.x (la versión que genere Initializr) |
| Web | Spring Web MVC (API REST, JSON) |
| Persistencia | Spring Data JPA, PostgreSQL |
| Validación | Bean Validation |
| Construcción | Maven (con Maven Wrapper, `./mvnw`) |
| Pruebas | JUnit, AssertJ, Mockito y MockMvc (los starters de prueba de Spring Boot) |
| Frontend | HTML5, CSS3 y JavaScript vanilla (módulos ES) en `src/main/resources/static` |

Dependencias de Initializr: `web`, `data-jpa`, `validation` y `postgresql`. No se agregan otras sin aprobación.

El frontend lo sirve la misma aplicación: un solo origen, sin CORS.

---

## 6. Arquitectura

Arquitectura por capas sencilla:

```text
Controller → Service → Repository → PostgreSQL
```

### 6.1 Capas

- **Controller:** recibe la solicitud, valida con Bean Validation, delega en el Service y devuelve DTOs. Sin lógica de negocio.
- **Service:** contiene toda la lógica: calcular calorías, determinar las semanas, construir métricas y comparativa, y eliminar. No depende de tipos web.
- **Repository:** acceso a PostgreSQL con Spring Data JPA. Sin lógica de negocio.
- **DTO:** las peticiones y respuestas de la API son `record`. Las entidades nunca salen del Service hacia el Controller ni hacia el JSON.
- **Entity y enum:** `Workout` y `WorkoutType`.

### 6.2 Paquetes

```text
com.example.trainingdashboard
├── controller   WorkoutController, WorkoutTypeController, DashboardController
├── service      WorkoutService, DashboardService
├── repository   WorkoutRepository
├── entity       Workout
├── dto          WorkoutRequest, WorkoutResponse, WorkoutTypeResponse,
│                WeeklyMetricsResponse, WeeklyDifferenceResponse,
│                WeeklyComparisonResponse, ErrorResponse
├── enums        WorkoutType
├── exception    GlobalExceptionHandler, WorkoutNotFoundException
└── config       ClockConfig, DemoDataLoader
```

Los nombres pueden ajustarse durante la implementación sin cambiar el alcance. Las capas y sus reglas no se ajustan.

---

## 7. Modelo de dominio

Una única entidad: `Workout`, tabla `workouts`.

| Campo | Tipo Java | Columna | Reglas |
|---|---|---|---|
| `id` | `Long` | `id BIGINT` | PK autogenerada |
| `type` | `WorkoutType` | `type VARCHAR(20) NOT NULL` | `@Enumerated(EnumType.STRING)` |
| `durationMinutes` | `Integer` | `duration_minutes INTEGER NOT NULL` | De 1 a 1440 |
| `calories` | `Integer` | `calories INTEGER NOT NULL` | Calculado por el backend |
| `date` | `LocalDate` | `workout_date DATE NOT NULL` | Obligatoria |

- No hay usuarios: los datos son globales y no existe una relación `Workout → User`.
- El estado de la semana no se guarda: se deriva de las fechas (§9).
- Las calorías se almacenan al registrar. Si algún día cambiara un factor, los registros anteriores conservan su valor.
- La entidad no define `equals`, `hashCode` ni `toString`.

---

## 8. Tipos de entrenamiento y calorías

### 8.1 Enum `WorkoutType`

El enum guarda, junto a cada valor, su etiqueta en español y su factor calórico. Es la única fuente de verdad: ni el frontend ni otras clases duplican estos datos.

| Código (JSON y base de datos) | Etiqueta en la interfaz | kcal/min |
|---|---|---:|
| `RUNNING` | Correr | 10 |
| `CYCLING` | Ciclismo | 8 |
| `SWIMMING` | Natación | 9 |
| `WEIGHTS` | Pesas | 6 |
| `WALKING` | Caminata | 5 |

El orden de declaración es el orden en que se muestran las opciones.

### 8.2 Fórmula

```text
calories = durationMinutes × caloriesPerMinute
```

Ejemplo: `RUNNING`, 30 minutos, factor 10 → `30 × 10 = 300 kcal`. El resultado es un entero.

### 8.3 Responsabilidad

El cliente **no envía las calorías**. La API recibe únicamente tipo, duración y fecha; el backend determina el factor, calcula y guarda. Un campo `calories` o `id` enviado por el cliente se ignora.

---

## 9. Semanas y métricas

### 9.1 Tiempo

- La fecha actual sale siempre de un `Clock` inyectado. Nunca `LocalDate.now()` sin reloj en la lógica de negocio.
- La zona del `Clock` se configura con `APP_TIME_ZONE` (propiedad `app.time-zone`), por defecto `America/Guayaquil`. Una zona inválida detiene el arranque.
- Las pruebas usan `Clock.fixed`.

### 9.2 Semana actual

Va de lunes a domingo, con fechas inclusivas:

```text
start = hoy.with(previousOrSame(MONDAY))
end   = start + 6 días
start <= date <= end
```

No depende de la configuración regional del servidor.

### 9.3 Semana anterior

Comienza siete días antes del inicio de la semana actual y termina un día antes de este.

```text
Semana actual     10/08/2026 → 16/08/2026
Semana anterior   03/08/2026 → 09/08/2026
```

### 9.4 Métricas

Tres métricas obligatorias por período, calculadas en el Service a partir de `findByDateBetween(start, end)`:

- Cantidad de entrenamientos.
- Minutos totales: suma de `durationMinutes`.
- Calorías totales: suma de `calories`.

Los totales son `long`. Un período sin entrenamientos produce ceros, nunca un error. No se agregan métricas sin una necesidad concreta.

### 9.5 Comparativa

Exactamente dos períodos: semana actual y semana anterior. La diferencia es absoluta y se calcula `actual - anterior`: positivo es incremento y negativo es disminución. No hay porcentajes.

El dashboard y la comparativa usan el mismo método para calcular las métricas de un período.

### 9.6 Reglas de fechas

- No se rechazan fechas futuras. Un entrenamiento con fecha dentro de la semana actual cuenta en el dashboard aunque sea posterior a hoy; uno de semanas posteriores se guarda y aparece cuando llegue su semana.
- No hay una fecha mínima.
- Limitación conocida: la semana actual está incompleta hasta el domingo y se compara contra una semana anterior completa, así que las diferencias salen negativas al inicio de la semana. Se mantiene así y se documenta en el README.

---

## 10. Validaciones

| Campo | Regla |
|---|---|
| `type` | Obligatorio; debe ser uno de los cinco códigos de §8.1 |
| `durationMinutes` | Obligatorio; entero de 1 a 1440 (un día completo) |
| `date` | Obligatoria; formato ISO `AAAA-MM-DD` |
| `calories` | No se recibe |

La lectura de los valores es estricta. Se aceptan solo estas formas JSON y cualquier otra se rechaza con 400:

- `type`: una cadena con uno de los cinco códigos exactos, en mayúsculas. Se rechazan los números (por ejemplo `0`), las minúsculas y cualquier otro tipo JSON.
- `durationMinutes`: un entero JSON. Se rechazan los decimales (`30.5` y `30.0`), el texto (incluido `"30"`), los booleanos, los arreglos y los objetos.
- `date`: una cadena `AAAA-MM-DD` con cuatro dígitos de año y una fecha válida del calendario. Se rechazan los números, los arreglos, la fecha con hora, los años con signo o de más de cuatro dígitos, los espacios sobrantes y las fechas imposibles como `2026-02-30`.
- Una cadena vacía cuenta como ausente y produce el mensaje de campo obligatorio.

`JacksonConfig` aplica estas reglas, porque los valores por defecto de Jackson 3 aceptan en silencio varios de ellos (por ejemplo, `30.5` como 30 o `0` como `RUNNING`). Los mensajes exactos están en el §12.

---

## 11. API REST

Convenciones: JSON de entrada y salida, campos en `camelCase`, fechas en formato ISO `AAAA-MM-DD`, rutas en inglés y en plural para recursos. Los endpoints semanales no reciben parámetros: el backend determina los períodos.

| Operación | Ruta | Éxito | Errores |
|---|---|---|---|
| Registrar | `POST /api/workouts` | 201 | 400 |
| Listar | `GET /api/workouts` | 200 | |
| Eliminar | `DELETE /api/workouts/{id}` | 204 | 400 (id no numérico), 404 |
| Tipos | `GET /api/workout-types` | 200 | |
| Dashboard semanal | `GET /api/dashboard/weekly` | 200 | |
| Comparativa | `GET /api/dashboard/comparison` | 200 | |

Cualquier error no previsto produce 500 mediante el manejo global (§12). Una ruta inexistente usa la respuesta por defecto de Spring y no se personaliza. El 201 no incluye cabecera `Location`, porque no existe un `GET` por id.

### 11.1 Registrar

```http
POST /api/workouts
```

```json
{ "type": "RUNNING", "durationMinutes": 30, "date": "2026-08-12" }
```

Respuesta 201:

```json
{ "id": 1, "type": "RUNNING", "durationMinutes": 30, "calories": 300, "date": "2026-08-12" }
```

### 11.2 Listar

Devuelve un arreglo de `WorkoutResponse`, ordenado por `date` descendente y luego por `id` descendente. Sin paginación. Sin entrenamientos devuelve `[]`.

### 11.3 Eliminar

Borrado duro: no hay historial que conservar. Respuesta 204 sin cuerpo.

### 11.4 Tipos

```json
[
  { "code": "RUNNING", "label": "Correr", "caloriesPerMinute": 10 },
  { "code": "CYCLING", "label": "Ciclismo", "caloriesPerMinute": 8 },
  { "code": "SWIMMING", "label": "Natación", "caloriesPerMinute": 9 },
  { "code": "WEIGHTS", "label": "Pesas", "caloriesPerMinute": 6 },
  { "code": "WALKING", "label": "Caminata", "caloriesPerMinute": 5 }
]
```

### 11.5 Dashboard semanal

```json
{
  "startDate": "2026-08-10",
  "endDate": "2026-08-16",
  "totalWorkouts": 5,
  "totalDurationMinutes": 180,
  "totalCalories": 1200
}
```

### 11.6 Comparativa

```json
{
  "currentWeek": {
    "startDate": "2026-08-10", "endDate": "2026-08-16",
    "totalWorkouts": 5, "totalDurationMinutes": 180, "totalCalories": 1200
  },
  "previousWeek": {
    "startDate": "2026-08-03", "endDate": "2026-08-09",
    "totalWorkouts": 4, "totalDurationMinutes": 150, "totalCalories": 980
  },
  "difference": { "workouts": 1, "durationMinutes": 30, "calories": 220 }
}
```

---

## 12. Errores y mensajes

### 12.1 Formato

Todo error de la API tiene este cuerpo:

```json
{
  "message": "Los datos del entrenamiento no son válidos",
  "fieldErrors": { "durationMinutes": "La duración debe ser mayor que 0" }
}
```

`fieldErrors` siempre está presente y es `{}` cuando el error no corresponde a un campo. Los errores de formato (cuerpo ilegible, tipo desconocido, duración no entera, fecha mal formada) se detectan al leer el cuerpo y se informa uno solo. Los de validación se informan todos juntos.

Nunca se muestran trazas, SQL ni mensajes de librerías. Ningún mensaje contiene credenciales.

Un error no previsto se registra a `ERROR` con el método, la ruta (sin query string) y la clase y los frames de la excepción y de cada causa. Nunca se registran los mensajes de las excepciones (podrían contener cadenas de conexión), el cuerpo de la solicitud, las cabeceras ni las variables de entorno.

Un entero fuera del rango de `int` se rechaza con 400, con el mensaje de la duración o con el de cuerpo inválido.

### 12.2 Mensajes por campo (400)

| Caso | Campo | Mensaje |
|---|---|---|
| Tipo ausente, nulo o cadena vacía | `type` | El tipo de entrenamiento es obligatorio |
| Tipo que no es uno de los códigos exactos (otro código, minúsculas, número u otro tipo JSON) | `type` | El tipo de entrenamiento no es válido |
| Duración ausente, nula o cadena vacía | `durationMinutes` | La duración es obligatoria |
| Duración menor que 1 | `durationMinutes` | La duración debe ser mayor que 0 |
| Duración mayor que 1440 | `durationMinutes` | La duración no puede superar 1440 minutos |
| Duración que no es un entero JSON (decimales, texto, booleano, arreglo u objeto) | `durationMinutes` | La duración debe ser un número entero de minutos |
| Fecha ausente, nula o cadena vacía | `date` | La fecha es obligatoria |
| Fecha que no es una cadena `AAAA-MM-DD` válida (formato, calendario u otro tipo JSON) | `date` | La fecha no es válida (formato AAAA-MM-DD) |

### 12.3 Mensajes generales

| Caso | Estado | `message` |
|---|---|---|
| Cualquier error por campo | 400 | Los datos del entrenamiento no son válidos |
| Cuerpo vacío, JSON mal formado o raíz que no es un objeto | 400 | El cuerpo de la solicitud no es válido |
| `DELETE` con id no numérico | 400 | El identificador del entrenamiento no es válido |
| `DELETE` de un id inexistente | 404 | No se encontró el entrenamiento con id N |
| Error no previsto | 500 | Ocurrió un error inesperado. Inténtalo de nuevo más tarde |

`N` es el id solicitado.

### 12.4 Mensajes de la interfaz

| Situación | Texto |
|---|---|
| Registro exitoso | Entrenamiento registrado: N kcal estimadas |
| Eliminación exitosa | Entrenamiento eliminado |
| Confirmación al eliminar | ¿Eliminar este entrenamiento? |
| Lista vacía | Todavía no hay entrenamientos registrados |
| Falla de red al enviar o eliminar | No se pudo conectar con el servidor. Inténtalo de nuevo más tarde |
| Falla al cargar datos | No se pudo cargar la información. Inténtalo de nuevo más tarde |

`N` en el registro exitoso son las calorías de la respuesta.

---

## 13. Frontend

Tres páginas estáticas en `src/main/resources/static`, con un único `styles.css` y un módulo JavaScript por página más dos módulos compartidos:

```text
static/
├── index.html         registro (ruta /)
├── dashboard.html
├── comparison.html
├── styles.css
└── js/
    ├── api.js         llamadas fetch y lectura del ErrorResponse
    ├── format.js      fechas, diferencias y etiquetas
    ├── register.js
    ├── dashboard.js
    └── comparison.js
```

`/` responde 200 y sirve de health check en Render. Todas las páginas tienen `lang="es"`, un menú con "Registro", "Dashboard" y "Comparativa", y se ven bien desde 375 px de ancho. La interfaz es funcional antes que visualmente compleja.

### 13.1 Registro (`index.html`)

- Selector de tipo, cargado desde `GET /api/workout-types`.
- Campo de duración, campo de fecha (por defecto, la fecha de hoy del navegador) y botón "Registrar". La duración se envía como número entero JSON (no como cadena) y la fecha como `AAAA-MM-DD` (§10).
- Tras registrar: mensaje de éxito (§12.4), se vacía la duración, se conservan tipo y fecha y se refresca la lista.
- Errores por campo bajo el campo correspondiente y `message` en la zona de estado.
- Tabla "Entrenamientos registrados" (Fecha, Tipo, Duración (min), Calorías (kcal), Acciones) con botón "Eliminar" por fila y confirmación.

### 13.2 Dashboard (`dashboard.html`)

"Semana del DD/MM/AAAA al DD/MM/AAAA" y tres tarjetas: "Entrenamientos", "Minutos totales" y "Calorías totales".

### 13.3 Comparativa (`comparison.html`)

Tabla con las columnas "Métrica", "Semana actual", "Semana anterior" y "Diferencia", y las filas "Entrenamientos", "Minutos totales" y "Calorías totales". Cada período muestra su rango de fechas.

### 13.4 Formatos

- Fechas en la interfaz: `DD/MM/AAAA`. La API usa ISO.
- Diferencia: `▲ +30` si es positiva, `▼ -20` si es negativa y `= 0` si es cero. El color acompaña al símbolo y nunca lo reemplaza.
- Todo texto que viene de la API se inserta con `textContent`.

---

## 14. Configuración y perfiles

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/training_dashboard}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
server.port=${PORT:8080}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
app.time-zone=${APP_TIME_ZONE:America/Guayaquil}
```

| Variable | Obligatoria | Valor por defecto |
|---|---|---|
| `DB_PASSWORD` | Sí | Ninguno |
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/training_dashboard` |
| `DB_USERNAME` | No | `postgres` |
| `PORT` | No | `8080` |
| `APP_TIME_ZONE` | No | `America/Guayaquil` |
| `TEST_DB_URL` | No | `jdbc:postgresql://localhost:5432/training_dashboard_test` |

- Perfil por defecto: desarrollo local con `ddl-auto=update`. No hay Flyway: el esquema lo gestiona Hibernate.
- Perfil `demo`: carga y recarga automática de los datos del §15.
- Perfil `test`: `TEST_DB_URL` y `ddl-auto=create-drop`.
- Render: `ddl-auto=none` mediante variable de entorno una vez creado el esquema (§17).
- Credenciales y secretos nunca se versionan. Solo se leen de variables de entorno.

---

## 15. Datos de demo

El perfil `demo` activa `DemoDataLoader`, que mantiene la demo siempre completa:

- Considera que los datos están viejos cuando la semana actual no tiene ningún entrenamiento (incluida la tabla vacía). Lo consulta con `DashboardService`, la única fuente de verdad de las semanas.
- Si están viejos, reemplaza todo el contenido de `workouts` por el conjunto de abajo. Los entrenamientos que haya agregado cualquier visitante también se pierden.
- Revisa al arrancar (como `CommandLineRunner`) y una vez al día a las 00:05 en la zona de `app.time-zone` (`@Scheduled`, sin dependencias nuevas, activo solo con el perfil `demo`). Mientras el servicio duerme no hay revisión: ocurre al despertar.
- Crea los entrenamientos a través de `WorkoutService`, de modo que las calorías las calcula el backend. Solo vaciar la tabla se hace con `WorkoutRepository`, porque no tiene reglas de negocio.
- Hace el vaciado y la carga en una sola transacción: todo o nada. Si falla, se conservan los datos anteriores.
- Nunca detiene la aplicación: ante cualquier falla (por ejemplo, la base no responde) la registra a `ERROR` sin credenciales y reintenta en la siguiente revisión. Una falla al arrancar no impide el arranque.
- Se ejecuta de a una vez: dos revisiones no corren al mismo tiempo.
- Se declara no perezoso (`@Lazy(false)`): con `SPRING_MAIN_LAZY_INITIALIZATION=true` (Render, §17.4) un bean perezoso podría no registrar su tarea programada.
- Calcula las fechas relativas al `Clock`. `start` es el `startDate` de la semana actual y la fecha de cada entrenamiento de la semana actual es `min(start + día, hoy)`.

| Semana | Día (desde el lunes) | Tipo | Minutos | kcal |
|---|---:|---|---:|---:|
| Actual | 0 | `RUNNING` | 20 | 200 |
| Actual | 1 | `WEIGHTS` | 60 | 360 |
| Actual | 2 | `CYCLING` | 20 | 160 |
| Actual | 3 | `SWIMMING` | 20 | 180 |
| Actual | 4 | `WALKING` | 60 | 300 |
| Anterior | 0 | `CYCLING` | 30 | 240 |
| Anterior | 2 | `WEIGHTS` | 40 | 240 |
| Anterior | 4 | `SWIMMING` | 25 | 225 |
| Anterior | 5 | `WALKING` | 55 | 275 |

Totales: semana actual 5 / 180 / 1200 y semana anterior 4 / 150 / 980. Los cinco tipos quedan representados.

### 15.1 Escenarios de demo

Se recorren en este orden, con los datos recién cargados:

| # | Escenario | Resultado esperado |
|---|---|---|
| 1 | Abrir el dashboard | 5 entrenamientos, 180 minutos, 1200 kcal |
| 2 | Abrir la comparativa | Anterior 4 / 150 / 980; diferencia `▲ +1`, `▲ +30`, `▲ +220` |
| 3 | Registrar `RUNNING`, 30 minutos, fecha de hoy | "Entrenamiento registrado: 300 kcal estimadas"; el dashboard pasa a 6 / 210 / 1500 y la diferencia a +2 / +60 / +520 |
| 4 | Registrar con duración 0 | "La duración debe ser mayor que 0" y no se guarda nada |
| 5 | Eliminar el entrenamiento del escenario 3 | "Entrenamiento eliminado"; el dashboard vuelve a 5 / 180 / 1200 |

La semana sin entrenamientos (ceros) se comprueba en las pruebas y con una base temporal vacía; no es un escenario de la demo.

### 15.2 Recarga automática

Las fechas se calculan al cargar, por lo que los datos envejecen. Cada lunes a las 00:05 (zona configurada) la semana actual queda vacía y la revisión recarga la demo: la semana actual vuelve a tener 5 entrenamientos y la anterior, 4. Si alguien elimina todos los entrenamientos de la semana actual, la demo se restaura en la siguiente revisión, como máximo un día después.

Consecuencias aceptadas: los datos que agreguen los visitantes se pierden en cada recarga, y la semana vacía no puede mostrarse de forma permanente en la demo (se comprueba en las pruebas y con una base temporal vacía).

---

## 16. Pruebas

Tres niveles:

1. **Unitarias de Services**, con Mockito para el repositorio y `Clock.fixed`, sin base de datos ni contexto de Spring.
2. **Integración** contra la base PostgreSQL local `training_dashboard_test`, con el perfil `test`. Sin H2 ni Testcontainers. Sin `@Transactional` en la clase de prueba; la limpieza va en `@BeforeEach` y `@AfterEach`.
3. **Flujo web con `MockMvc`:** rutas, códigos de estado, cuerpo de error y mensajes exactos, y el escenario completo.

Casos prioritarios:

- Calorías de cada tipo y duraciones límite: 0, 1, 1440 y 1441.
- Semana con `Clock.fixed` en lunes, a mitad de semana y en domingo; fechas en los bordes (lunes, domingo, el día anterior y el posterior); semanas que cruzan un mes o un año.
- Semana sin entrenamientos: ceros. Diferencias positivas, negativas y cero.
- Los números del §15 para ambas semanas.
- Entrenamiento con fecha futura dentro de la semana actual: cuenta.
- Orden del listado: fecha descendente, luego id descendente.
- Eliminar un entrenamiento existente, uno inexistente y un id no numérico.
- Cada mensaje del §12, incluidos `30.5`, texto en la duración, tipo desconocido, fecha mal formada y cuerpo vacío, y cada valor de lectura estricta del §10 (por ejemplo `"30"`, tipo `0`, fecha con hora, arreglos y cadenas vacías).
- `calories` e `id` enviados por el cliente se ignoran.
- `GET /api/workout-types` y `GET /` responden 200.
- Un error no previsto devuelve el 500 genérico sin detalles, y el log no contiene mensajes de excepciones ni la cadena de conexión.
- `DemoDataLoader`: totales exactos; no toca los datos si la semana actual tiene entrenamientos; reemplaza los datos viejos y la tabla vacía; conserva los datos anteriores y no detiene el arranque si la carga falla; la tarea programada queda registrada con la inicialización perezosa activada; arranque sobre una base recién creada.

El frontend se verifica recorriendo los escenarios del §15 en el navegador, incluida la vista de 375 px. No hay herramientas de prueba de JavaScript.

---

## 17. Despliegue en Render + Neon

Se despliega solo con el proyecto terminado (Fase 10) y solo con datos de demo, porque la aplicación no tiene autenticación: cualquiera con la URL puede crear y eliminar datos.

### 17.1 Hechos (documentación oficial de Render, noviembre de 2026)

- Render ejecuta de forma nativa Node.js/Bun, Python, Ruby, Go, Rust y Elixir. **Java solo con Docker.** Por eso el repositorio incluye un `Dockerfile` en la raíz, que Render detecta.
- El servicio gratuito se duerme a los 15 minutos sin tráfico y tarda hasta un minuto en despertar.
- Cada workspace recibe 750 horas gratuitas de instancia al mes. Si se agotan, Render suspende los servicios gratuitos hasta el mes siguiente. Los servicios dormidos no consumen horas.
- La base de datos gratuita de Render expira a los 30 días: no se usa. La base va en **Neon** (plan gratuito; el plan vigente se verifica en la Fase 10).
- El sistema de archivos del servicio gratuito es efímero: la aplicación no escribe en disco.

### 17.2 Decisiones

- Cuenta de Render propia para este proyecto, distinta de la de otros proyectos, para no compartir las 750 horas del workspace.
- Un monitor gratuito (UptimeRobot) visita `/` con un intervalo menor a 15 minutos para mantener el servicio despierto.
- Neon en la misma región que Render (Ohio), con conexión directa: sin pooler, host sin `-pooler`.
- `DB_URL=jdbc:postgresql://HOST/neondb?sslmode=require`; usuario y contraseña van en `DB_USERNAME` y `DB_PASSWORD`, nunca dentro de la URL.

### 17.3 Dockerfile y .dockerignore

Las pruebas necesitan una base local, por eso la imagen se construye con `-DskipTests`:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

`.dockerignore`: `target`, `.git`, `.idea`, `*.iml`, `.opencode`, `docs`, `*.md`.

### 17.4 Servicio en Render

Web Service con Language Docker, rama `main`, región Ohio, instancia Free, health check `/`, y los campos de Docker por defecto. Variables de entorno:

| Variable | Valor |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Los de Neon |
| `SPRING_PROFILES_ACTIVE` | `demo` |
| `JAVA_TOOL_OPTIONS` | `-Xmx300m -XX:+UseSerialGC -XX:TieredStopAtLevel=1` |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `5` |
| `SPRING_MAIN_LAZY_INITIALIZATION` | `true` (con el esquema ya creado) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `none` (con el esquema ya creado) |

El primer arranque se hace localmente contra Neon con el perfil `demo`: crea el esquema y carga los datos. Cada `git push` a `main` redespliega. Si el esquema cambia, se vuelve a `update` durante un despliegue.

### 17.5 Checklist de portabilidad

- Todo por variables de entorno; sin estado local en disco; la aplicación no usa sesiones.
- PostgreSQL estándar: cambiar de proveedor solo cambia `DB_URL`. Las bases gestionadas exigen TLS (`?sslmode=require`).
- Perfil `demo` con datos ficticios; sin secretos en el repositorio ni en su historial.
- El README avisa de que la demo puede tardar en despertar y de que no tiene autenticación.

---

## 18. Criterios de aceptación

### Registro y consulta

- Un entrenamiento válido se registra con 201 y las calorías calculadas.
- Cada regla del §10 se rechaza con 400 y su mensaje exacto del §12.
- Las calorías se calculan en el backend, se almacenan y no pueden ser fijadas por el cliente.
- La lista se ordena por fecha descendente y luego por id descendente.
- Eliminar un entrenamiento existente devuelve 204; uno inexistente, 404 con su mensaje.

### Dashboard

- La semana va de lunes a domingo y se calcula en el backend con el `Clock` inyectado.
- Solo cuentan los entrenamientos de la semana actual.
- Cantidad, minutos y calorías son correctos; una semana vacía devuelve ceros.

### Comparativa

- La semana anterior se calcula correctamente.
- Las métricas de ambas semanas son correctas.
- Las diferencias corresponden a `actual - anterior`.

### Frontend

- Se puede registrar, ver y eliminar entrenamientos, y ver el dashboard y la comparativa.
- Los errores son comprensibles y aparecen junto al campo.
- Se ve bien a 375 px.

---

## 19. Decisiones de diseño deliberadas

- **Sin usuarios:** los datos son globales.
- **Sin edición:** solo existe `DELETE`. Un registro equivocado se elimina y se vuelve a registrar. Hay borrado duro porque no hay historial que conservar.
- **Calorías calculadas en el backend:** el cliente no puede fijarlas.
- **Semana calculada en el backend:** el frontend no envía `startDate` ni `endDate`.
- **Estado derivado de fechas:** nada de la semana se persiste.
- **Zona horaria configurable:** el límite de la semana depende de la zona del `Clock`, no de la del servidor.
- **Fechas futuras permitidas:** el servidor tiene una zona fija y un navegador en otra zona puede tener un "hoy" distinto; rechazar fechas futuras haría fallar el formulario por defecto.
- **Diferencias absolutas:** sin porcentajes, para evitar divisiones por cero.
- **Sin métricas adicionales.**
- **Un solo origen:** el frontend estático lo sirve la misma aplicación.
- **Docker solo para Render.**
- **Demo que se mantiene sola:** la semana actual vacía dispara la recarga de los datos de demo (§15). Una falla de la recarga nunca detiene la aplicación.

---

## 20. Regla de alcance

Toda funcionalidad nueva se evalúa contra esta pregunta:

> **¿Es necesaria para demostrar el MVP o para practicar directamente una tecnología o concepto ya previsto?**

Si la respuesta es no, queda fuera del MVP. El objetivo es terminar un proyecto funcional y entendible, no convertirlo en un sistema de producción.

---

## 21. Glosario

| Español | Inglés (código) |
|---|---|
| Entrenamiento | `Workout` (tabla `workouts`, ruta `/api/workouts`) |
| Tipo de entrenamiento | `WorkoutType` (ruta `/api/workout-types`) |
| Correr, Ciclismo, Natación, Pesas, Caminata | `RUNNING`, `CYCLING`, `SWIMMING`, `WEIGHTS`, `WALKING` |
| Duración en minutos | `durationMinutes` (columna `duration_minutes`) |
| Calorías | `calories` |
| Fecha | `date` (columna `workout_date`) |
| Factor calórico | `caloriesPerMinute` |
| Semana actual, semana anterior | `currentWeek`, `previousWeek` |
| Métricas semanales | `WeeklyMetricsResponse` |
| Diferencia | `difference`, `WeeklyDifferenceResponse` |
| Comparativa | comparison (ruta `/api/dashboard/comparison`) |
| Dashboard semanal | weekly (ruta `/api/dashboard/weekly`) |
| Entrenamiento no encontrado | `WorkoutNotFoundException` |
| Respuesta de error | `ErrorResponse` |
| Datos de demo | `DemoDataLoader`, perfil `demo` |

---

## 22. Versión y registro de cambios

**Versión actual: 1.2**

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0 | 2026-11-04 | Versión inicial a partir del SPEC original, con las decisiones cerradas: rutas y enums en inglés, `DELETE` sin `PUT`, zona horaria configurable, formato de errores, datos de demo y despliegue en Render + Neon |
| 1.1 | 2026-11-04 | Los datos de demo se recargan solos cuando la semana actual queda vacía (revisión al arrancar y diaria) y la revisión nunca detiene la aplicación |
| 1.2 | 2026-11-07 | Lectura estricta de `type`, `durationMinutes` y `date` (§10), cadena vacía como ausente y log del 500 sin mensajes (§12.1) |
