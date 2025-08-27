/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.persistence.JoinColumn;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies one element of a {@linkplain JoinColumnsOrFormulas composite join condition}
 * involving both {@linkplain JoinFormula formulas} and {@linkplain JoinColumn columns}.
 * One of {@link #formula()} or {@link #column()} must be specified, but not both. If a
 * composite join condition involves only columns, this annotation is unnecessary.
 *
 * @see JoinColumnsOrFormulas
 * @see JoinFormula
 * @see JoinColumn
 *
 * @author Sharath Reddy
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(JoinColumnsOrFormulas.class)
public @interface JoinColumnOrFormula {
	/**
	 * The formula to use in the join condition.
	 */
	JoinFormula formula() default @JoinFormula(value="", referencedColumnName="");

	/**
	 * The column to use in the join condition.
	 */
	JoinColumn column() default @JoinColumn();
}
