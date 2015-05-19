/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
