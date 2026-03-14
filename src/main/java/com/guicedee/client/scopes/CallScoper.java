package com.guicedee.client.scopes;

import com.google.common.collect.Maps;
import com.google.inject.*;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IOnCallScopeEnter;
import com.guicedee.client.services.lifecycle.IOnCallScopeExit;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Guice {@link Scope} implementation that manages a per-call thread-local scope.
 * <p>
 * Entering a scope seeds {@link CallScopeProperties} and triggers lifecycle callbacks.
 */
@Singleton
public class CallScoper implements Scope
{
    /**
     * Creates a new call scoper.
     */
    public CallScoper() {
    }

    private static final Provider<Object> SEEDED_KEY_PROVIDER =
            new Provider<Object>()
            {
                public Object get()
                {
                    throw new IllegalStateException("If you got here then it means that" +
                                                            " your code asked for scoped object which should have been" +
                                                            " explicitly seeded in this scope by calling" +
                                                            " SimpleScope.seed(), but was not.");
                }
            };
    private static final ThreadLocal<Map<Key<?>, Object>> values
            = new ThreadLocal<Map<Key<?>, Object>>();

    /**
     * Indicates whether the current thread has an active call scope.
     *
     * @return true when a scope is active
     */
    public boolean isStartedScope()
    {
        return values.get() != null;
    }

    /**
     * Enters a new call scope for the current thread and notifies enter listeners.
     */
    public void enter()
    {
        checkState(values.get() == null, "A scoping block is already in progress");
        values.set(Maps.<Key<?>, Object>newHashMap());
        // Seed CallScopeProperties and explicitly mark the source as Unknown on scope start
        CallScopeProperties props = new CallScopeProperties();
        props.setSource(CallScopeSource.Unknown);
        seed(CallScopeProperties.class, props);
        @SuppressWarnings("rawtypes")
        Set<IOnCallScopeEnter> scopeEnters = IGuiceContext.loaderToSet(ServiceLoader.load(IOnCallScopeEnter.class));
        for (IOnCallScopeEnter<?> scopeEnter : scopeEnters)
        {
            try
            {
                scopeEnter.onScopeEnter(this);
            }
            catch (Throwable T)
            {
                Logger.getLogger("CallScoper")
                        .log(Level.WARNING, "Exception on scope entry - " + scopeEnter, T);
            }
        }
    }

    /**
     * Returns the map of scoped values for the current thread.
     *
     * @return the scoped values, or null if no scope exists
     */
    public Map<Key<?>, Object> getValues()
    {
        return values.get();
    }

    /**
     * Copies the provided scoped values into the current scope.
     *
     * @param values the values to merge into this scope
     */
    public void setValues(Map<Key<?>, Object> values)
    {
        this.values.get().putAll(values);
    }

    /**
     * Exits the current call scope and notifies exit listeners.
     */
    public void exit()
    {
        checkState(values.get() != null, "No scoping block in progress");
        Set<IOnCallScopeExit> scopeExits = IGuiceContext.loaderToSet(ServiceLoader.load(IOnCallScopeExit.class));
        for (IOnCallScopeExit<?> scopeExit : scopeExits)
        {
            try
            {
                scopeExit.onScopeExit();
            }
            catch (Throwable T)
            {
                Logger.getLogger("CallScoper")
                        .log(Level.WARNING, "Exception on call scope exit - " + scopeExit, T);
            }
        }
        values.remove();
    }

    /**
     * Seeds a value into the current scope under the given key.
     *
     * @param key the scope key
     * @param value the value to seed
     * @param <T> the value type
     */
    public <T> void seed(Key<T> key, T value)
    {
        Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
        checkState(!scopedObjects.containsKey(key), "A value for the key %s was " +
                                                            "already seeded in this scope. Old value: %s New value: %s", key,
                scopedObjects.get(key), value);
        scopedObjects.put(key, value);
    }

    /**
     * Seeds a value into the current scope using a class key.
     *
     * @param clazz the scope key class
     * @param value the value to seed
     * @param <T> the value type
     */
    public <T> void seed(Class<T> clazz, T value)
    {
        seed(Key.get(clazz), value);
    }

    /**
     * Returns a provider that respects call scope and caches scoped instances.
     *
     * @param key the scope key
     * @param unscoped the unscoped provider
     * @param <T> the value type
     * @return a scoped provider
     */
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped)
    {
        return new Provider<T>()
        {
            public T get()
            {
                Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);

                @SuppressWarnings("unchecked")
                T current = (T) scopedObjects.get(key);
                if (current == null && !scopedObjects.containsKey(key))
                {
                    current = unscoped.get();

                    // don't remember proxies; these exist only to serve circular dependencies
                    if (Scopes.isCircularProxy(current))
                    {
                        return current;
                    }

                    scopedObjects.put(key, current);
                }
                return current;
            }
        };
    }

    private <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key)
    {
        Map<Key<?>, Object> scopedObjects = values.get();
        if (scopedObjects == null)
        {
            throw new OutOfScopeException("Cannot access " + key
                                                  + " outside of a scoping block");
        }
        return scopedObjects;
    }

    /**
     * Returns a provider that always throws an exception when used, indicating the key must be seeded.
     *
     * @param <T> the value type
     * @return a provider that throws when accessed
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Provider<T> seededKeyProvider()
    {
        return (Provider<T>) SEEDED_KEY_PROVIDER;
    }
}
