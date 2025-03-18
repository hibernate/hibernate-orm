/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.envers.strategy.spi.AuditStrategy;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used to indicate that a test should be run only for specific audit strategies.
 *
 * @author Chris Cranford
 * @since 6.0
 */
@Retention(RUNTIME)
@Target({ METHOD, TYPE })
public @interface RequiresAuditStrategy {
	/**
	 * The strategies against which to run the test
	 *
	 * @return The strategies
	 */
	Class<? extends AuditStrategy>[] value();

	/**
	 * Comment describing the reason why the audit strategy is required.
	 *
	 * @return The comment
	 */
	String comment() default "";

	/**
	 * The key of a JIRA issue which relates to this restriction.
	 *
	 * @return The jira issue key.
	 */
	String jiraKey() default "";
}
