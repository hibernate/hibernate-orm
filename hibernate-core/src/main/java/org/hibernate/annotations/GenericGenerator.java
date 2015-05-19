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
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Generator annotation describing any kind of Hibernate generator in a generic (de-typed) manner.
 *
 * @author Emmanuel Bernard
 */
@Target({PACKAGE, TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface GenericGenerator {
	/**
	 * unique generator name.
	 */
	String name();
	/**
	 * Generator strategy either a predefined Hibernate strategy or a fully qualified class name.
	 */
	String strategy();
	/**
	 * Optional generator parameters.
	 */
	Parameter[] parameters() default {};
}
