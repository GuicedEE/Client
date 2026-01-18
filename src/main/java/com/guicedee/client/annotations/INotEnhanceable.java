package com.guicedee.client.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as excluded from AOP enhancement.
 * <p>
 * Entity classes are commonly excluded to keep criteria and reflection behavior predictable.
 */
@Target(
	{
		ElementType.TYPE, ElementType.TYPE_USE
	})
@Retention(RetentionPolicy.RUNTIME)
public @interface INotEnhanceable
{
}
