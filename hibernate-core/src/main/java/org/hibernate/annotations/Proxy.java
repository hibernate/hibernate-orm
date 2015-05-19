/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Lazy and proxy configuration of a particular class.
 *
 * @author Emmanuel Bernard
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Proxy {
	/**
	 * Whether this class is lazy or not.  Default to true.
	 */
	boolean lazy() default true;

	/**
	 * Proxy class or interface used.  Default is to use the entity class name.
	 */
	Class proxyClass() default void.class;
}
