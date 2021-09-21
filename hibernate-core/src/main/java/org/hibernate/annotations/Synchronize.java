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
 * Ensures that auto-flush happens correctly and that queries against the derived 
 * entity do not return stale data.
 * 
 * Mostly used with {@link Subselect}.
 * 
 * @author Sharath Reddy
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Synchronize {
	/**
	 * Table names.
	 */
	String[] value();
}
