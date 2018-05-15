/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.dynamictests;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A {@link org.junit.jupiter.api.Test} equivalent.
 *
 * A marker annotation that identifies methods that should be collected and executed like a normal test
 * but under the control of of our custom test factory implementation.
 *
 * A test which uses this annotation should extend {@link AbstractDynamicTest} for these marker methods
 * to be injected into the Jupiter test framework via a {@link org.junit.jupiter.api.TestFactory}.
 *
 * @author Chris Cranford
 */
@Target( METHOD )
@Retention( RUNTIME )
public @interface DynamicTest {
	/**
	 * Default empty exception.
	 */
	class None extends Throwable {
		private None() {
		}
	}

	/**
	 * An expected {@link Throwable} to cause a test method to succeed, but only if an exception
	 * of the <code>expected</code> type is thrown.
	 */
	Class<? extends Throwable> expected() default None.class;
}
