# GuicedEE Inject Client

An extension library for GuicedEE that provides client‑side injection utilities, lifecycle hooks, and module wiring to simplify bootstrapping client components in Java applications.

This repository follows a documentation‑first, stage‑gated workflow using the Rules Repository model (Pact → Rules → Guides → Implementation). See the key docs below for full details.

—

Highlights
- Lightweight Guice module to register client services and defaults
- Pre/Post startup lifecycle hooks for client initialization
- Reactive and event‑driven friendly (Vert.x, Mutiny in ecosystem)
- Designed for Java Platform Module System (JPMS)
- Docs‑as‑code with architecture diagrams and rules

Status
- Maturity: Active development (2.x snapshot lineage via parent POM)
- Compatibility: Java 25 LTS (as selected in project PACT)
- Build: Maven

Table of Contents
- Overview
- Quick Start
- Installation
- Usage
- Configuration
- Architecture
- Contributing
- Security
- Versioning & Support
- License
- Acknowledgements

Overview
GuicedEE Inject Client centralizes client‑side dependency injection setup. It contributes a Guice module and lifecycle hooks that are discovered and registered at runtime, enabling consistent initialization across applications using the GuicedEE stack.

Key Docs
- PACT: ./PACT.md
- RULES: ./RULES.md
- GUIDES: ./GUIDES.md
- IMPLEMENTATION: ./IMPLEMENTATION.md
- GLOSSARY (topic‑first): ./GLOSSARY.md

Architecture & Diagrams (Docs‑as‑Code)
- Index: ./docs/architecture/README.md
- C4 Context: ./docs/architecture/c4-context.md
- C4 Container: ./docs/architecture/c4-container.md
- Components: ./docs/architecture/c4-component-client.md
- Sequences: ./docs/architecture/sequence-startup.md, ./docs/architecture/sequence-injection.md
- ERD: ./docs/architecture/erd-core.md

Rules Repository (project‑local copy for this client)
- ./rules — topic indexes and enterprise rules referenced by project RULES.md

Environment and CI
- Example environment file: ./.env.example
- GitHub Actions (build/test): ./.github/workflows/build.yml

Quick Start
Below is a minimal example showing how the client module and lifecycle hooks are integrated. The concrete bootstrap may vary based on your host application.

// Module: com.guicedee.client (JPMS)
// Services provided (examples):
//  - com.guicedee.client.implementations.GuicedEEClientModule (Guice module)
//  - com.guicedee.client.implementations.GuicedEEClientStartup (Pre‑startup)
//  - com.guicedee.client.implementations.GuicedEEClientPostStartup (Post‑startup)

// Example: Creating an injector with the client module
Injector injector = Guice.createInjector(new GuicedEEClientModule());

// If your platform scans META‑INF/services or JPMS providers, the lifecycle
// hooks (Pre/Post startup) will be discovered and executed by the host.

Installation
Maven (preferred)
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>guice-inject-client</artifactId>
  <version>${guicedee.version}</version>
</dependency>

Gradle (Kotlin DSL)
implementation("com.guicedee:guice-inject-client:${guicedeeVersion}")

Requirements
- Java 25 LTS
- Maven 3.9+ (or Gradle 8+)

Usage
- Add GuicedEEClientModule to your injector or rely on service discovery if your platform auto‑loads modules.
- Implement or register client services using Guice bindings in your own modules.
- Observe or extend lifecycle via:
  - IGuicePreStartup (e.g., GuicedEEClientStartup)
  - IGuicePostStartup (e.g., GuicedEEClientPostStartup)

Configuration
- Global properties and environment integration can be provided via your host application; see ./GUIDES.md and ./RULES.md for conventions and examples.

Architecture
For a deep dive into structure and startup/injection sequences, see Architecture & Diagrams above.

Contributing
- Issues and pull requests are welcome. Please align changes with the Rules → Guides → Implementation workflow and keep docs in sync.
- Follow code style, module boundaries, and topic‑first glossary conventions.
- Before submitting, update or add diagrams/docs where it improves clarity.

Security
- Please do not file public issues for vulnerabilities. If you believe you’ve found a security issue, contact the maintainers privately if possible; otherwise, open an issue with minimal details so we can coordinate responsibly.

Versioning & Support
- Intended to follow Semantic Versioning where feasible. Snapshot versions track active development under the GuicedEE parent.
- Java LTS alignment per PACT selections.

License
- Copyright © The GuicedEE Contributors.
- License: GNU General Public License v3.0 (GPL‑3.0).
- Reference: https://www.gnu.org/licenses/gpl-3.0.en.html

Acknowledgements
- Built on top of GuicedEE, Guice, Vert.x, Mutiny, and other excellent open‑source libraries. See pom.xml for the full dependency set.

Notes
- See PACT.md for the adoption statement and cross‑links, and RULES.md for the selected stacks and conventions.
