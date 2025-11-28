package com.guicedee.client.services.websocket;

import com.guicedee.client.services.IDefaultService;
import com.guicedee.client.services.IServiceEnablement;

/**
 * Service to load authentication data for web service
 */
@SuppressWarnings("unused")
public interface IWebSocketAuthDataProvider<J extends IWebSocketAuthDataProvider<J>>
		extends IDefaultService<J>, IServiceEnablement<J>
{
	StringBuilder getJavascriptToPopulate();

	String name();
}
