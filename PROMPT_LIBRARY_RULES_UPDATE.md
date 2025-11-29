# 🧰 Starter Prompt — Library Rules Update (Framework/Component Libraries)

This is a project-scoped copy of the Library Rules Update prompt, preconfigured for the GuicedEE Inject Client. Use this file from the project root to drive rules maintenance under the topic path: `rules/generative/backend/guicedee/client`.

Supported: Junie, AI Assistant, GitHub Copilot Chat, Cursor, ChatGPT, Claude, Roo, Codex.

---

## 0) Provide Inputs
Fill before running.

- Library name: GuicedEE Inject Client
- Current/new version: 2.0.0-SNAPSHOT
- Repository URL / path: C:/Java/DevSuite/GuicedEE/client
- Short description: Guiced EE Client Library
- Type:
  - [ ] UI component library
  - [ ] Data/ORM
  - [x] Service/Framework
  - [ ] Other: <OTHER>

- Stage approvals preference for this run (controls STOP gates)
  - Choose exactly one:
    - [ ] Require explicit approval at each stage (default)
    - [X] Approvals are optional; proceed with documented defaults if no reply
    - [ ] Blanket approval granted for this run (no STOPs)

- AI engine used:
  - [x] Junie
  - [ ] GitHub Copilot
  - [ ] Cursor
  - [ ] ChatGPT
  - [ ] Claude
  - [ ] Roo
  - [X] Codex
  - [ ] AI Assistant
  - Note: Select every engine participating in the release and ensure rules/templates are configured for each.
    - AI Assistant consumes rules from `.aiassistant/rules/`; replicate enforced policies there.

- Architecture:
  - [x] Specification-Driven Design (SDD) (mandatory)
  - [x] Documentation-as-Code (mandatory)
  - [ ] Monolith
  - [ ] Microservices
  - [ ] Micro Frontends
  - [ ] DDD
  - [ ] TDD (docs-first, test-first)
  - [ ] BDD (docs-first, executable specs)
- Language selection (configure here)
  - Languages
    - Java (choose exactly one LTS)
      - [ ] Java 17 LTS
      - [ ] Java 21 LTS
      - [X] Java 25 LTS
    - Web
      - [ ] TypeScript
        - [ ] Angular (TypeScript)
        - [ ] React (TypeScript)
          - [ ] Next.js (TypeScript)
        - [ ] Vue (TypeScript)
          - [ ] Nuxt (TypeScript)
      - [ ] JavaScript
    - Kotlin
      - [ ] Kotlin
      - [ ] Ktor (requires Kotlin)
    - Other: <OTHER_LANGUAGES>
  - Build engines
    - Java/Kotlin builds
      - [x] Maven
      - [ ] Gradle (Groovy DSL)
      - [ ] Gradle (Kotlin DSL)
      - [ ] Apache Ivy
    - Web builds
      - [ ] npm / package.json scripts
      - [ ] pnpm
      - [ ] yarn
      - [ ] Babel (transpile configuration lives in package.json/babel.config.*)
    - Other build tooling: <OTHER_BUILDS>
  - Dependency declarations
    - JVM: document artifact coordinates only (groupId:artifactId:version); detailed build configuration belongs in build-tooling topics.
    - JavaScript/Web: document package names + versions (npm/pnpm/yarn/Babel) and leave script wiring to language/build guides.

- Component/topic areas (list): generative/backend/guicedee/client
- Fluent API Strategy (choose exactly one):
  - [x] CRTP
  - [ ] Builder pattern (Lombok @Builder/manual)
- Backend Reactive:
  - Core stacks:
    - [X] Vert.x 5 — ./generative/backend/vertx/README.md
    - [ ] Hibernate Reactive 7 — ./generative/backend/hibernate/README.md
  - Quarkus:
    - [ ] Core project setup
    - [ ] RESTEasy Reactive APIs
    - [ ] Persistence (Hibernate/Panache)
    - [ ] Reactive messaging
    - [ ] Security/OIDC
    - [ ] Dev Services & local tooling
    - [ ] Native build & packaging
    - [ ] Testing strategy
    - Note: Quarkus currently embeds Vert.x 4; pick Vert.x 5 only for direct Vert.x API usage.
  - GuicedEE:
    - [X] Core
    - [ ] Web
    - [ ] Rest
    - [ ] Persistence
    - [ ] RabbitMQ
    - [ ] Cerial
    - [ ] OpenAPI
    - [ ] Sockets
    - Note: If Core is selected, also select Vert.x 5; if Persistence is selected, also select Hibernate Reactive 7.
- Backend:
  - [ ] Spring
  - [ ] Quarkus
  - [x] GuicedEE
  - [ ] Vert.x

Notes
- This client library explicitly uses JSpecify (nullness annotations) and Mutiny (reactive types). Ensure rules and examples reference:
  - JSpecify — ./rules/generative/backend/jspecify/README.md
  - Mutiny — reactive usage in conjunction with Vert.x and GuicedEE Core

---

## 1) Confirm Topic Path and Scope
Project topic for this library’s rules must reside under:
- Path: `rules/generative/backend/guicedee/client`
- Scope: GuicedEE Inject Client specific rules, indexes, glossary, and integration guides.

If the path does not yet exist, plan its structure but do not create it here; use the topic scaffolding prompts as required.

---

## 2) Sync With Host Rules Repository Index
- Ensure the GuicedEE topic index exists at: `rules/generative/backend/guicedee/README.md`.
- Add or update a sub-index for `client` under the GuicedEE topic.
- Ensure cross-links from related topics (CRTP, Vert.x, Hibernate) reference this client topic where relevant.

---

## 3) Library Rules Update — Tasks
Follow the steps below to update or create the rules for this library. Stop after each stage if approvals are required.

1. Discover/Harvest existing docs
   - Locate README, guides, and existing rules in this repo.
   - Identify API surfaces, SPI contracts, extension points, and module names.
2. Define Glossary and Index
   - Create/Update `README.md` under `rules/generative/backend/guicedee/client` as the topic index.
   - Create/Update `GLOSSARY.md` with GuicedEE-specific client terms.
3. Write/Update Rules
   - Author rules that govern usage, dependencies, module names, and extension patterns for the client.
   - Enforce CRTP for fluent APIs in the GuicedEE ecosystem.
4. Add References and Examples
   - Provide minimal, self-contained examples.
   - Reference related topics (CRTP, Vert.x, Hibernate, Services).
5. Validate and Link
   - Validate links and ensure JPMS module names are correct.
   - Link from parent GuicedEE topic to this client subtopic.

---

## 4) Outputs Checklist
Produce or update the following within `rules/generative/backend/guicedee/client/`:
- README.md — Topic index
- GLOSSARY.md — GuicedEE client terminology
- rules/*.md — Specific rules (e.g., DI lifecycle, SPI contracts)
- examples/*.md — Minimal examples
- services/*.md or *.xml — If applicable, references to GuicedEE Services artifacts

---

## 5) Review Gates
- STOP: Confirm index and glossary content accuracy.
- STOP: Confirm rule alignment with GuicedEE ecosystem policies (CRTP, JPMS modules, Services coordinates).
- STOP: Confirm cross-topic links are valid and bi-directional where applicable.

---

## 6) Handover Notes
- Commit changes with a message referencing the GuicedEE Inject Client rules update.
- Notify stakeholders that the client topic is available under `generative/backend/guicedee/client`.
