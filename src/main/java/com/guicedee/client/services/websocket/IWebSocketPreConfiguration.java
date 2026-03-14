package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;
import com.guicedee.client.services.IServiceEnablement;

/**
 * Pre-configuration hook for WebSocket server setup.
 * <p>
 * Purpose: configure server-level WebSocket settings before runtime.
 * Trigger: invoked during WebSocket initialization.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should be safe to invoke once and tolerate repeats.
 *
 * @param <J> the implementing configuration type
 */
public interface IWebSocketPreConfiguration<J extends IWebSocketPreConfiguration<J>>
		extends IDefaultService<J>, IServiceEnablement<J>
{
	/**
	 * Executes the WebSocket configuration logic.
	 */
	void configure();
}
