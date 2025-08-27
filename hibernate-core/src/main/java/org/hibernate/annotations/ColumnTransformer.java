/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies custom SQL expressions used to read and write to the column mapped by
 * the annotated persistent attribute in all generated SQL involving the annotated
 * persistent attribute.
 * <ul>
 * <li>A {@link #write} expression must contain exactly one JDBC-style '?' placeholder.
 * <li>A {@link #read} expression may not contain JDBC-style placeholders.
 * </ul>
 * <p>
 * For example:
 * <pre>
 * &#64;Column(name="credit_card_num")
 * &#64;ColumnTransformer(read="decrypt(credit_card_num)"
 *                    write="encrypt(?)")
 * String creditCardNumber;
 * </pre>
 * <p>
 * A column transformer {@link #write} expression transforms the value of a persistent
 * attribute of an entity as it is being written to the database.
 * <ul>
 * <li>If there is a matching {@link #read} expression to undo the effect of this
 *     transformation, then we're entitled to consider the in-memory state of the Java
 *     entity instance as synchronized with the database after a SQL {@code insert} or
 *     {@code update} is executed.
 * <li>On the other hand, if there's no matching {@link #read} expression, or if the
 *     read expression does not exactly undo the effect of the transformation, the
 *     in-memory state of the Java entity instance should be considered unsynchronized
 *     with the database after every SQL {@code insert} or {@code update} is executed.
 * </ul>
 * <p>
 * In the second scenario, we may ask Hibernate to resynchronize the in-memory state
 * with the database after each {@code insert} or {@code update} by annotating the
 * persistent attribute {@link Generated @Generated(event={INSERT,UPDATE}, writable=true)}.
 * This results in a SQL {@code select} after every {@code insert} or {@code update}.
 *
 * @see ColumnTransformers
 *
 * @author Emmanuel Bernard
 */
@Target({FIELD,METHOD})
@Retention(RUNTIME)
@Repeatable(ColumnTransformers.class)
public @interface ColumnTransformer {
	/**
	 * The name of the mapped column, if a persistent attribute maps to multiple columns.
	 * Optional if a persistent attribute is mapped to a single column
	 */
	String forColumn() default "";

	/**
	 * A custom SQL expression used to read from the column.
	 */
	String read() default "";

	/**
	 * A custom SQL expression used to write to the column. The expression must contain
	 * exactly one JDBC-style '?' placeholder.
	 */
	String write() default "";
}
