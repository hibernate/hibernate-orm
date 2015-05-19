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
 * This specifies that a property is part of the natural id of the entity.
 *
 * @author Nicol�s Lichtmaier
 *
 * @see NaturalIdCache
 */
@Target( { METHOD, FIELD } )
@Retention( RUNTIME )
public @interface NaturalId {
	/**
	 * Whether the natural id is mutable (or immutable)?  {@code false} (the default) indicates it is immutable;
	 * {@code true} indicates it is mutable.
	 */
	boolean mutable() default false;
}
