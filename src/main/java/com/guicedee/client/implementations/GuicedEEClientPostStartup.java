package com.guicedee.client.implementations;

import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import com.guicedee.client.services.websocket.IGuicedWebSocket;
import io.vertx.core.Future;

import java.util.List;

/**
 * Post-startup hook that loads WebSocket receivers.
 * <p>
 * Purpose: initialize WebSocket listener registration after injector creation.
 * Trigger: executed during {@link com.guicedee.client.services.lifecycle.IGuicePostStartup#postLoad()}.
 * Order: {@link #sortOrder()} ensures it runs early in post-startup.
 * Idempotency: safe to call repeatedly; registration checks avoid duplicates.
 */
public class GuicedEEClientPostStartup implements IGuicePostStartup<GuicedEEClientPostStartup>
{
		
		/**
		 * Loads and registers WebSocket message receivers.
		 *
		 * @return a completed future indicating success
		 */
		@Override
		public List<Future<Boolean>> postLoad()
		{
				IGuicedWebSocket.loadWebSocketReceivers();
				return List.of(Future.succeededFuture(true));
		}
		
		/**
		 * Ensures this hook runs near the beginning of post-startup.
		 *
		 * @return the sort order
		 */
		@Override
		public Integer sortOrder()
		{
				return Integer.MIN_VALUE + 650;
		}
}
