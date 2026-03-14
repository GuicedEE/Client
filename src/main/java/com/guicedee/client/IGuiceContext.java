package com.guicedee.client;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.guicedee.client.annotations.INotEnhanceable;
import com.guicedee.client.annotations.INotInjectable;
import com.guicedee.client.services.IDefaultService;
import com.guicedee.client.services.IGuiceConfig;
import com.guicedee.client.services.IGuiceProvider;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.vertx.core.Future;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central access point for the GuicedEE client injector, configuration, and lifecycle loading.
 * <p>
 * Provides static helpers for bootstrap, service loading, and scoped injection while exposing
 * instance-level hooks for lifecycle coordination.
 */
public interface IGuiceContext {
    /**
     * Registered Guice contexts keyed by name (typically "default").
     */
    Map<String, IGuiceContext> contexts = new HashMap<>();
    /**
     * Module names registered for scanning when filtering is enabled.
     */
    Set<String> registerModuleForScanning = new LinkedHashSet<>();
    /**
     * Additional Guice modules to include during injector creation.
     */
    List<com.google.inject.Module> modules = new ArrayList<>();
    /**
     * Cache of loaded service types and their resolved implementations.
     */
    Map<Class<?>, Set<?>> allLoadedServices = new LinkedHashMap<>();

    /**
     * Returns the shared cache of loaded service types.
     *
     * @return the loaded service cache
     */

    @SuppressWarnings("LombokGetterMayBeUsed")
    static Map<Class<?>, Set<?>> getAllLoadedServices() {
        return allLoadedServices;
    }

    /**
     * Returns the default Guice context, initializing it via {@link ServiceLoader} if needed.
     * Best not to log before this to apply proper logging
     *
     * @return the default {@link IGuiceContext}
     */
    static IGuiceContext getContext() {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        if (contexts.isEmpty()) {
            ServiceLoader<IGuiceProvider> load = ServiceLoader.load(IGuiceProvider.class);
            for (IGuiceProvider iGuiceProvider : load) {
                IGuiceContext iGuiceContext = iGuiceProvider.get();
                //todo make this allow multiples for tests or something
                contexts.put("default", iGuiceContext);
                break;
            }
        }
        var out = contexts.get("default");
        if (out == null) {
            try {
                Class<?> guiceContextClass = Class.forName("com.guicedee.guicedinjection.GuiceContext");
                Object instance = guiceContextClass.getMethod("instance").invoke(null);
                if (instance instanceof IGuiceContext ctx) {
                    contexts.put("default", ctx);
                    ctx.getConfig().setClasspathScanning(true).setAnnotationScanning(true).setMethodInfo(true).setFieldInfo(true);
                    out = ctx;
                }
            } catch (Exception e) {
                // Class.forName fallback failed
            }
            if (out == null) {
                throw new RuntimeException("No Guice Contexts have been registered. Please add com.guicedee:inject to the dependencies");
            }
        }
        return out;
    }

    /**
     * Alias for {@link #getContext()}.
     *
     * @return the default {@link IGuiceContext}
     */
    static IGuiceContext instance() {
        return getContext();
    }

    /**
     * Returns a future completed when the injector loading process finishes.
     *
     * @return the loading completion future
     */
    Future<Void> getLoadingFinished();

    /**
     * Returns the Guice injector for this context.
     *
     * @return the injector
     */
    Injector inject();

    /**
     * Returns the Guice configuration backing this context.
     *
     * @return the configuration
     */
    IGuiceConfig<?> getConfig();

    /**
     * Shuts down the context and releases resources.
     */
    void destroy();

