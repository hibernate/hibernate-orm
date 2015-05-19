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
 * To be used as a replacement for {@code @JoinColumn} in most places.  The formula has to be a valid
 * SQL fragment
 *
 * @author Sharath Reddy
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinFormula {
	/**
	 * The formula.
	 */
	String value();

	/**
	 * The column this formula references.
	 */
	String referencedColumnName() default "";
}
