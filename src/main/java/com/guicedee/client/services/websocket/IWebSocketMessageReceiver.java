package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;

import java.util.Set;

/**
 * Registers receivers for WebSocket messages.
 */
public interface IWebSocketMessageReceiver<R,J extends IWebSocketMessageReceiver<R,J>>
		extends IDefaultService<J>
{
	/**
	 * Returns the message names handled by this receiver.
	 *
	 * @return the set of message names
	 */
	Set<String> messageNames();

	/**
	 * Receives a WebSocket message routed by {@link IGuicedWebSocket}.
	 *
	 * @param message the incoming message payload
	 * @return the response payload as a Uni
	 * @throws SecurityException if the connection is not valid
	 */
	io.smallrye.mutiny.Uni<R> receiveMessage(WebSocketMessageReceiver<?> message) throws SecurityException;
}
