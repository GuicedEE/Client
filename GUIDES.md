# GUIDES — GuicedEE Inject Client

Purpose
- How to apply selected rules and use project components.

Topics and How-Tos
- Java/Maven setup: rules/generative/language/java/build-tooling.md
- CRTP Fluent APIs: rules/generative/backend/fluent-api/crtp.md
- Vert.x core primitives (Futures/Promise) only: rules/generative/backend/vertx/README.md
- GuicedEE Core injection lifecycle: rules/generative/backend/guicedee/README.md

Project Guides
- Client Startup Flow
  - Entry points: com.guicedee.client.implementations.GuicedEEClientStartup and GuicedEEClientPostStartup
  - Sequence: docs/architecture/sequence-startup.md
- Client Injection
  - Modules are registered via SPI as implementations of com.guicedee.client.services.lifecycle.IGuiceModule (not via createInjector()).
  - GuiceContext discovers IGuiceModule providers (ServiceLoader), orders them by sortOrder(), and applies their bindings.
  - Pre/Post startup services can be grouped and executed by sortOrder.
  - Retrieve components using either IGuiceContext.getInstance(MyType) or @Inject.
  - Sequence: docs/architecture/sequence-injection.md

- Lifecycle Services (Inventory & How-To)
  - Inventory location: see IMPLEMENTATION.md → Lifecycle Services Inventory
  - Lifecycle package: com.guicedee.client.services.lifecycle
  - Interfaces provided by this client library:
    - IGuicePreStartup — runs before Guice is injected; returns List<Future<Boolean>>; default sortOrder()=100; @INotInjectable.
    - IGuicePostStartup — runs after initial startup to finalize client initialization.
    - IGuicePreDestroy — used for closing or terminating resources before shutdown; invoked prior to final destroy.
    - IGuicePreDestroy — runs during shutdown to release resources (final destroy stage).
    - IGuiceModule — service-located Guice Module (extends com.google.inject.Module) with default enabled()=true.
    - IGuiceConfigurator — configures IGuiceConfig for GuiceContext/Injector.
    - IOnCallScopeEnter — invoked when a call/request scope begins; onScopeEnter(Scope scope).
    - IOnCallScopeExit — invoked when a call/request scope ends; onScopeExit().
  - For each lifecycle service: document purpose, trigger, sort order, idempotency, and side effects.
  - Additions/changes should update: this section, IMPLEMENTATION.md inventory, and sequence diagrams if flows change.
  - Reference rules: rules/generative/backend/guicedee/README.md
  - Note: This client library does not directly expose REST or WebSocket interfaces.

- WebSocket Services (How-To)
  - Package: com.guicedee.client.services.websocket
  - Message receivers (IWebSocketMessageReceiver)
    - Implement Set<String> messageNames() to declare the unique action names your receiver handles.
    - Implement Uni<R> receiveMessage(WebSocketMessageReceiver<?> message) for async handling (Mutiny).
    - Registration options:
      - Preferred: ServiceLoader — add your implementation class name to META-INF/services/com.guicedee.client.services.websocket.IWebSocketMessageReceiver so it is auto-discovered.
      - Programmatic: call IGuicedWebSocket.addWebSocketMessageReceiver(receiver) during startup.
    - See also: docs/architecture/sequence-websocket-receivers.md
  - Registry and broadcasting (IGuicedWebSocket)
    - Manages a shared map of messageListeners keyed by action name.
    - Helpers: addWebSocketMessageReceiver, isWebSocketReceiverRegistered, getMessagesListeners, loadWebSocketReceivers.
    - Groups: addToGroup(String), removeFromGroup(String); EveryoneGroup constant available.
  - Group and publish hooks
    - GuicedWebSocketOnAddToGroup — onAddToGroup(String) returns CompletableFuture<Boolean> indicating completion/already-done.
    - GuicedWebSocketOnRemoveFromGroup — onRemoveFromGroup(String) returns CompletableFuture<Boolean>.
    - GuicedWebSocketOnPublish — publish(String groupName, String message) may throw Exception; return boolean to indicate handling.
  - Auth/bootstrap and pre-configuration
    - IWebSocketAuthDataProvider — provide client-side bootstrap/auth JavaScript via getJavascriptToPopulate(); name() returns a unique id.
    - IWebSocketPreConfiguration — perform any pre-configuration before sockets usage.
  - Scope note: These are client-side SPI contracts; this module does not host a WebSocket server.

Acceptance Criteria (initial)
- Startup sequence completes with succeeded Future and logs configured scanning options.
- SPI-discovered modules (IGuiceModule) are applied in sortOrder by GuiceContext without manual createInjector(); no runtime errors when guiced-injection is on classpath.
- Client components can be obtained via IGuiceContext.getInstance(...) and via @Inject.
- Lifecycle services are enumerated and ordered; idempotent behavior is documented.

Cross-Links
- PACT: PACT.md
- RULES: RULES.md
- IMPLEMENTATION: IMPLEMENTATION.md
- GLOSSARY: GLOSSARY.md
- Architecture Diagrams: docs/architecture/README.md