/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.dialect.Dialect;

/**
 * Annotation used to indicate that a test should be run only when run against the
 * indicated dialects.
 *
 * @see RequiresDialects
 *
 * @author Hardy Ferentschik
 * @deprecated Use JUnit 5 and {@link org.hibernate.testing.orm.junit.RequiresDialect} instead.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RequiresDialects.class)
@Deprecated(forRemoval = true)
public @interface RequiresDialect {
	/**
	 * The dialects against which to run the test
	 *
	 * @return The dialects
	 */
	Class<? extends Dialect> value();

	/**
	 * Used to indicate if the dialects should be matched strictly (classes equal) or
	 * non-strictly (instanceof).
	 *
	 * @return Should strict matching be used?
	 */
	boolean strictMatching() default false;

	/**
	 * Comment describing the reason why the dialect is required.
	 *
	 * @return The comment
	 */
	String comment() default "";

	/**
	 * The key of a JIRA issue which relates this this restriction
	 *
	 * @return The jira issue key
	 */
	String jiraKey() default "";
}
