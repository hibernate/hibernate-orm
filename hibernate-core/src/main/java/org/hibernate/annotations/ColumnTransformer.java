/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies custom SQL expressions used to read and write to a column in all generated SQL
 * involving the annotated persistent attribute.
 * <ul>
 * <li>A <code>write</code> expression must contain exactly one JDBC-style '?' placeholder.
 * <li>A <code>read</code> expression may not contain JDBC-style placeholders.
 * </ul>
 * For example:
 * <pre>
 * {@code @Column(name="credit_card_num")
 * @ColumnTransformer(read="decrypt(credit_card_num)"
 *                    write="encrypt(?)")
 * String creditCardNumber;}
 * </pre>
 *
 * @see ColumnTransformers
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target({FIELD,METHOD})
@Retention(RUNTIME)
@Repeatable(ColumnTransformers.class)
public @interface ColumnTransformer {
	/**
	 * The name of the mapped column, if a persistent attribute maps to multiple columns.
	 * Optional if a persistent attribute is mapped to a single column
	 */
	String forColumn() default "";

	/**
	 * Custom SQL expression used to read from the column.
	 */
	String read() default "";

	/**
	 * Custom SQL expression used to write to the column. The expression must contain exactly
	 * one JDBC-style '?' placeholder.
	 */
	String write() default "";
}
