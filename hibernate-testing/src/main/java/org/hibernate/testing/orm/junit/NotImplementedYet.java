/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Indicates that the test tests functionality that has not been implemented yet.
 *
 * @see NotImplementedYetExtension
 *
 * @author Jan Schatteman
 */
@Inherited
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith( NotImplementedYetExtension.class )
public @interface NotImplementedYet {

	/**
	 * A reason why the failure is expected
	 */
	String reason() default "";

	/**
	 * A version expectation by when this feature is supposed to become implemented
	 */
	String expectedVersion() default "";
}
