# IMPLEMENTATION — GuicedEE Inject Client

Code Layout
- Maven module: guice-inject-client (pom.xml)
- Java sources: ./src/main/java
  - com.guicedee.client.utils.GlobalProperties — configuration utilities
  - com.guicedee.client.implementations.GuicedEEClientStartup — pre-start lifecycle
  - com.guicedee.client.implementations.GuicedEEClientPostStartup — post-start lifecycle
  - com.guicedee.client.implementations.GuicedEEClientModule — Guice module wiring
- Tests: ./src/test/java

Runtime Flow (overview)
- Startup sequence triggers GuicedEEClientStartup.onStartup(), configures scanning options, and returns Future<Boolean> succeeded.
- Post startup sequence (if present) finalizes client initialization.
- Injection is SPI-driven: modules are provided as implementations of com.guicedee.client.services.lifecycle.IGuiceModule and discovered via ServiceLoader by GuiceContext. GuiceContext orders modules by sortOrder() and applies their bindings; no manual createInjector(new Module()) is used.
- Pre/Post startup services are also discovered and may be grouped/executed by sortOrder for deterministic lifecycle sequencing.
- Client components can be obtained via IGuiceContext.getInstance(MyType) or by relying on @Inject.
- Note: This client library does not directly expose Vert.x Web/REST or WebSocket layers; it only uses Vert.x core primitives (e.g., Future/Promise).

Lifecycle Services Inventory (to be maintained)
- Package anchor: com.guicedee.client.services (and subpackages)
- Lifecycle package: com.guicedee.client.services.lifecycle
  - IGuicePreStartup — Initializes before Guice is injected. Returns List<Future<Boolean>> for async startup steps. Default sortOrder() = 100. Not injectable (@INotInjectable).
    - Source: src/main/java/com/guicedee/client/services/lifecycle/IGuicePreStartup.java
  - IGuicePostStartup — Runs after initial startup to finalize client initialization. (See: src/main/java/com/guicedee/client/services/lifecycle/IGuicePostStartup.java)
  - IGuicePreDestroy — Runs during shutdown to release resources.
    - Source: src/main/java/com/guicedee/client/services/lifecycle/IGuicePreDestroy.java
  - IGuicePreDestroy — Used for closing or terminating resources before shutdown; invoked prior to final destroy. Must be idempotent and thread-safe.
    - Source: src/main/java/com/guicedee/client/services/lifecycle/IGuicePreDestroy.java
  - IGuiceModule — Service-located Guice Module (extends com.google.inject.Module and IDefaultService). Default enabled() = true.
    - Source: src/main/java/com/guicedee/client/services/lifecycle/IGuiceModule.java
  - IGuiceConfigurator — Functional interface to configure IGuiceConfig for GuiceContext/Injector setup.
    - Source: src/main/java/com/guicedee/client/services/lifecycle/IGuiceConfigurator.java
  - IOnCallScopeEnter — Functional interface invoked when a call/request scope begins; onScopeEnter(Scope scope).
    - Source: src/main/java/com/guicedee/client/services/lifecycle/IOnCallScopeEnter.java
  - IOnCallScopeExit — Functional interface invoked when a call/request scope ends; onScopeExit().
    - Source: src/main/java/com/guicedee/client/services/lifecycle/IOnCallScopeExit.java
- Implementations (current module)
  - Pre-Start: com.guicedee.client.implementations.GuicedEEClientStartup (sortOrder: Integer.MIN_VALUE + 1)
  - Post-Start: com.guicedee.client.implementations.GuicedEEClientPostStartup (if present)
  - Shutdown/Health: <none in this module> — document here when introduced

WebSocket Services (SPI) Inventory
- Package: com.guicedee.client.services.websocket
  - IGuicedWebSocket — Core WebSocket registry/contract used by clients to manage message receivers and broadcasting. Provides group management and a shared map of messageListeners.
    - Key methods: addToGroup(String), removeFromGroup(String), broadcastMessage(...), broadcastMessageSync(...)
    - Static helpers: addWebSocketMessageReceiver(IWebSocketMessageReceiver), isWebSocketReceiverRegistered(String), getMessagesListeners(), loadWebSocketReceivers()
    - Constants: EveryoneGroup
    - Source: src/main/java/com/guicedee/client/services/websocket/IGuicedWebSocket.java
  - IWebSocketMessageReceiver<R,J> — SPI for handling inbound WebSocket messages by action/name. Implementations advertise supported names and handle messages asynchronously via Mutiny Uni.
    - Methods: Set<String> messageNames(), Uni<R> receiveMessage(WebSocketMessageReceiver<?> message)
    - Source: src/main/java/com/guicedee/client/services/websocket/IWebSocketMessageReceiver.java
  - WebSocketMessageReceiver — DTO for inbound message payloads used by receivers. Includes action, broadcastGroup, dataService, data map, and webSocketSessionId. Jackson‑annotated for flexible JSON mapping.
    - Source: src/main/java/com/guicedee/client/services/websocket/WebSocketMessageReceiver.java
  - IWebSocketAuthDataProvider<J> — SPI to provide authentication bootstrap data (e.g., JavaScript snippet) for clients establishing WebSocket connections. Enable/disable via IServiceEnablement.
    - Methods: StringBuilder getJavascriptToPopulate(), String name()
    - Source: src/main/java/com/guicedee/client/services/websocket/IWebSocketAuthDataProvider.java
  - IWebSocketPreConfiguration<J> — SPI to perform any pre‑configuration before WebSocket usage (e.g., environment vars or client options). Follows IDefaultService conventions.
    - Source: src/main/java/com/guicedee/client/services/websocket/IWebSocketPreConfiguration.java
  - GuicedWebSocketOnAddToGroup<J> — Hook invoked when adding the current client to a group. Asynchronous, returns CompletableFuture<Boolean> indicating if already complete.
    - Method: CompletableFuture<Boolean> onAddToGroup(String groupName)
    - Source: src/main/java/com/guicedee/client/services/websocket/GuicedWebSocketOnAddToGroup.java
  - GuicedWebSocketOnRemoveFromGroup<J> — Hook invoked when removing the current client from a group. Asynchronous, returns CompletableFuture<Boolean> indicating if action already taken.
    - Method: CompletableFuture<Boolean> onRemoveFromGroup(String groupName)
    - Source: src/main/java/com/guicedee/client/services/websocket/GuicedWebSocketOnRemoveFromGroup.java
  - GuicedWebSocketOnPublish<J> — Hook for publish/broadcast events to a group.
    - Method: boolean publish(String groupName, String message) throws Exception
    - Source: src/main/java/com/guicedee/client/services/websocket/GuicedWebSocketOnPublish.java

Notes
- These are client‑side service interfaces/SPI contracts. This module does not host a Vert.x Web server; instead, it supplies extension points and registries that host applications can wire to their transport.

Diagrams
- C4 Context: docs/architecture/c4-context.md
- C4 Container: docs/architecture/c4-container.md
- Component (Client): docs/architecture/c4-component-client.md
- Sequences: docs/architecture/sequence-startup.md, docs/architecture/sequence-injection.md
- ERD (initial): docs/architecture/erd-core.md

Back‑Links
- PACT: ./PACT.md
- RULES: ./RULES.md
- GUIDES: ./GUIDES.md
- GLOSSARY: ./GLOSSARY.md

Forward‑Only Note
- Implementation names and labels align to the GLOSSARY. Changes to naming should be coordinated via glossary updates and forward‑only doc changes.