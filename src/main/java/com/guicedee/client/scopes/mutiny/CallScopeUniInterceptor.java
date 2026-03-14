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

/**
 * Mutiny {@link UniInterceptor} that propagates {@link com.guicedee.client.scopes.CallScope} state.
 * <p>
 * Captures the current call scope on Uni creation and restores it on subscription.
 */
public class CallScopeUniInterceptor
		implements UniInterceptor
{
		/**
		 * Creates a new call scope Uni interceptor.
		 */
		public CallScopeUniInterceptor() {
		}

		private static final Key<CallScopeProperties> CALL_SCOPE_PROPERTIES_KEY = Key.get(CallScopeProperties.class);

		/**
		 * Wraps newly created {@link Uni} instances to carry call scope values across async boundaries.
		 *
		 * @param uni the created Uni
		 * @param <T> the item type
		 * @return a Uni with call scope propagation
		 */
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
				INTERCEPTING.remove();
			}
		}

		private static final ThreadLocal<Boolean> INTERCEPTING = ThreadLocal.withInitial(() -> false);

		/**
		 * Captures the scoped values from the active call scope.
		 *
		 * @param callScoper the call scoper to read from
		 * @return a snapshot of scoped values, or an empty map
		 */
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

		/**
		 * Uni wrapper that restores call scope values at subscription time.
		 */
		private static final class CallScopeAwareUni<T> extends AbstractUni<T>
		{
				private final Uni<T> upstream;
				private final Map<Key<?>, Object> capturedValues;

				private CallScopeAwareUni(Uni<T> upstream, Map<Key<?>, Object> capturedValues)
				{
						this.upstream = upstream;
						this.capturedValues = capturedValues.isEmpty() ? Collections.emptyMap() : capturedValues;
				}

				/**
				 * Subscribes while ensuring call scope values are available for the subscription.
				 *
				 * @param subscriber the downstream subscriber
				 */
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
								INTERCEPTING.remove();
						}
				}
		}

		/**
		 * Subscriber wrapper that ends the call scope after completion or failure.
		 */
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

				/**
				 * Wraps the subscription to close the scope on cancel.
				 *
				 * @param subscription the upstream subscription
				 */
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

				/**
				 * Delegates the item and then closes the scope if it was started locally.
				 *
				 * @param item the item produced
				 */
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

				/**
				 * Delegates the failure and then closes the scope if it was started locally.
				 *
				 * @param failure the failure produced
				 */
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

				/**
				 * Ensures scope exit occurs at most once for this subscription.
				 */
				private void endScope()
				{
						if (startedScope && ended.compareAndSet(false, true))
						{
								safeExit(callScoper);
						}
				}
		}

		/**
		 * Subscription wrapper that invokes a callback on cancel.
		 */
		private static final class ScopedUniSubscription implements UniSubscription
		{
				private final UniSubscription delegate;
				private final Runnable onCancel;

				private ScopedUniSubscription(UniSubscription delegate, Runnable onCancel)
				{
						this.delegate = delegate;
						this.onCancel = onCancel;
				}

				/**
				 * Delegates demand to the upstream subscription.
				 *
				 * @param n the requested demand
				 */
				@Override
				public void request(long n)
				{
						delegate.request(n);
				}

				/**
				 * Cancels the upstream subscription and triggers the scope cleanup callback.
				 */
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

		/**
		 * Attempts to exit the call scope while ignoring state errors.
		 *
		 * @param callScoper the scoper to exit
		 */
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

		/**
		 * Records a touch into {@link CallScopeProperties} when available.
		 *
		 * @param callScoper the scoper containing the properties
		 * @param reason the reason identifier
		 * @param location the origin marker
		 */
		private static void recordTouch(CallScoper callScoper, String reason, String location)
		{
				CallScopeProperties properties = getCallScopeProperties(callScoper);
				if (properties != null)
				{
						properties.getTouches().add(reason + "@" + location);
				}
		}

		/**
		 * Resolves the call scope properties from the current scoped values.
		 *
		 * @param callScoper the scoper to inspect
		 * @return the properties or null if unavailable
		 */
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

		/**
		 * Captures a lightweight diagnostic location marker.
		 *
		 * @return a constant location marker
		 */
		private static String captureLocation()
		{
			// Always return something simple to avoid StackWalker overhead and depth issues
			return "omitted";
		}
}
