package com.guicedee.client.scopes;

import com.google.common.collect.Maps;
import com.google.inject.*;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IOnCallScopeEnter;
import com.guicedee.client.services.lifecycle.IOnCallScopeExit;
import io.vertx.core.Vertx;
import io.vertx.core.Context;
import io.vertx.core.spi.context.storage.ContextLocal;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Guice {@link Scope} implementation that manages a per-call scope backed by the
 * current Vert.x {@link Context} local data.
 * <p>
 * A Vert.x context must be available on the current thread; if not, an
 * {@link IllegalStateException} is thrown.
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
    @SuppressWarnings("unchecked")
    private static final ContextLocal<Map<Key<?>, Object>> SCOPE_LOCAL_KEY =
            (ContextLocal<Map<Key<?>, Object>>) (ContextLocal<?>) ContextLocal.registerLocal(Map.class);

    /**
     * Tracks how many participants have entered the scope on the current context.
     * <p>
     * A Vert.x context (and therefore its context-locals) is shared by every task
     * running on it — including blocking tasks dispatched with
     * {@code executeBlocking} and concurrently subscribed {@code Uni} chains. Without
     * a depth counter the first {@link #exit()} would tear the scope down for all the
     * other participants, causing {@code No scoping block in progress} failures.
     */
    private static final ContextLocal<AtomicInteger> SCOPE_DEPTH_KEY =
            ContextLocal.registerLocal(AtomicInteger.class);

    /**
     * Returns the current Vert.x context, or null if not on a Vert.x thread.
     */
    private static Context vertxContext()
    {
        return Vertx.currentContext();
    }

    /**
     * Returns the scoped-values map from the current Vert.x context.
     *
     * @throws IllegalStateException if no Vert.x context is available
     */
    private static Map<Key<?>, Object> currentScopeMap()
    {
        Context ctx = vertxContext();
        if (ctx != null)
        {
            try
            {
                return ctx.getLocal(SCOPE_LOCAL_KEY);
            }
            catch (IllegalArgumentException e)
            {
                return null;
            }
        }
        return null;
    }

    /**
     * Stores the scoped-values map into the current Vert.x context.
     *
     * @throws IllegalStateException if no Vert.x context is available
     */
    private static void setScopeMap(Map<Key<?>, Object> map)
    {
        Context ctx = vertxContext();
        if (ctx != null)
        {
            if (map != null)
            {
                ctx.putLocal(SCOPE_LOCAL_KEY, map);
            }
            else
            {
                ctx.removeLocal(SCOPE_LOCAL_KEY);
                try
                {
                    ctx.removeLocal(SCOPE_DEPTH_KEY);
                }
                catch (IllegalArgumentException localNotAvailable)
                {
                    // context created before this local was registered - nothing to clean up
                }
            }
        }
        else
        {
            throw new IllegalStateException(
                    "No Vert.x context available on the current thread. " +
                    "CallScoper requires a Vert.x context — ensure this code runs on a Vert.x event-loop, worker, or virtual thread.");
        }
    }

    /**
     * Returns the re-entrancy counter of the active scope on this context, or null when absent.
     */
    private static AtomicInteger currentDepth()
    {
        Context ctx = vertxContext();
        if (ctx == null)
        {
            return null;
        }
        try
        {
            return ctx.getLocal(SCOPE_DEPTH_KEY);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    /**
     * Registers this participant against the active scope on the current context.
     */
    private static void enterDepth()
    {
        Context ctx = vertxContext();
        if (ctx == null)
        {
            return;
        }
        try
        {
            AtomicInteger depth = currentDepth();
            if (depth == null)
            {
                ctx.putLocal(SCOPE_DEPTH_KEY, new AtomicInteger(1));
            }
            else
            {
                depth.incrementAndGet();
            }
        }
        catch (IllegalArgumentException localNotAvailable)
        {
            // context created before this local was registered - fall back to single depth behaviour
        }
    }

    /**
     * Deregisters this participant and reports whether the scope must still stay open.
     *
     * @return true when other participants are still inside the scope
     */
    private static boolean exitDepthAndStillActive()
    {
        AtomicInteger depth = currentDepth();
        return depth != null && depth.decrementAndGet() > 0;
    }

    /**
     * Indicates whether the current Vert.x context has an active call scope.
     * <p>
     * Returns {@code false} if no Vert.x context is available (rather than throwing).
     *
     * @return true when a scope is active
     */
    public boolean isStartedScope()
    {
        return currentScopeMap() != null;
    }

    /**
     * Enters a new call scope on the current Vert.x context and notifies enter listeners.
     * <p>
     * The call is re-entrant: when a scope is already active on this context the existing
     * scope is joined and only the matching number of {@link #exit()} calls will close it.
     */
    public void enter()
    {
        if(currentScopeMap() != null)
        {
            enterDepth();
            Logger.getLogger("CallScoper")
                    .log(Level.FINEST, "A call scope is already active on this context - joining it.");
        }else {
            setScopeMap(Maps.<Key<?>, Object>newHashMap());
            enterDepth();
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
    }

    /**
     * Enters a new call scope on the current Vert.x context <b>without</b> notifying
     * lifecycle listeners. Use this when a scope is needed but the full Guice context
     * should not be bootstrapped (e.g. in Maven plugins or tooling).
     */
    public void enterQuietly()
    {
        checkState(currentScopeMap() == null, "A scoping block is already in progress");
        setScopeMap(Maps.<Key<?>, Object>newHashMap());
        enterDepth();
        CallScopeProperties props = new CallScopeProperties();
        props.setSource(CallScopeSource.Unknown);
        seed(CallScopeProperties.class, props);
    }

    /**
     * Exits the current call scope <b>without</b> notifying lifecycle listeners.
     * Counterpart to {@link #enterQuietly()}.
     */
    public void exitQuietly()
    {
        checkState(currentScopeMap() != null, "No scoping block in progress");
        if (exitDepthAndStillActive())
        {
            return;
        }
        setScopeMap(null);
    }

    /**
     * Returns the map of scoped values for the current Vert.x context.
     *
     * @return the scoped values, or null if no scope exists
     */
    public Map<Key<?>, Object> getValues()
    {
        return currentScopeMap();
    }

    /**
     * Copies the provided scoped values into the current scope.
     *
     * @param values the values to merge into this scope
     */
    public void setValues(Map<Key<?>, Object> values)
    {
        currentScopeMap().putAll(values);
    }

    /**
     * Exits the current call scope and notifies exit listeners.
     * <p>
     * When the scope was joined re-entrantly the scope stays open until the last
     * participant exits.
     */
    public void exit()
    {
        checkState(currentScopeMap() != null, "No scoping block in progress");
        if (exitDepthAndStillActive())
        {
            // another participant on this context is still inside the scope
            return;
        }
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
        setScopeMap(null);
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
        Map<Key<?>, Object> scopedObjects = currentScopeMap();
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