    /**
     * Resolves an instance from the injector, optionally constructing and member-injecting entities.
     *
     * @param type the Guice key to resolve
     * @param <T>  the resolved type
     * @return the resolved instance
     */
    static <T> T get(Key<T> type) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) type
                .getTypeLiteral()
                .getRawType();
        T instance;
        boolean isEntityType = isEntityType(clazz);
        if (isNotEnhanceable(clazz) || isEntityType) {
            try {
                instance = clazz
                        .getDeclaredConstructor()
                        .newInstance();
                if (!isNotInjectable(clazz)) {
                    getContext()
                            .inject()
                            .injectMembers(instance)
                    ;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            instance = getContext()
                    .inject()
                    .getInstance(type);
        }
        return instance;
    }

    private static boolean isNotEnhanceable(Class<?> clazz) {
        return clazz.isAnnotationPresent(INotEnhanceable.class);
    }

    private static boolean isNotInjectable(Class<?> clazz) {
        return clazz.isAnnotationPresent(INotInjectable.class);
    }

    private static boolean isEntityType(Class<?> clazz) {
        try {
            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation
                        .annotationType()
                        .getCanonicalName()
                        .equalsIgnoreCase("jakarta.persistence.Entity")) {
                    return true;
                }
            }
        } catch (NullPointerException npe) {
            return false;
        }
        return false;
    }

    /**
     * Resolves an instance from the injector using an optional binding annotation.
     *
     * @param type       the requested type
     * @param annotation the binding annotation, or {@code null} for the default binding
     * @param <T>        the resolved type
     * @return the resolved instance
     */
    static <T> T get(Class<T> type, Class<? extends Annotation> annotation) {
        if (annotation == null) {
            return get(Key.get(type));
        }
        return get(Key.get(type, annotation));
    }

    /**
     * Resolves an instance from the injector using the default binding.
     *
     * @param type the requested type
     * @param <T>  the resolved type
     * @return the resolved instance
     */
    static <T> T get(Class<T> type) {
        return get(type, null);
    }

    /**
     * Returns the classpath scan result associated with this context.
     *
     * @return the scan result
     */
    ScanResult getScanResult();

    /**
     * Cache of service loader type names to resolved implementation classes.
     */
    Map<String, Set<Class>> loaderClasses = new ConcurrentHashMap<>();

    /**
     * Loads service implementations into a sorted set, optionally scanning the classpath.
     *
     * @param loader the service loader to enumerate
     * @param <T>    the service type
     * @return a sorted set of injected service instances
     */
    static <T extends Comparable<T>> Set<T> loaderToSet(ServiceLoader<T> loader) {
        @SuppressWarnings("rawtypes")
        Set<Class> loadeds = new HashSet<>();

        String type = loader.toString();
        type = type.replace("java.util.ServiceLoader[", "");
        type = type.substring(0, type.length() - 1);
        if (!loaderClasses.containsKey(type)) {
            IGuiceConfig<?> config = getContext().getConfig();
            if (config.isServiceLoadWithClassPath()) {
                for (ClassInfo classInfo : instance()
                        .getScanResult()
                        .getClassesImplementing(type)) {
                    Class<T> load = (Class<T>) classInfo.loadClass();
                    loadeds.add(load);
                }
            }
            try {
                for (T newInstance : loader) {
                    loadeds.add(newInstance.getClass());
                }
            } catch (Throwable T) {
                LogManager.getLogger(IGuiceContext.class).error("❌ Failed to provide instance of '{}' to TreeSet: {}", type, T.getMessage(), T);
            }
            loaderClasses.put(type, loadeds);
        }

        Set<T> outcomes = new TreeSet<>();
        for (Class<?> aClass : loaderClasses.get(type)) {
            outcomes.add((T) IGuiceContext.get(aClass));
        }
        return outcomes;
    }

    /**
     * Loads service implementations without member injection.
     *
     * @param loader the service loader to enumerate
     * @param <T>    the service type
     * @return a set of instantiated services
     */
    static <T> Set<T> loaderToSetNoInjection(ServiceLoader<T> loader) {
        Set<Class<T>> loadeds = new HashSet<>();
        IGuiceConfig<?> config = getContext().getConfig();
        String type = loader.toString();
        type = type.replace("java.util.ServiceLoader[", "");
        type = type.substring(0, type.length() - 1);
        if (config.isServiceLoadWithClassPath() && instance().getScanResult() != null) {
            for (ClassInfo classInfo : instance()
                    .getScanResult()
                    .getClassesImplementing(type)) {
                Class<T> load = (Class<T>) classInfo.loadClass();
                loadeds.add(load);
            }
        }

        Set<Class<T>> completed = new LinkedHashSet<>();
        Set<T> output = new LinkedHashSet<>();
        try {
            for (T newInstance : loader) {
                output.add(newInstance);
                completed.add((Class<T>) newInstance.getClass());
            }
        } catch (Throwable T) {
            LogManager.getLogger(IGuiceContext.class).error("❌ Cannot load services for '{}': {}", type, T.getMessage(), T);
        }
        return output;
    }

    /**
     * Loads the implementation classes for a service loader without instantiating them.
     *
     * @param loader the service loader to enumerate
     * @param <T>    the service type
     * @return the set of implementation classes
     */
    static <T extends Comparable<T>> Set<Class<T>> loadClassSet(ServiceLoader<T> loader) {
        String type = loader.toString();
        type = type.replace("java.util.ServiceLoader[", "");
        type = type.substring(0, type.length() - 1);

        if (!loaderClasses.containsKey(type)) {
            Set<Class> loadeds = new HashSet<>();
            IGuiceConfig<?> config = getContext().getConfig();
            if (config.isServiceLoadWithClassPath()) {
                for (ClassInfo classInfo : instance()
                        .getScanResult()
                        .getClassesImplementing(type)) {
                    @SuppressWarnings("unchecked")
                    Class<T> load = (Class<T>) classInfo.loadClass();
                    loadeds.add(load);
                }
            } else {
            }

            try {
                for (T newInstance : loader) {
                    //noinspection unchecked
                    Class<T> implementationClass = (Class<T>) newInstance.getClass();
                    loadeds.add(implementationClass);
                }
            } catch (Throwable T) {
                LogManager.getLogger(IGuiceContext.class).error("❌ Failed to provide instance of '{}' to TreeSet: {}", type, T.getMessage(), T);
            }

            loaderClasses.put(type, loadeds);
        }

        //noinspection unchecked
        Set<Class<T>> result = (Set) loaderClasses.get(type);
        return result;
    }

    /**
     * Loads services through the provided loader and injects them if required by the context.
     *
     * @param loaderType    the service type
     * @param serviceLoader the backing service loader
     * @param <T>           the service type
     * @return a set of loaded service instances
     */
    <T extends IDefaultService<T>> Set<T> getLoader(Class<T> loaderType, ServiceLoader<T> serviceLoader);

    /**
     * Loads services with optional injection suppression.
     *
     * @param loaderType    the service type
     * @param dontInject    when true, bypasses injector member injection
     * @param serviceLoader the backing service loader
     * @param <T>           the service type
     * @return a set of loaded service instances
     */
    <T extends IDefaultService<T>> Set<T> getLoader(Class<T> loaderType, @SuppressWarnings("unused") boolean dontInject, ServiceLoader<T> serviceLoader);

    /**
     * Indicates whether the injector is currently being built.
     *
     * @return true when injector creation is in progress
     */
    boolean isBuildingInjector();

    /**
     * Registers a Java module for scanning when filtering is enabled.
     *
     * @param javaModuleName the module name from {@code module-info.java}
     */
    @SuppressWarnings("unchecked")
    static void registerModule(String javaModuleName) {
        registerModuleForScanning.add(javaModuleName);
        instance()
                .getConfig()
                .setIncludeModuleAndJars(true)
        ;
    }

    /**
     * Adds a Guice module to the injector for processing.
     *
     * @param module the Guice module to register
     */
    static void registerModule(com.google.inject.Module module) {
        modules.add(module);
    }

    /**
     * Loads pre-destroy lifecycle services in call order.
     *
     * @return the pre-destroy service set
     */
    Set<IGuicePreDestroy> loadPreDestroyServices();

    /**
     * Loads pre-startup lifecycle services in call order.
     *
     * @return the pre-startup service set
     */
    Set<IGuicePreStartup> loadPreStartupServices();
}
