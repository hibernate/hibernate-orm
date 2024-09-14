/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks the canonical constructor to be used for instantiation of an embeddable.
 * This will implicitly add a special {@link EmbeddableInstantiator}.
 *
 * @since 6.2
 */
@Target({ CONSTRUCTOR })
@Retention(RUNTIME)
@Incubating
public @interface Instantiator {
	/**
	 * The persistent attribute names the constructor parameters at the respective index assigns the value to.
	 */
	String[] value();
}
