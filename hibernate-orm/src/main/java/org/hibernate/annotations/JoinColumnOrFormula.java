/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.persistence.JoinColumn;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows joins based on column or a formula.  One of {@link #formula()} or {@link #column()} should be
 * specified, but not both.
 *
 * @author Sharath Reddy
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Repeatable(JoinColumnsOrFormulas.class)
public @interface JoinColumnOrFormula {
	/**
	 * The formula to use in joining.
	 */
	JoinFormula formula() default @JoinFormula(value="", referencedColumnName="");

	/**
	 * The column to use in joining.
	 */
	JoinColumn column() default @JoinColumn();
}
