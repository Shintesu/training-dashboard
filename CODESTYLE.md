# Code Style

Rules for writing code in this repository. Short and opinionated. No formatter or linter is configured, so every rule is caught in review.

If a rule below conflicts with what an IDE, a formatter preset or a tutorial suggests, this file wins. If it conflicts with `SPECS.md`, the SPECS wins.

---

## 0. The four rules that matter most

1. **English for all code.** Identifiers (domain names included), comments, commits, logs, tests, branch names, routes and file names. Spanish only for text shown to users (see §1).
2. **No decorative separators in source code.** Never `// ------------`, never `/* ===== */`, never box drawings.
3. **No over-explained comments.** If the code says it, the comment must not repeat it.
4. **Credentials stay out of logs and messages.** `DB_PASSWORD` and any connection string that carries credentials never reach a log line, an exception message or an API response.

---

## 1. Language

All code is written in English: identifiers (domain names included), table names, routes, file names, comments, commit messages, log lines, test names and branch names. `SPECS.md` §21 lists the English name of every domain term.

Spanish is used only for text the user sees:
- Text in the HTML pages and text inserted by JavaScript.
- Bean Validation messages.
- The `message` and every `fieldErrors` value of `ErrorResponse`.
- The message of business exceptions.
- The Spanish label of each enum value shown in the UI (for example `WorkoutType.RUNNING` → "Correr"), defined next to the enum.

Enum codes, JSON field names and routes stay in English. Exceptions that signal programming errors (illegal state, null parameter) use English messages and never reach the user.

```java
// bad
// Recorre los entrenamientos de la semana y suma las calorias
for (Entrenamiento entrenamiento : entrenamientos) { }
```

```java
// good
// Totals are long: summing many int values must not overflow.
for (Workout workout : workouts) { }
```

Identifiers use ASCII only. No accents, no emoji, no non-Latin scripts in names, package paths or file names. String literals shown to users may contain any Unicode they need (`ñ`, accents, `▲`, `▼`).

---

## 2. Comments

### 2.1 Do not explain what the code already says

```java
// bad
// increment the counter by one
counter++;

// bad
/**
 * Gets the duration.
 * @return the duration
 */
public Integer getDurationMinutes() { return durationMinutes; }
```

Delete both. The second one is worse than nothing: it costs a review, it costs a merge conflict, and it says less than the signature.

### 2.2 Comment the *why*, the non-obvious, and the trap

```java
// good
// Compare against the injected clock, never LocalDate.now(): tests rely on a fixed date.
LocalDate today = LocalDate.now(clock);
```

```java
// good
// endDate is inclusive: a workout on the last day of the week still counts (SPECS §9.2).
return workoutRepository.findByDateBetween(startDate, endDate);
```

Good comment triggers: a workaround for a framework or database quirk, an ordering constraint, a time-handling decision, a spec reference (`SPECS §9.2`), a deliberate deviation from the obvious approach.

### 2.3 Javadoc

Javadoc is optional. Use it only where a method has a contract the signature cannot express: preconditions, thrown exceptions, side effects. As filler it is forbidden.

Write it as a contract, not a paraphrase:

```java
/**
 * Deletes the workout with the given id.
 *
 * @throws WorkoutNotFoundException if no workout has that id
 */
public void deleteWorkout(Long id) {
```

No `@author` tags. Git knows. No `@version` tags. The build knows.

### 2.4 Banned comment forms

- Separator banners of any kind, including Unicode box characters.
- Commented-out code. Delete it; git has it.
- Changelog comments (`// modified by X on 2026-01-01`).
- `// TODO` without an issue.

```java
// bad
// TODO fix this later

// good
// TODO(#12): extract the 1440-minute limit into configuration.
```

`FIXME` and `HACK` are allowed under the same rule. A `TODO` that outlives the roadmap phase it was written in is a bug report, not a comment.

These rules apply to Java, JavaScript, CSS and HTML alike.

---

## 3. Formatting (Java)

- 4 spaces, no tabs.
- 120 column soft limit. Break at a semantic boundary, not at column 121.
- Braces on the same line, always present, even for a one-statement `if`.
- One statement per line. One declaration per line.
- Blank lines separate logical blocks. A method needing five blank-line groups probably wants to be five methods.
- Imports: no wildcards, no unused, ordered `java` → `javax` → third-party → `com.example`.
- Files end with a newline. No trailing whitespace.
- `final` on parameters is not required. `final` on fields is (see §7).

No formatter is configured. Set the IDE to 4 spaces and 120 columns. Do not argue formatting in review; change this file or move on. Formatting for the frontend is in §11.

### 3.1 Separators, explicitly

```java
// bad
// =============================================================================
// Week rules
// =============================================================================

// bad
/* --------------------------- helpers --------------------------- */

// bad
//////////////////////////////////////////////////////////
```

If a file needs visual sections to be navigable, the file is too big. Split it. Reviewers reject any comment line made of three or more repeated punctuation characters.

Markdown documentation may use `---` as a horizontal rule. That is documentation, not source.

---

## 4. Naming

