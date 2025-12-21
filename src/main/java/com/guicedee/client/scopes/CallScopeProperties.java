package com.guicedee.client.scopes;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CallScope
@Getter
@Setter
@Accessors(chain = true)
public class CallScopeProperties implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;
	/**
	 * The source of the call scope entry
	 */
	private CallScopeSource source = CallScopeSource.Unknown;
	/**
	 * Any properties to carry within the call scope
	 */
	private Map<Object, Object> properties = new HashMap<>();

	/**
	 * Ordered list of locations where this scope was touched (creation, bounces, Uni starts).
	 */
	private List<String> touches = new ArrayList<>();

}
