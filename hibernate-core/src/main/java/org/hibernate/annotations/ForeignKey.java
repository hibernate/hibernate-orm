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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Define the foreign key name.
 *
 * @deprecated Prefer the JPA 2.1 introduced {@link javax.persistence.ForeignKey} instead.
 */
@Target({FIELD, METHOD, TYPE})
@Retention(RUNTIME)
@Deprecated
public @interface ForeignKey {
	/**
	 * Name of the foreign key.  Used in OneToMany, ManyToOne, and OneToOne
	 * relationships.  Used for the owning side in ManyToMany relationships
	 */
	String name();

	/**
	 * Used for the non-owning side of a ManyToMany relationship.  Ignored
	 * in other relationships
	 */
	String inverseName() default "";
}
