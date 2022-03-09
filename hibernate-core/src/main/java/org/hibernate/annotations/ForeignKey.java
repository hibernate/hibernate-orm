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
 * Specifies a foreign key name.
 *
 * @deprecated use the JPA 2.1 {@link jakarta.persistence.ForeignKey} annotation
 */
@Target({FIELD, METHOD, TYPE})
@Retention(RUNTIME)
@Deprecated( forRemoval = true )
@Remove( )
public @interface ForeignKey {
	/**
	 * Name of the foreign key of a {@code OneToMany}, {@code ManyToOne}, or
	 * {@code OneToOne} association. May also be applied to the owning side a
	 * {@code ManyToMany} association.
	 */
	String name();

	/**
	 * Used for the non-owning side of a {@code ManyToMany} association.
	 * Ignored for other association cardinalities.
	 */
	String inverseName() default "";
}
