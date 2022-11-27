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
 * Specifies the tables that hold state mapped by the annotated derived
 * entity, ensuring that auto-flush happens correctly and that queries
 * against the derived entity do not return stale data.
 * <p>
 * This annotation may be used in combination with {@link Subselect}, or
 * when an entity maps a database view.
 * 
 * @author Sharath Reddy
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Synchronize {
	/**
	 * Names of tables that hold state mapped by the derived entity.
	 * Updates to these tables must be flushed to the database before
	 * the derived entity is queried.
	 */
	String[] value();
}
