## Sequence — WebSocket Receiver Discovery and Registration

```mermaid
sequenceDiagram
    participant App as Host Application
    participant WS as IGuicedWebSocket (Registry)
    participant SL as ServiceLoader<IWebSocketMessageReceiver>
    participant Rec as IWebSocketMessageReceiver Impl

    Note over App,WS: Startup phase (registry initialization)
    App->>WS: getMessagesListeners()
    WS->>WS: check messageListeners.isEmpty()
    alt empty
        WS->>SL: load(IWebSocketMessageReceiver)
        SL-->>WS: Set<IWebSocketMessageReceiver>
        loop for each receiver
            WS->>Rec: messageNames()
            Rec-->>WS: Set<String> names
            WS->>WS: addReceiver(Rec, name)
        end
    end
    WS-->>App: listeners map ready

    Note over App,Rec: Programmatic registration (optional)
    App->>WS: addWebSocketMessageReceiver(Rec)
    WS->>Rec: messageNames()
    Rec-->>WS: Set<String> names
    WS->>WS: addReceiver(Rec, name)

    Note over WS,Rec: Message handling at runtime (conceptual)
    App->>WS: getMessagesListeners().get(action)
    WS-->>App: Rec
    App->>Rec: receiveMessage(WebSocketMessageReceiver<?> msg)
    Rec-->>App: Uni<Result>
```

Notes
- Mirrors logic in IGuicedWebSocket: addWebSocketMessageReceiver, getMessagesListeners, loadWebSocketReceivers, addReceiver.
- Receivers declare supported actions via messageNames(); messages are handled asynchronously with Mutiny Uni.
- This is client-side SPI wiring; no WebSocket server is hosted in this module.
