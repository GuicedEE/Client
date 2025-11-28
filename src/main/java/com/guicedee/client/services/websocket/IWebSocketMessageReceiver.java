package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;

import java.util.Set;

/**
 * Registers receivers for web socket messages
 */
public interface IWebSocketMessageReceiver<R,J extends IWebSocketMessageReceiver<R,J>>
		extends IDefaultService<J>
{
	/**
	 * Returns a unique list of names that this applies for
	 *
	 * @return
	 */
	Set<String> messageNames();

	/**
	 * Receives a message on the web socket to a specific designated name registered on GuicedWebSocket
	 *
	 * @param message
	 * 		The message if required
	 *
	 * @throws java.lang.SecurityException
	 * 		if any consumer decides the connection is not valid
	 */
	io.smallrye.mutiny.Uni<R> receiveMessage(WebSocketMessageReceiver<?> message) throws SecurityException;
}
