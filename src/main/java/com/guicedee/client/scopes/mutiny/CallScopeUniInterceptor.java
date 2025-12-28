package com.guicedee.client.scopes.mutiny;

import com.google.inject.Key;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScoper;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.UniInterceptor;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CallScopeUniInterceptor
		implements UniInterceptor
{
		private static final Key<CallScopeProperties> CALL_SCOPE_PROPERTIES_KEY = Key.get(CallScopeProperties.class);

		@Override
		public <T> Uni<T> onUniCreation(Uni<T> uni)
		{
			// Avoid re-wrapping if this interceptor already produced the Uni instance
			if (uni instanceof CallScopeAwareUni || INTERCEPTING.get())
			{
				return uni;
			}
			INTERCEPTING.set(true);
			try
			{
				CallScoper callScoper = IGuiceContext.get(CallScoper.class);
				if (callScoper.isStartedScope())
				{
					recordTouch(callScoper, "uni-creation", captureLocation());
				}
				Map<Key<?>, Object> snapshot = captureCallScope(callScoper);
				return new CallScopeAwareUni<>(uni, snapshot);
			}
			finally
			{
				INTERCEPTING.set(false);
			}
		}

		private static final ThreadLocal<Boolean> INTERCEPTING = ThreadLocal.withInitial(() -> false);

		private Map<Key<?>, Object> captureCallScope(CallScoper callScoper)
		{
				if (!callScoper.isStartedScope())
				{
						return Collections.emptyMap();
				}
				Map<Key<?>, Object> values = callScoper.getValues();
				if (values == null || values.isEmpty())
				{
						return Collections.emptyMap();
				}
				return new HashMap<>(values);
		}

		private static final class CallScopeAwareUni<T> extends AbstractUni<T>
		{
				private final Uni<T> upstream;
				private final Map<Key<?>, Object> capturedValues;

				private CallScopeAwareUni(Uni<T> upstream, Map<Key<?>, Object> capturedValues)
				{
						this.upstream = upstream;
						this.capturedValues = capturedValues.isEmpty() ? Collections.emptyMap() : capturedValues;
				}

				@Override
				public void subscribe(UniSubscriber<? super T> subscriber)
				{
						if (INTERCEPTING.get())
						{
								AbstractUni.subscribe(upstream, subscriber);
								return;
						}
						INTERCEPTING.set(true);
						try
						{
								CallScoper callScoper = IGuiceContext.get(CallScoper.class);
								boolean startedHere = false;

								if (!callScoper.isStartedScope())
								{
										callScoper.enter();
										startedHere = true;
										if (!capturedValues.isEmpty())
										{
												callScoper.setValues(capturedValues);
										}
										recordTouch(callScoper, "uni-bounce", captureLocation());
								}

								ScopedUniSubscriber<T> scopedSubscriber = new ScopedUniSubscriber<>(subscriber, callScoper, startedHere);
								try
								{
										recordTouch(callScoper, startedHere ? "uni-bounce" : "uni-subscribe", captureLocation());
										AbstractUni.subscribe(upstream, scopedSubscriber);
								}
								catch (Throwable t)
								{
										if (startedHere)
										{
												safeExit(callScoper);
										}
										throw t;
								}
						}
						finally
						{
								INTERCEPTING.set(false);
						}
				}
		}

		private static final class ScopedUniSubscriber<T> implements UniSubscriber<T>
		{
				private final UniSubscriber<? super T> delegate;
				private final CallScoper callScoper;
				private final boolean startedScope;
				private final AtomicBoolean ended = new AtomicBoolean(false);

				private ScopedUniSubscriber(UniSubscriber<? super T> delegate, CallScoper callScoper, boolean startedScope)
				{
						this.delegate = delegate;
						this.callScoper = callScoper;
						this.startedScope = startedScope;
				}

				@Override
				public void onSubscribe(UniSubscription subscription)
				{
						UniSubscription wrapped = startedScope ? new ScopedUniSubscription(subscription, this::endScope) : subscription;
						try
						{
								delegate.onSubscribe(wrapped);
						}
						catch (Throwable t)
						{
								endScope();
								throw t;
						}
				}

				@Override
				public void onItem(T item)
				{
						try
						{
								delegate.onItem(item);
						}
						finally
						{
								endScope();
						}
				}

				@Override
				public void onFailure(Throwable failure)
				{
						try
						{
								delegate.onFailure(failure);
						}
						finally
						{
								endScope();
						}
				}

				private void endScope()
				{
						if (startedScope && ended.compareAndSet(false, true))
						{
								safeExit(callScoper);
						}
				}
		}

		private static final class ScopedUniSubscription implements UniSubscription
		{
				private final UniSubscription delegate;
				private final Runnable onCancel;

				private ScopedUniSubscription(UniSubscription delegate, Runnable onCancel)
				{
						this.delegate = delegate;
						this.onCancel = onCancel;
				}

				@Override
				public void request(long n)
				{
						delegate.request(n);
				}

				@Override
				public void cancel()
				{
						try
						{
								delegate.cancel();
						}
						finally
						{
								onCancel.run();
						}
				}
		}

		private static void safeExit(CallScoper callScoper)
		{
				try
				{
						callScoper.exit();
				}
				catch (IllegalStateException ignored)
				{
						// Scope already exited
				}
		}

		private static void recordTouch(CallScoper callScoper, String reason, String location)
		{
				CallScopeProperties properties = getCallScopeProperties(callScoper);
				if (properties != null)
				{
						properties.getTouches().add(reason + "@" + location);
				}
		}

		private static CallScopeProperties getCallScopeProperties(CallScoper callScoper)
		{
				Map<Key<?>, Object> values = callScoper.getValues();
				if (values == null)
				{
						return null;
				}
				Object properties = values.get(CALL_SCOPE_PROPERTIES_KEY);
				if (properties instanceof CallScopeProperties)
				{
						return (CallScopeProperties) properties;
				}
				return null;
		}

		private static String captureLocation()
		{
			// Always return something simple to avoid StackWalker overhead and depth issues
			return "omitted";
		}
}
