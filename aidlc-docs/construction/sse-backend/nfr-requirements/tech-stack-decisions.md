# Tech Stack Decisions — Unit 1: sse-backend

## Bestehender Stack (unverändert)

| Komponente | Version | Entscheidung |
|---|---|---|
| Spring Boot | 3.3.5 | Beibehalten |
| Java | 21 | Beibehalten |
| Spring Security | via Boot | Beibehalten; neuer Filter additiv |
| Spring Data JPA | via Boot | Beibehalten |
| PostgreSQL | 16 | Beibehalten |
| jjwt | 0.12.6 | Beibehalten; für JwtQueryParamFilter wiederverwendet |

## Neue Technologie-Entscheidungen

### Spring SseEmitter (spring-web, bereits enthalten)
- **Entscheidung**: `org.springframework.web.servlet.mvc.method.annotation.SseEmitter` aus `spring-boot-starter-web`
- **Begründung**: Kein neues Maven-Dependency nötig; Spring SseEmitter ist production-ready, unterstützt named Events, hat eingebaute Timeout/Callback-Mechanismen
- **Alternative verworfen**: WebSocket (zu komplex für unidirektionalen Push), reactive Flux/SSE (erfordert WebFlux-Migration)

### ConcurrentHashMap + CopyOnWriteArrayList
- **Entscheidung**: `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>` für die Emitter-Registry
- **Begründung**: Thread-safe ohne explizite Synchronisierung; mehrere Threads können gleichzeitig broadcasten und Emitter hinzufügen/entfernen; kein deadlock-Risiko
- **Alternative verworfen**: `HashMap` + `synchronized` (fehleranfälliger), Guava `Multimap` (externe Abhängigkeit)

### OncePerRequestFilter (spring-security, bereits enthalten)
- **Entscheidung**: `OncePerRequestFilter` als Basis für `JwtQueryParamFilter`
- **Begründung**: Spring-Standardmuster für Security-Filter; garantiert einmalige Ausführung pro Request; nahtlose Integration in bestehende FilterChain

### Kein neues Maven-Dependency
- Alle benötigten Klassen sind in `spring-boot-starter-web` und `spring-boot-starter-security` enthalten
- `pom.xml` bleibt unverändert