| Kind              | Convention                | Example                                |
| ----------------- | ------------------------- | -------------------------------------- |
| Package           | lowercase, no underscores | `com.example.trainingdashboard.service` |
| Class / interface | `PascalCase`              | `DashboardService`                     |
| Method / field    | `camelCase`               | `weeklyMetrics`, `durationMinutes`     |
| Constant          | `UPPER_SNAKE_CASE`        | `MAX_DURATION_MINUTES`                 |
| Enum value        | `UPPER_SNAKE_CASE`        | `RUNNING`                              |
| Type parameter    | single letter or short    | `T`, `ID`                              |
| Test method       | `camelCase`, a sentence   | `rejectsDurationAboveOneDay`           |

Rules:

- No Hungarian notation, no `m_` or `_` prefixes, no `I` prefix on interfaces.
- No abbreviations except universally known ones (`id`, `dto`, `sql`, `uuid`).
- No `Impl` suffix. A class with a single implementation does not need an interface: `WorkoutService` is a class.
- Layer suffixes are part of the name: `WorkoutController`, `WorkoutService`, `WorkoutRepository`, `WorkoutNotFoundException`.
- Request and response records end in `Request` and `Response`: `WorkoutRequest`, `WeeklyMetricsResponse`. No `Dto` suffix.
- Boolean methods read as assertions: `isEmpty()`, `isInWeek()`.
- Do not name a class `Utils`, `Helper`, `Manager` or `Common`. Name it for what it does.

---

## 5. Packages and layer boundaries

```
com.example.trainingdashboard.controller   HTTP: receive, validate, delegate, return a response
com.example.trainingdashboard.service      business rules; returns DTOs
com.example.trainingdashboard.repository   Spring Data JPA access
com.example.trainingdashboard.entity       JPA entities
com.example.trainingdashboard.dto          request and response records
com.example.trainingdashboard.enums        WorkoutType
com.example.trainingdashboard.exception    business exceptions and GlobalExceptionHandler
com.example.trainingdashboard.config       configuration, including the Clock bean and DemoDataLoader
```

Hard boundaries:

- Dependencies point one way: `controller` → `service` → `repository`.
- Controllers do not call repositories and contain no business rules.
- Services own the business rules. They return DTOs, so entities never reach a controller or the JSON. They do not depend on web types (`HttpServletRequest`, `ResponseEntity`).
- Repositories contain no business rules.
- The week range and the metrics live in `DashboardService` only. Calories are calculated in the service from `WorkoutType`, nowhere else.
- The frontend contains no business rules: it never computes calories or weeks, and it never sends `startDate` or `endDate`.

---

## 6. Dependencies

The stack is defined in `SPECS.md` §5. Do not add a dependency outside it.

Adding one requires: an issue explaining why the existing stack cannot cover it, its license (MIT / Apache-2.0 / BSD only), and explicit approval. Expect to be told no.

- Lombok is not used, and neither is DevTools.
- No JavaScript framework, library or CDN.
- AssertJ and Mockito come bundled with Spring Boot Test and need no separate approval. SLF4J comes with Spring Boot.

---

## 7. Design

- **Request and response objects are `record`s.** Request records carry the Bean Validation annotations.
- **JPA entities** keep a no-arg constructor and mutable fields, as JPA requires. No `equals`, `hashCode` or `toString`.
- **`WorkoutType` holds its Spanish label and its calorie factor.** No `switch` on the type to calculate calories.
- **Fields in controllers, services and components are `private final`** and injected through the constructor. No field injection.
- **Time comes from an injected `Clock`.** Never call `LocalDate.now()` or `LocalDateTime.now()` without a clock in business logic (SPECS §9.1). Tests use `Clock.fixed`.
- **Totals are `long`.**
- **No static mutable state.** Shared state lives in Spring-managed beans.
- **Constructors do not do work.** No I/O, no database access in a constructor.
- **Return `Optional<T>` for a genuinely absent single result.** Never return `Optional` from a field, never as a parameter, never for collections. Return an empty collection.
- **Never return `null` from a public method.** Empty collection, `Optional`, or throw.
- **Never accept `null` silently.** Validate at the boundary with Bean Validation on request records. Inside services, throw `NullPointerException` with the parameter name (`Objects.requireNonNull(value, "value")`), or accept it explicitly and document it.
- **No checked exceptions.** Business exceptions are unchecked.

---

## 8. Errors

- Business exceptions are unchecked: `WorkoutNotFoundException`. It carries the message shown to the user, in Spanish, without technical details.
- `GlobalExceptionHandler` is the only place that builds an `ErrorResponse`. It maps validation failures, unreadable bodies, type mismatches on the path, `WorkoutNotFoundException` and anything else (a generic 500) to the exact messages in SPECS §12.
- Every message names the concrete thing: `"No se encontró el entrenamiento con id 42"`, never `"No encontrado"`.
- Never show a stack trace, SQL or a vendor message. The 500 response is generic; the cause is logged at `ERROR`.
- Messages never contain credentials.
- Never `catch (Exception e) { }`. Never catch to log-and-rethrow the same thing twice.
- Fail early: validate in the request record and in the service before touching the database.
- Do not use exceptions for control flow. Ask with `existsBy...`; do not catch `WorkoutNotFoundException` to decide.

