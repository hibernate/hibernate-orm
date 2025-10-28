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
 * Specifies an expression written in native SQL that is used to read the value of
 * an attribute instead of storing the value in a {@link jakarta.persistence.Column}.
 * <p>
 * A {@code Formula} mapping defines a "derived" attribute, whose state is determined
 * from other columns and functions when an entity is read from the database.
 * <p>
 * A formula may involve multiple columns and SQL operators:
 * <pre>
 * // perform calculations using SQL operators
 * &#64;Formula("sub_total * (1.0 + tax)")
 * BigDecimal totalWithTax;
 * </pre>
 * <p>
 * It may even call SQL functions:
 * <pre>
 * // call native SQL functions
 * &#64;Formula("upper(substring(middle_name from 0 for 1))")
 * Character middleInitial;
 * </pre>
 * <p>
 * By default, the fields of an entity are not updated with the results of evaluating
 * the formula after an {@code insert} or {@code update}. The {@link Generated @Generated}
 * annotation may be used to specify that this should happen:
 * <pre>
 * &#64;Generated  // evaluate the formula after an insert
 * &#64;Formula("sub_total * (1.0 + tax)")
 * BigDecimal totalWithTax;
 * </pre>
 * <p>
 * The placeholder {@code {alias}} is resolved to the alias of the entity:
 * <pre>
 * &#64;Formula("balance/(select sum(a.balance) from customer a where a.gender={alias}.gender)")
 * private BigDecimal percentage;
 * </pre>
 * <p>
 * For an entity with {@linkplain jakarta.persistence.SecondaryTable secondary tables},
 * a formula may involve columns of the primary table, or columns of any one of the
 * secondary tables. But it may not involve columns of more than one table.
 * <p>
 * The {@link ColumnTransformer @ColumnTransformer} annotation is an alternative in
 * certain cases, allowing the use of native SQL to read <em>and write</em> values to
 * a column.
 * <pre>
 * // it might be better to use &#64;ColumnTransformer in this case
 * &#064;Formula("decrypt(credit_card_num)")
 * String getCreditCardNumber() { ... }
 * </pre>
 *
 * @see Generated
 * @see ColumnTransformer
 * @see DiscriminatorFormula
 * @see JoinFormula
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @see DialectOverride.Formula
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Formula {
	/**
	 * The formula, written in native SQL.
	 */
	String value();
}
