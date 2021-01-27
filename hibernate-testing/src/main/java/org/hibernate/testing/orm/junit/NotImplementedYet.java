/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.NotImplementedYetException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hibernate.testing.junit5.StandardTags.NOT_IMPLEMENTED_YET;

/**
 * Marks a test method or class's tested functionality as not being implemented yet.
 *
 * @see NotImplementedYetExtension
 *
 * @author Jan Schatteman
 */
@Inherited
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)

@Tag(NOT_IMPLEMENTED_YET)

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

	/**
	 * Generally this handles tests failing due to {@link NotImplementedYetException} exceptions
	 * being thrown (strict).  Setting this to false allows it to handle failure for any reason.
	 */
	boolean strict() default true;
}
