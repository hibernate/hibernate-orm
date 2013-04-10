/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom SQL expression used to read the value from and write a value to a column.
 * Use for direct object loading/saving as well as queries.
 * The write expression must contain exactly one '?' placeholder for the value. 
 *
 * For example: <code>read="decrypt(credit_card_num)" write="encrypt(?)"</code>
 *
 * @see ColumnTransformers
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({FIELD,METHOD})
@Retention(RUNTIME)
public @interface ColumnTransformer {
	/**
	 * (Logical) column name for which the expression is used.
	 *
	 * This can be left out if the property is bound to a single column
	 */
	String forColumn() default "";

	/**
	 * Custom SQL expression used to read from the column.
	 */
	String read() default "";

	/**
	 * Custom SQL expression used to write to the column. The write expression must contain exactly
	 * one '?' placeholder for the value.
	 */
	String write() default "";
}
