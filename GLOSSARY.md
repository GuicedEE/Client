# GLOSSARY — Topic‑First (GuicedEE Inject Client)

Glossary Precedence Policy
- Topic glossaries take precedence over the root glossary for their scope.
- The host glossary (this file) acts as an index and aggregator with minimal duplication.
- Use links to topic files under ./rules wherever possible.

Selected Topics and Canonical Terms
- Java 25 LTS — rules/generative/language/java/java-25.rules.md
- Maven Build — rules/generative/language/java/build-tooling.md
- CRTP (Curiously Recurring Template Pattern) Fluent API — rules/generative/backend/fluent-api/crtp.md
- Vert.x (core primitives only) — rules/generative/backend/vertx/README.md
- GuicedEE Core — rules/generative/backend/guicedee/README.md
- JSpecify — rules/generative/backend/jspecify/README.md
- Logging — rules/generative/backend/logging/README.md

Lifecycle Terms
- Lifecycle Service — A component participating in application lifecycle (e.g., pre-start, post-start, shutdown, health). Located under package: com.guicedee.client.services (and subpackages). See rules/generative/backend/guicedee/README.md.
- Scopes (applicable to handlers/services)
  - Application/Singleton — one instance per application; stateless; thread-safe.
  - Request — ephemeral per inbound request/message; carries per-call context.
  - Session — stateful across interactions for a principal (use sparingly in reactive apps).
 - IGuicePreDestroy — A pre-shutdown hook used for closing or terminating resources before shutdown; invoked prior to final destroy. Source package: com.guicedee.client.services.lifecycle.

WebSocket Terms (Client Library Scope)
- WebSocket Receiver — An implementation of IWebSocketMessageReceiver that declares supported messageNames and handles inbound messages asynchronously via Mutiny Uni. Source package: com.guicedee.client.services.websocket.
- WebSocket Registry — The static registry within IGuicedWebSocket storing messageListeners keyed by action name; provides helper methods to register/load receivers.
- WebSocket Group — A logical broadcast group to which a client can be added or removed via IGuicedWebSocket.addToGroup/removeFromGroup.
- EveryoneGroup — Default broadcast group constant defined on IGuicedWebSocket that represents all connected clients.
- WebSocket Auth Data Provider — IWebSocketAuthDataProvider SPI that supplies client bootstrap/auth JavaScript and a unique provider name.
- WebSocket Pre-Configuration — IWebSocketPreConfiguration SPI for performing pre-use configuration before sockets are utilized by the host app.

Enforced Prompt Language Alignment (copied locally as required)
- Fluent API Strategy
  - CRTP: Use “CRTP” to refer to fluent chaining returning (J)this; avoid saying “Builder” in this project unless explicitly describing an external API.
  - Builder: Not selected for this project; do not introduce @Builder‑based chaining.

Host Terminology
- Client Startup: The initialization flow triggered via GuicedEEClientStartup and GuicedEEClientPostStartup.
- Client Module: GuicedEEClientModule, a Guice module wiring client bindings.
- Global Properties: Configuration keys accessed via com.guicedee.client.utils.GlobalProperties.
- Lifecycle Package Anchor: com.guicedee.client.services

Cross‑Links
- PACT: ./PACT.md
- RULES: ./RULES.md
- GUIDES: ./GUIDES.md
- IMPLEMENTATION: ./IMPLEMENTATION.md