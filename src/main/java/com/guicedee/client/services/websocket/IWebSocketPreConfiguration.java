package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;
import com.guicedee.client.services.IServiceEnablement;

/**
 * A service for JWebMPWebSockets to configure app servers
 */
public interface IWebSocketPreConfiguration<J extends IWebSocketPreConfiguration<J>>
		extends IDefaultService<J>, IServiceEnablement<J>
{
	void configure();
}
