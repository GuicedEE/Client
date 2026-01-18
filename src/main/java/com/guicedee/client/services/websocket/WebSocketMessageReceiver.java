package com.guicedee.client.services.websocket;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * DTO for messages received over WebSocket connections.
 *
 * @param <J> the concrete receiver type
 */
@JsonAutoDetect(fieldVisibility = ANY,
	getterVisibility = NONE,
	setterVisibility = NONE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class WebSocketMessageReceiver<J extends WebSocketMessageReceiver<J>>
{
		private String action;
		private String broadcastGroup;
		private String dataService;
		private Map<String, Object> data = new HashMap<>();
		private String webSocketSessionId;
		
		/**
		 * Creates a receiver with an action and payload data.
		 *
		 * @param action the action name
		 * @param data the payload data
		 */
		public WebSocketMessageReceiver(String action, Map<String, Object> data)
		{
				this.action = action;
				this.data = data;
		}
		
		/**
		 * Adds a key/value pair to the payload data map.
		 *
		 * @param key the data key
		 * @param value the data value
		 */
		@JsonAnySetter
		public void add(String key, String value)
		{
				data.put(key, value);
		}
		
		@Override
		public String toString()
		{
				return "WebSocketMessageReceiver{" +
												"action=" + action +
												", broadcastGroup='" + broadcastGroup + '\'' +
												", data=" + data +
												'}';
		}
}
