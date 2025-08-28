/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a test method or class as being expected to fail.
 *
 * @see FailureExpectedExtension
 *
 * @author Steve Ebersole
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable( FailureExpectedGroup.class )

@ExtendWith( FailureExpectedExtension.class )
public @interface FailureExpected {
	/**
	 * Setting used to indicate that FailureExpected tests should be run and
	 * that we should validate they still fail.  Note that in this "validation
	 * mode", a test failure is interpreted as a success which is the main
	 * difference from JUnit's support.
	 */
	String VALIDATE_FAILURE_EXPECTED = "hibernate.test.validatefailureexpected";

	/**
	 * A reason why the failure is expected
	 */
	String reason() default "";

	/**
	 * The key of a JIRA issue which covers this expected failure.
	 * @return The jira issue key
	 */
	String jiraKey() default "";
}
