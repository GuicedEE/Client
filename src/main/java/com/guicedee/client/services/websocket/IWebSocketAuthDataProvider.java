package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;
import com.guicedee.client.services.IServiceEnablement;

/**
 * Supplies authentication data used by WebSocket clients.
 * <p>
 * Purpose: provide client-side authentication bootstrap data.
 * Trigger: invoked during WebSocket setup or connection.
 * Order: ascending {@link #sortOrder()}, default 100.
 * Idempotency: implementations should be safe to invoke repeatedly.
 */
@SuppressWarnings("unused")
public interface IWebSocketAuthDataProvider<J extends IWebSocketAuthDataProvider<J>>
		extends IDefaultService<J>, IServiceEnablement<J>
{
	/**
	 * Returns JavaScript that populates authentication data.
	 *
	 * @return the JavaScript snippet
	 */
	StringBuilder getJavascriptToPopulate();

	/**
	 * Returns the provider name.
	 *
	 * @return the provider name
	 */
	String name();
}
