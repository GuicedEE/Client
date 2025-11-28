## Sequence — Client Injection (SPI-based)

```mermaid
sequenceDiagram
    participant App as Host Application
    participant Ctx as IGuiceContext
    participant SL as ServiceLoader<IGuiceModule>
    participant Mod as IGuiceModule (providers)
    participant Comp as Client Components

    Note over App,Ctx: Injector/modules are not created manually
    App->>Ctx: instance()
    Ctx-->>App: context
    App->>Ctx: getConfig() / boot
    Ctx->>SL: load(IGuiceModule)
    SL-->>Ctx: Set<IGuiceModule>
    loop ordered by sortOrder()
        Ctx->>Mod: configure(binder) (bindings applied)
        Mod-->>Ctx: bindings contributed
    end
    Note over Ctx,Comp: Retrieval options for client components
    App->>Ctx: getInstance(MyType)
    Ctx-->>App: instance
    App->>Comp: @Inject fields/ctor (handled by Guice)
    Comp-->>App: ready
```

Notes
- Modules are discovered via ServiceLoader as IGuiceModule and applied by GuiceContext in sortOrder() sequence; no createInjector(new Module()) here.
- Client components can be obtained via IGuiceContext.getInstance(...) or by using @Inject.
- Pre/Post startup services are also discovered and may be grouped by sortOrder for execution.
