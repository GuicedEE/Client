package com.guicedee.client.test;

import com.google.inject.*;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScoper;
import com.guicedee.client.scopes.mutiny.CallScopeUniInterceptor;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class UniBootstrapIsolationTest {
    Map<String,IGuiceContext> previous;
    @BeforeEach void save() {previous=new HashMap<>(IGuiceContext.contexts);IGuiceContext.contexts.clear();}
    @AfterEach void restore() {IGuiceContext.contexts.clear();IGuiceContext.contexts.putAll(previous);}
    IGuiceContext context(Optional<Injector> injector,AtomicInteger boots) {
        return (IGuiceContext)Proxy.newProxyInstance(IGuiceContext.class.getClassLoader(),new Class[]{IGuiceContext.class},(proxy,method,args) -> {
            if(method.getName().equals("existingInjector"))return injector;
            if(method.getName().equals("getConfig"))return Proxy.newProxyInstance(com.guicedee.client.services.IGuiceConfig.class.getClassLoader(),
                    new Class[]{com.guicedee.client.services.IGuiceConfig.class},(p,m,a) -> {
                        if(m.getName().equals("isServiceLoadWithClassPath"))return false;
                        throw new AssertionError("Unexpected configuration operation: "+m.getName());
                    });
            if(method.getName().equals("inject")) {boots.incrementAndGet();throw new IllegalStateException("fixture bootstrap must not run");}
            throw new AssertionError("Unexpected context operation: "+method.getName());
        });
    }
    @Test void creatingUniWithoutContextDoesNotDiscoverOrBootstrapProviders() {
        assertEquals("value",Uni.createFrom().item("value").await().indefinitely());
        assertTrue(IGuiceContext.contexts.isEmpty());
    }
    @Test void registeredButUninitializedContextIsNeverBootstrappedByInterceptor() {
        var boots=new AtomicInteger();IGuiceContext.contexts.put("default",context(Optional.empty(),boots));
        var source=Uni.createFrom().item("value");
        assertSame(source,new CallScopeUniInterceptor().onUniCreation(source));
        assertEquals("value",source.await().indefinitely());assertEquals(0,boots.get());
    }
    @Test void failureFromExistingScopeProviderIsNotSwallowedAsMissingBootstrap() {
        var source=Uni.createFrom().item("value");var boots=new AtomicInteger();
        var injector=Guice.createInjector(new AbstractModule(){protected void configure(){
            bind(CallScoper.class).toProvider(() -> {throw new IllegalStateException("fixture scope failure");});
        }});
        IGuiceContext.contexts.put("default",context(Optional.of(injector),boots));
        assertThrows(ProvisionException.class,() -> new CallScopeUniInterceptor().onUniCreation(source));assertEquals(0,boots.get());
    }
    @Test void initializedScopeIsResolvedWithoutInvokingBootstrap() {
        var boots=new AtomicInteger();var resolutions=new AtomicInteger();var scoper=new CallScoper();
        var injector=Guice.createInjector(new AbstractModule(){protected void configure(){bind(CallScoper.class).toProvider(() -> {resolutions.incrementAndGet();return scoper;});}});
        IGuiceContext.contexts.put("default",context(Optional.of(injector),boots));
        assertEquals("ready",Uni.createFrom().item("ready").await().indefinitely());
        assertTrue(resolutions.get()>0);assertEquals(0,boots.get());
    }
    @Test void activeScopeSnapshotCrossesContextsAndIsRemovedAfterCompletion() throws Exception {
        var scoper=new CallScoper();var boots=new AtomicInteger();
        var injector=Guice.createInjector(new AbstractModule(){protected void configure(){bind(CallScoper.class).toInstance(scoper);}});
        IGuiceContext.contexts.put("default",context(Optional.of(injector),boots));
        var vertx=io.vertx.core.Vertx.vertx();
        try {
            var captured=new java.util.concurrent.CompletableFuture<Uni<String>>();
            vertx.getOrCreateContext().runOnContext(ignored -> {
                try {
                    scoper.enterQuietly();scoper.seed(String.class,"original-actor");
                    var uni=Uni.createFrom().item("read").map(value -> (String)scoper.getValues().get(Key.get(String.class)));
                    scoper.exitQuietly();captured.complete(uni);
                }catch(Throwable failed){captured.completeExceptionally(failed);}
            });
            var uni=captured.get(5,java.util.concurrent.TimeUnit.SECONDS);var result=new java.util.concurrent.CompletableFuture<String>();
            var target=vertx.getOrCreateContext();target.runOnContext(ignored -> uni.subscribe().with(result::complete,result::completeExceptionally));
            assertEquals("original-actor",result.get(5,java.util.concurrent.TimeUnit.SECONDS));
            var empty=new java.util.concurrent.CompletableFuture<Boolean>();target.runOnContext(ignored -> empty.complete(!scoper.isStartedScope()));
            assertTrue(empty.get(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,boots.get());
        } finally {vertx.close().toCompletionStage().toCompletableFuture().get(5,java.util.concurrent.TimeUnit.SECONDS);}
    }

    @Test void cancellationRunsOnSubscriptionContextAndPreservesUnrelatedScope() throws Exception {
        var scoper=new CallScoper();var boots=new AtomicInteger();
        var injector=Guice.createInjector(new AbstractModule(){protected void configure(){bind(CallScoper.class).toInstance(scoper);}});
        IGuiceContext.contexts.put("default",context(Optional.of(injector),boots));
        var vertx=io.vertx.core.Vertx.vertx();
        try {
            for(boolean existing:new boolean[]{false,true}) {
                for(int source=0;source<4;source++) {
                    var owner=vertx.getOrCreateContext();var foreign=vertx.getOrCreateContext();
                    assertNotSame(owner,foreign);
                    var finished=new java.util.concurrent.CompletableFuture<Void>();
                    var cancellations=new AtomicInteger();
                    var handle=new java.util.concurrent.atomic.AtomicReference<io.smallrye.mutiny.subscription.Cancellable>();
                    runInContext(owner,() -> {
                        assertFalse(scoper.isStartedScope());
                        if(existing)scoper.enterQuietly();
                        var caller=existing?scoper.getValues():null;
                        handle.set(Uni.createFrom().deferred(() -> {
                            scoper.seed(String.class,"subscription-owner");
                            return Uni.createFrom().emitter(emitter -> emitter.onTermination(() -> {
                                try {
                                    assertSame(owner,io.vertx.core.Vertx.currentContext());
                                    assertEquals("subscription-owner",scoper.getValues().get(Key.get(String.class)));
                                    assertEquals(1,cancellations.incrementAndGet());
                                    owner.runOnContext(ignored -> {
                                        try {
                                            assertEquals(existing,scoper.isStartedScope());
                                            if(existing)assertSame(caller,scoper.getValues());
                                            finished.complete(null);
                                        } catch(Throwable failure) {finished.completeExceptionally(failure);}
                                    });
                                } catch(Throwable failure) {finished.completeExceptionally(failure);}
                            }));
                        }).subscribe().with(value -> finished.completeExceptionally(new AssertionError("Unexpected item")),
                                finished::completeExceptionally));
                    });
                    Runnable cancel=() -> {handle.get().cancel();handle.get().cancel();};
                    if(source==0)runInContext(owner,cancel);
                    else if(source==3) {assertNull(io.vertx.core.Vertx.currentContext());cancel.run();}
                    else {
                        boolean scoped=source==2;
                        runInContext(foreign,() -> {
                            assertFalse(scoper.isStartedScope());
                            if(scoped) {scoper.enterQuietly();scoper.seed(String.class,"unrelated-caller");}
                            var previous=scoped?scoper.getValues():null;
                            try {
                                cancel.run();
                                assertEquals(scoped,scoper.isStartedScope());
                                if(scoped) {
                                    assertSame(previous,scoper.getValues());
                                    assertEquals("unrelated-caller",scoper.getValues().get(Key.get(String.class)));
                                }
                            } finally {if(scoped && scoper.isStartedScope())scoper.exitQuietly();}
                        });
                    }
                    finished.get(5,java.util.concurrent.TimeUnit.SECONDS);
                    runInContext(owner,() -> {if(existing)scoper.exitQuietly();assertFalse(scoper.isStartedScope());});
                }
            }
            assertEquals(0,boots.get());
        } finally {vertx.close().toCompletionStage().toCompletableFuture().get(5,java.util.concurrent.TimeUnit.SECONDS);}
    }
    private static void runInContext(io.vertx.core.Context context,Runnable action) throws Exception {
        var done=new java.util.concurrent.CompletableFuture<Void>();
        context.runOnContext(ignored -> {
            try {action.run();done.complete(null);}catch(Throwable failure) {done.completeExceptionally(failure);}
        });
        done.get(5,java.util.concurrent.TimeUnit.SECONDS);
    }

}
