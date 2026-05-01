# FR-13 / FR-20 Clarification Questions

Ich habe einen Widerspruch in deinen Antworten entdeckt, der vor der Implementierung geklärt werden muss:

---

## Widerspruch: Frage 2 vs. Frage 8

**Frage 2 — Antwort B**: Ein User kann gleichzeitig Eigentümer seines eigenen Homes UND Mitglied eines anderen Homes sein — alle Räume werden in einer gemeinsamen Ansicht angezeigt (merged).

**Frage 8 — Antwort B**: Ein User kann nur entweder Owner ODER Member sein (nicht beides gleichzeitig).

Diese Antworten widersprechen sich: Antwort B bei Frage 2 setzt voraus, dass ein User gleichzeitig Owner und Member sein kann — Antwort B bei Frage 8 schließt genau das aus.

---

### Clarification Question 1
Wie soll das Rollenmodell grundsätzlich funktionieren?

A) **Exklusiv**: Ein User ist systemweit entweder Owner ODER Member — wer als Member eingeladen wird, kann kein eigenes Home verwalten. Die Frage 2 (merged view) ist damit hinfällig, da Member kein eigenes Home haben.

B) **Kombinierbar**: Ein User ist immer Owner seines eigenen Homes und kann zusätzlich in einem anderen Home als Member eingeladen werden. Der merged view aus Frage 2 (Antwort B) zeigt dann eigene Räume + Räume des fremden Homes gemeinsam.

C) **Kombinierbar mit Kontext-Wechsel**: Ein User ist immer Owner seines eigenen Homes und kann in einem anderen Home Member sein — aber er wählt aktiv, welches Home er gerade sieht (Dropdown/Tab), kein merged view.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

*Bitte beantworte diese eine Klärungsfrage, dann kann ich direkt mit der Requirements-Analyse und dem Workflow-Plan fortfahren.*
