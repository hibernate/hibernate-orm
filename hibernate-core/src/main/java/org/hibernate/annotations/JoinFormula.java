/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a join condition based on an arbitrary native SQL formula
 * instead of a {@linkplain jakarta.persistence.JoinColumn column name}.
 *
 * @author Sharath Reddy
 *
 * @see DialectOverride.JoinFormula
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinFormula {
	/**
	 * The formula, in native SQL.
	 */
	String value();

	/**
	 * The column this formula references.
	 */
	String referencedColumnName() default "";
}
