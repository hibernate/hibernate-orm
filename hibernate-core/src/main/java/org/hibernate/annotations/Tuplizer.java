/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Define a tuplizer for an entity or a component.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated since {@link org.hibernate.tuple.Tuplizer} is deprecated
 */
@java.lang.annotation.Target( {TYPE, FIELD, METHOD} )
@Retention( RUNTIME )
@Repeatable(Tuplizers.class)
@Deprecated
public @interface Tuplizer {
	/**
	 * Tuplizer implementation.
	 */
	Class<? extends org.hibernate.tuple.Tuplizer> impl();
}
