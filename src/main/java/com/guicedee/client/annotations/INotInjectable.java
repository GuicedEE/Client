package com.guicedee.client.annotations;

import java.lang.annotation.*;

/**
 * Marks a type as not eligible for member injection when resolved via {@link com.guicedee.client.IGuiceContext}.
 */

@Target(
	{
		ElementType.TYPE, ElementType.TYPE_USE
	})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface INotInjectable
{
}
