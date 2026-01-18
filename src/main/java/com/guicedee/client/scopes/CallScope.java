package com.guicedee.client.scopes;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Guice scope annotation for per-call scoped objects.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@ScopeAnnotation
public @interface CallScope {
}
