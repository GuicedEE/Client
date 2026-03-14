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

/**
 * Mutable properties carried across a {@link CallScope}.
 * <p>
 * Used to record scope provenance, additional metadata, and diagnostic touches.
 */
@CallScope
@Getter
@Setter
@Accessors(chain = true)
public class CallScopeProperties implements Serializable
{
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Creates new call scope properties with default values.
	 */
	public CallScopeProperties() {
	}

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
