# Project RULES — GuicedEE Inject Client

Scope and Intent
- This project adopts the Rules Repository model with a documentation‑first, forward‑only workflow.
- Fluent API Strategy: CRTP (no Lombok @Builder chains). Implement manual fluent setters that return (J)this where applicable.

Selected Stacks
- Language: Java 25 LTS — see rules/generative/language/java/java-25.rules.md
- Build: Maven — see rules/generative/language/java/build-tooling.md
- Vert.x Core Types Only (Futures/Promise) — see rules/generative/backend/vertx/README.md
- Framework: GuicedEE Core — see rules/generative/backend/guicedee/README.md
- Optional Security/Observability references:
  - Security (reactive): rules/generative/platform/security-auth/README.md
  - Observability: rules/generative/platform/observability/README.md

Topic Index Links (enterprise rules in ./rules)
- Architecture: rules/generative/architecture/README.md
- Java Language Rules (LTS): rules/generative/language/java/java-25.rules.md
- Java Build Tooling: rules/generative/language/java/build-tooling.md
- CRTP Fluent API Strategy: rules/generative/backend/fluent-api/crtp.md
- JSpecify: rules/generative/backend/jspecify/README.md
- Lombok (general usage guidance): rules/generative/backend/lombok/README.md
- Logging: rules/generative/backend/logging/README.md
- Vert.x (Core primitives only): rules/generative/backend/vertx/README.md
- GuicedEE: rules/generative/backend/guicedee/README.md
- CI/CD: rules/generative/platform/ci-cd/README.md

Lifecycle Services (Client Library Scope)
- This library provides lifecycle services under the package and subpackages of: com.guicedee.client.services
- It is a client library and does not directly expose or depend on Vert.x Web/REST or WebSocket layers.
- Use of Vert.x is limited to core primitives (e.g., Future/Promise) for async composition.
- Document all lifecycle services (pre-start, post-start, shutdown, health, etc.) that the client exposes or participates in. For each service include: purpose, trigger, order, and idempotency expectations.

Glossary Alignment
- The project GLOSSARY.md is topic‑first. Topic glossaries (when present) take precedence for their scopes. Use terms consistently across code, docs, and prompts.

Documentation Modularity Policy
- Do not place project‑specific docs under ./rules. Link to topic rules within ./rules instead.
- Prefer linking to modular topic entries rather than duplicating content.

Forward‑Only Change Policy
- Replace outdated docs with modular references; update all inbound links as part of the same change set.

Cross‑Links
- PACT: ./PACT.md
- GUIDES: ./GUIDES.md
- IMPLEMENTATION: ./IMPLEMENTATION.md
- GLOSSARY: ./GLOSSARY.md
- Architecture Index: ./docs/architecture/README.md
