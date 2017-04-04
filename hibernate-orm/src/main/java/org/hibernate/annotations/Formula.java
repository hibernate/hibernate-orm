/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a formula (derived value) which is a SQL fragment that acts as a @Column alternative in most cases.
 * Represents read-only state.
 *
 * In certain cases @ColumnTransformer might be a better option, especially as it leaves open the option of still
 * being writable.
 *
 * <blockquote><pre>
 *     // perform calculations
 *     &#064;Formula( "sub_total + (sub_total * tax)" )
 *     long getTotalCost() { ... }
 * </pre></blockquote>
 *
 * <blockquote><pre>
 *     // call database functions ( e.g. MySQL upper() and substring() )
 *     &#064;Formula( "upper( substring( middle_name, 1 ) )" )
 *     Character getMiddleInitial() { ... }
 * </pre></blockquote>
 *
 * <blockquote><pre>
 *     // this might be better handled through @ColumnTransformer
 *     &#064;Formula( "decrypt(credit_card_num)" )
 *     String getCreditCardNumber() { ... }
 * </pre></blockquote>
 *
 * @see ColumnTransformer
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Formula {
	/**
	 * The formula string.
	 */
	String value();
}
