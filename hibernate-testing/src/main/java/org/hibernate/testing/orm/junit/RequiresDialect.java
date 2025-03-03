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

import org.hibernate.dialect.Dialect;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Indicates that the annotated test class/method should only
 * be run when the indicated Dialect is being used.
 *
 * @author Steve Ebersole
 */
@Inherited
@Retention( RetentionPolicy.RUNTIME )
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable( RequiresDialects.class )

@ExtendWith( DialectFilterExtension.class )
public @interface RequiresDialect {
	/**
	 * The Dialect class to match.
	 */
	Class<? extends Dialect> value();

	/**
	 * Should subtypes of {@link #value()} be matched?
	 */
	boolean matchSubTypes() default true;

	int majorVersion() default -1;

	int minorVersion() default -1;

	int microVersion() default -1;

	/**
	 * Comment describing the reason why the dialect is required.
	 *
	 * @return The comment
	 */
	String comment() default "";
}
