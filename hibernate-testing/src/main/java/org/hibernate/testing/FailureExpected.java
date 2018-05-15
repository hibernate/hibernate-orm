/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 *
 * @deprecated Use {@link org.hibernate.testing.junit5.FailureExpected} instead
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Deprecated
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
