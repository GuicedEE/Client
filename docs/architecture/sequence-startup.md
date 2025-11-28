## Sequence — Client Startup

```mermaid
sequenceDiagram
    participant App as Host Application
    participant Startup as GuicedEEClientStartup
    participant Ctx as IGuiceContext
    participant Cfg as Config

    App->>Startup: onStartup()
    activate Startup
    Startup->>Ctx: instance()
    Ctx-->>Startup: context
    Startup->>Cfg: getConfig()
    Startup->>Cfg: setFieldScanning(true)
    Startup->>Cfg: setMethodInfo(true)
    Startup->>Cfg: setIgnoreClassVisibility(true)
    Startup->>Cfg: setIgnoreMethodVisibility(true)
    Startup->>Cfg: setIgnoreFieldVisibility(true)
    Startup->>Cfg: setAnnotationScanning(true)
    Startup-->>App: Future.succeededFuture(true)
    deactivate Startup
```

Notes
- Mirrors implementation in com.guicedee.client.implementations.GuicedEEClientStartup.
- Pre and Post startup services can be grouped and executed deterministically using sortOrder() values.
