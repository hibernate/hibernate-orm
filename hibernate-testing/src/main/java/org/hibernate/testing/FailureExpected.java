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
 * Annotation used to mark a test as an expected failure.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @deprecated Use JUnit Jupiter and {@link org.hibernate.testing.orm.junit.FailureExpected} instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Deprecated(forRemoval = true)
public @interface FailureExpected {
	String VALIDATE_FAILURE_EXPECTED = "hibernate.test.validatefailureexpected";

	/**
	 * The key of a JIRA issue which covers this expected failure.
	 * @return The jira issue key
	 */
	String jiraKey();

	/**
	 * A message explaining the reason for the expected failure.  Optional.
	 * @return The reason
	 */
	String message() default "";
}
