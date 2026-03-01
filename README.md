# GuicedEE Inject Client

[![Build](https://github.com/GuicedEE/Client/actions/workflows/build.yml/badge.svg)](https://github.com/GuicedEE/Client/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=GuicedEE_Client&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=GuicedEE_Client)
[![Maven Central](https://img.shields.io/maven-central/v/com.guicedee/guice-inject-client)](https://central.sonatype.com/artifact/com.guicedee/guice-inject-client)
[![Maven Snapshot](https://img.shields.io/nexus/s/com.guicedee/guice-inject-client?server=https%3A%2F%2Foss.sonatype.org&label=Maven%20Snapshot)](https://oss.sonatype.org/content/repositories/snapshots/com/guicedee/guice-inject-client/)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)
![Java 25+](https://img.shields.io/badge/Java-25%2B-green)
![Modular](https://img.shields.io/badge/Modular-Level3-green)
![Guice 7](https://img.shields.io/badge/Guice-7-green)
![Vert.X 5](https://img.shields.io/badge/Vert.x-5-green)
![Maven 4](https://img.shields.io/badge/Maven-4-green)

Dependency injection interfaces, lifecycle hooks, and scoping primitives for the [GuicedEE](https://github.com/GuicedEE) ecosystem.
Built on [Google Guice](https://github.com/google/guice) and fully modularized for JPMS (`com.guicedee.client`).

This library is the **client SPI** — it defines the contracts that all GuicedEE modules program against without pulling in the full runtime.

## 📦 Installation

```xml
<dependency>
    <groupId>com.guicedee</groupId>
    <artifactId>client</artifactId>
</dependency>
```

<details>
<summary>Gradle (Kotlin DSL)</summary>

```kotlin
implementation("com.guicedee:guice-inject-client:2.0.0-SNAPSHOT")
```
</details>

## 🚀 Quick Start

Register your application module for classpath scanning, then let GuicedEE discover everything automatically:

```java
// Enable your module for classpath scanning
IGuiceContext.registerModuleForScanning.add("my.app");

// Bootstrap — modules and lifecycle hooks are discovered via ServiceLoader
IGuiceContext context = IGuiceContext.instance();
Injector injector = context.inject();

// Use managed instances
MyService svc = injector.getInstance(MyService.class);
```

Standard Guice injection works everywhere:

```java
@Inject
private MyService svc;

@Inject
public void onCreate() {
    // called after injection
}
```

Register your own module with a single JPMS `provides` directive:

```java
module my.app {
    requires com.guicedee.client;

    provides com.guicedee.client.services.lifecycle.IGuiceModule
        with my.app.AppModule;
}
```

## 🔄 Lifecycle

All lifecycle hooks implement `IDefaultService` — override `sortOrder()` to control execution order.

```
IGuicePreStartup  →  Injector created  →  IGuicePostStartup
                                                   ↓
                                          IGuicePreDestroy (shutdown)
```

| Interface | Runs |
|---|---|
| `IGuicePreStartup` | Before the injector is created |
| `IGuicePostStartup` | After the injector is created |
| `IGuicePreDestroy` | On shutdown or context teardown |

## 🔒 Scoping

`CallScope` provides request-style scoping that works outside of servlets — including reactive Mutiny pipelines.

| Class | Purpose |
|---|---|
| `CallScope` | Guice scope for per-request state |
| `CallScoper` | Opens and closes a scope boundary |
| `CallScopeUniInterceptor` | Propagates scope across Mutiny `Uni` chains |

## 🔌 SPI Contracts

| Interface | Purpose |
|---|---|
| `IGuiceContext` | Bootstrap, injection, and service loading |
| `IGuiceModule` | Contribute Guice bindings |
| `IGuiceProvider` | Supply the `IGuiceContext` implementation |
| `IGuiceConfigurator` | Configure classpath scanning and module filtering |

## 🗺️ Module Graph

```
com.guicedee.client
 ├── com.google.guice
 ├── io.github.classgraph
 ├── com.fasterxml.jackson.databind
 ├── io.vertx.core
 └── io.smallrye.mutiny
```

## 📝 Logging — `LogUtils`

`LogUtils` provides one-call Log4j2 appender setup — no XML configuration files needed.
When the `CLOUD` environment variable is set, all layouts automatically switch to compact JSON for log aggregator ingestion.

```java
// ANSI-highlighted console output (local development)
LogUtils.addHighlightedConsoleLogger();

// Plain console output at a specific level
LogUtils.addConsoleLogger(Level.INFO);

// Rolling file logger (time + size based, auto-rollover on startup)
LogUtils.addFileRollingLogger("my-app", "logs");

// Dedicated logger with its own file — isolated from the root logger
Logger auditLog = LogUtils.getSpecificRollingLogger(
    "audit",        // logger name & file base
    "logs/audit",   // directory
    null,           // pattern (null = default)
    false           // additive — false keeps it out of the root logger
);
auditLog.info("User signed in");
```

| Method | What it does |
|---|---|
| `addConsoleLogger()` | Adds a stdout appender with a default pattern |
| `addHighlightedConsoleLogger()` | Adds a stdout appender with ANSI `%highlight` colors |
| `addFileRollingLogger(name, dir)` | Adds a rolling file appender (100 MB / daily rollover) |
| `addMinimalFileRollingLogger(name)` | Same as above with a `logs/` default directory |
| `getSpecificRollingLogger(…)` | Returns a named `Logger` with its own isolated rolling file |
| `addAppender(appender, level)` | Escape hatch — attach any Log4j2 `Appender` to the root logger |

## 🔍 Classpath Scanner Configuration

GuicedEE uses [ClassGraph](https://github.com/classgraph/classgraph) under the hood.
The scan scope is controlled through a set of SPI interfaces — implement them and register via `ServiceLoader` / JPMS `provides`.

### Module & JAR filtering

| SPI Interface | Method | Purpose |
|---|---|---|
| `IGuiceScanModuleInclusions` | `includeModules()` | Modules to **include** in scanning |
| `IGuiceScanModuleExclusions` | `excludeModules()` | Modules to **exclude** from scanning |
| `IGuiceScanJarInclusions` | `includeJars()` | JAR filenames to **include** |
| `IGuiceScanJarExclusions` | `excludeJars()` | JAR filenames to **exclude** |

```java
public class MyModuleExclusions
        implements IGuiceScanModuleExclusions<MyModuleExclusions> {

    @Override
    public Set<String> excludeModules() {
        return Set.of("java.sql", "jdk.crypto.ec");
    }
}
```

### Package & path filtering

| SPI Interface | Method | Purpose |
|---|---|---|
| `IPackageContentsScanner` | `searchFor()` | Packages to **include** in the scan |
| `IPackageRejectListScanner` | `exclude()` | Packages to **exclude** from the scan |
| `IPathContentsScanner` | `searchFor()` | Resource paths to **include** |
| `IPathContentsRejectListScanner` | `searchFor()` | Resource paths to **exclude** |

### File content scanners

These fire during the ClassGraph scan and let you process matched resources inline:

| SPI Interface | Method | Match by |
|---|---|---|
| `IFileContentsScanner` | `onMatch()` | Exact file name (`Map<String, ByteArrayConsumer>`) |
| `IFileContentsPatternScanner` | `onMatch()` | Regex pattern (`Map<Pattern, ByteArrayConsumer>`) |

```java
public class LiquibaseFileScanner implements IFileContentsScanner {

    @Override
    public Map<String, ResourceList.ByteArrayConsumer> onMatch() {
        return Map.of("changelog.xml", (resource, bytes) -> {
            // process the matched resource bytes
        });
    }
}
```

## 🤝 Contributing

Issues and pull requests are welcome.

## 📄 License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
