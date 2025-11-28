# PACT — GuicedEE Inject Client

Purpose
- Adopt the Rules Repository model (Pact → Rules → Guides → Implementation) for this existing project.
- Operate in documentation‑first, stage‑gated mode with forward‑only changes.

Scope
- Repository: C:/Java/DevSuite/GuicedEE/client
- Org/Project: GuicedEE / Inject Client
- Description: Guiced EE Client Library providing client‑side injection utilities and startup hooks.

Selections
- Language: Java 25 LTS
- Build: Maven
- Fluent API Strategy: CRTP (no Lombok @Builder for fluent chains)
- Backend Reactive Topics: Vert.x 5, GuicedEE Core
- OpenAPI Provider: Swagger (if applicable in downstream usage)

Cross‑Links
- RULES: ./RULES.md
- GUIDES: ./GUIDES.md
- IMPLEMENTATION: ./IMPLEMENTATION.md
- GLOSSARY (topic‑first): ./GLOSSARY.md
- Architecture index and diagrams: ./docs/architecture/README.md
- Enterprise rules (project‑local copy): ./rules

Evidence (Repository Discovery)
- Build: pom.xml managed by guicedee parent; dependencies include guice, jspecify, vertx-core, classgraph, jackson, log4j, mutiny, junit-jupiter.
- Source modules (examples):
  - com.guicedee.client.utils.GlobalProperties
  - com.guicedee.client.implementations.GuicedEEClientStartup
  - com.guicedee.client.implementations.GuicedEEClientPostStartup
  - com.guicedee.client.implementations.GuicedEEClientModule

Constraints
- Document Modularity Policy: topic‑first glossaries; docs link to rules under ./rules; do not place project docs inside the rules directory.
- Forward‑Only Change Policy: remove/replace legacy docs as needed; update references.

Stage‑Gated Workflow
- Approvals are optional per project prompt configuration; Junie auto‑approves gates. Gates will be rendered for human review and may be skipped by policy.

Traceability
- This PACT links to RULES/GUIDES/IMPLEMENTATION and the architecture diagrams. Those artifacts link back here to close the loop.
