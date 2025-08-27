/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;
import jakarta.persistence.JoinColumn;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a composite join condition involving one or more {@linkplain JoinFormula formulas}
 * and, optionally, one or more {@linkplain JoinColumn columns}. If a join condition has just
 * one column or formula, or involves only columns, this annotation is unnecessary.
 *
 * @author Sharath Reddy
 *
 * @see JoinColumnOrFormula
 * @see jakarta.persistence.JoinColumns
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinColumnsOrFormulas {
	/**
	 * A list of columns and formulas to use in the join condition.
	 */
	JoinColumnOrFormula[] value();
}