---

## 9. Logging

### 9.1 The rules

- Log through SLF4J: `private static final Logger log = LoggerFactory.getLogger(WorkoutService.class);`. Never `System.out.println`, never `System.err.println`, never `e.printStackTrace()`.
- Messages are parameterized, never concatenated.

```java
// bad
log.debug("registered workout " + workoutId + " of type " + type);

// good
log.debug("registered workout {} of type {}", workoutId, type);
```

- Guard expensive argument computation with `log.isDebugEnabled()`.
- Never log database credentials or connection strings that carry them.
- Expected business errors (invalid request, not found) are not logged above `DEBUG`.

### 9.2 Levels

| Level   | Use for                                                           |
| ------- | ----------------------------------------------------------------- |
| `ERROR` | The operation failed unexpectedly (a 500)                         |
| `WARN`  | Handled but suspicious                                            |
| `INFO`  | Once-per-lifecycle facts: application started, demo data loaded   |
| `DEBUG` | Per-operation detail: workout registered, week range determined   |
| `TRACE` | Per-item detail                                                   |

`INFO` must stay quiet in a steady-state application. If a request handler can reach it, it is `DEBUG`.

---

## 10. Tests

- JUnit, AssertJ and Mockito. Mockito is for unit tests of Services, replacing repositories. If a unit needs heavy mocking, the design is wrong.
- Unit tests of Services run without a database or a Spring context, with `Clock.fixed` wherever dates matter.
- Integration tests run against the local `training_dashboard_test` PostgreSQL database under the `test` profile (`create-drop`). No H2, no Testcontainers (SPECS §16). No `@Transactional` on the test class; clean up in `@BeforeEach` and `@AfterEach`.
- Web flow tests use `MockMvc` and assert status codes, response bodies and the exact messages of SPECS §12.
- Test names are sentences: `rejectsDurationAboveOneDay`, `countsWorkoutDatedOnSunday`.
- One behaviour per test. No assertion-free tests. No `Thread.sleep`.
- Priority tests follow SPECS §16.
- Every bug fix begins with a failing test.
- Spring Boot 4.1 moved some test annotations between packages: check the import against the jars instead of copying it from older projects.
- After changing resources, run `./mvnw clean test`: a stale `target/classes` can make a test pass when it should not.

---

## 11. Frontend (HTML, CSS, JavaScript)

### 11.1 JavaScript

- ES modules (`<script type="module">`): one module per page plus `api.js` and `format.js` (SPECS §13). No libraries, no CDN, no build step.
- 4 spaces, 120 columns, semicolons, double quotes.
- `const` by default, `let` when reassigned, never `var`. Strict equality only.
- `async`/`await` with `fetch`. Only `api.js` calls `fetch` and reads the `ErrorResponse`.
- Functions are `camelCase`; constants are `UPPER_SNAKE_CASE`.
- No inline event handlers (`onclick=...`) and no inline styles.
- Text coming from the API is inserted with `textContent`, never with `innerHTML`.
- No business rules: no calorie or week calculation.

### 11.2 HTML and CSS

- Semantic elements. `lang="es"` on every page. Every input has an associated `<label>`.
- Success messages use `role="status"`; errors use `role="alert"`.
- A single `styles.css`. Class names are English `kebab-case`. Colors are CSS custom properties on `:root`. Mobile first; the pages must work at 375 px. No `!important`.
- Color never carries meaning alone: every indicator also has a symbol and text (SPECS §13.4).

---

## 12. Git

- Conventional commits: `feat(workout): reject non-integer durations`, `fix(dashboard): count workouts dated on Sunday`, `docs(readme): …`, `refactor`, `test`, `build`, `chore`. Useful scopes: `workout`, `dashboard`, `frontend`, `demo`, `deploy`, `readme`.
- Imperative mood, lowercase subject, no trailing period, under 72 characters.
- The body explains *why*. The diff already explains *what*.
- One logical change per commit. Code and documentation go in separate commits. Formatting-only changes go in their own commit.
- Branches: `feat/weekly-comparison`, `fix/week-boundary`.
- Never commit build output (`target/`), IDE files, or credentials.

---

## 13. PR checklist

Copy this into the PR description:

```
- [ ] English everywhere except text shown to users
- [ ] No separator comments, no commented-out code, no redundant javadoc
- [ ] No dependency added outside the SPECS stack; no Lombok
- [ ] Business rules only in Services; no business logic in the frontend
- [ ] Time comes from the injected Clock
- [ ] Errors use ErrorResponse with the exact messages of SPECS §12
- [ ] No credentials in code, logs, messages or API responses
- [ ] Tests added and ./mvnw test is green
- [ ] SPECS.md and README.md updated if behavior changed
```

---

## 14. Enforcement

No formatter or linter is configured in this project. Everything above is caught in review. Reviewers are expected to cite the rule number.

```bash
./mvnw test
```
