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
 * Maps an immutable and read-only entity to a given SQL {@code select} expression.
 * <p>
 * This is an alternative to defining a database view and mapping the entity to
 * the view using the {@link jakarta.persistence.Table @Table} annotation.
 *
 * @see Synchronize
 *
 * @author Sharath Reddy
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Subselect {
	/**
	 * The query.
	 */
	String value();
}
