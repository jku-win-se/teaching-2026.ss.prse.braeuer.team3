# NFR Requirements — Unit 2: sse-frontend

**Datum**: 2026-04-12

| ID | Anforderung | Quelle |
|---|---|---|
| NFR-FE-01 | Latenz ≤ 2s: SSE-Event muss innerhalb 2s im UI sichtbar sein | NFR-01 |
| NFR-FE-02 | Auto-Reconnect mit Warning-Banner bei Verbindungsabbruch | US-008 AC-1, Anforderungsfrage Q5 |
| NFR-FE-03 | `RealtimeService` hat keine Browser-Abhängigkeiten in Tests (testbar ohne DOM) | NFR-03 |
| NFR-FE-04 | Kein `console.log` in Produktionscode | NFR-04 analog |
| NFR-FE-05 | Fehler in einem SSE-Event dürfen andere Events nicht blockieren | NFR-05 |
| NFR-FE-06 | TypeScript strict mode: keine impliziten `any`-Typen | Codebase-Standard |
