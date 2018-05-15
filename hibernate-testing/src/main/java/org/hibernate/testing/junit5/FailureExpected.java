/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

import static org.hibernate.testing.junit5.StandardTags.FAILURE_EXPECTED;

/**
 * Marks a test method or class as being expected to fail.
 *
 * This works very similar to JUnit's own {@link org.junit.jupiter.api.Disabled}
 * annotation.  Normally this annotation indicates that the test
 * should be skipped/ignored/disabled.  However, it is possible to still
 * run these tests by deactivating the condition that checks this
 * ({@link FailureExpectedExtension} which works the same as deactivating
 * JUnit's `@Disabled` - see http://junit.org/junit5/docs/current/user-guide/#extensions-conditions-deactivation
 *
 * @see FailureExpectedExtension
 *
 * @author Steve Ebersole
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Tag( FAILURE_EXPECTED )
public @interface FailureExpected {
	String VALIDATE_FAILURE_EXPECTED = "hibernate.test.validatefailureexpected";

	/**
	 * A reason why the failure is expected
	 */
	String value();
}
