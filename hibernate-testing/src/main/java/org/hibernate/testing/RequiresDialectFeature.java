/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicate that a test should be run only when the current dialect supports the
 * specified feature.
 *
 * @author Hardy Ferentschik
 * @deprecated Use JUnit Jupiter and {@link org.hibernate.testing.orm.junit.RequiresDialectFeature} instead.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Deprecated(forRemoval = true)
public @interface RequiresDialectFeature {
	/**
	 * @return Class which checks the necessary dialect feature
	 */
	Class<? extends DialectCheck>[] value();

	/**
	 * Comment describing the reason why the feature is required.
	 *
	 * @return The comment
	 */
	String comment() default "";

	/**
	 * The key of a JIRA issue which relates this this feature requirement.
	 *
	 * @return The jira issue key
	 */
	String jiraKey() default "";
}
