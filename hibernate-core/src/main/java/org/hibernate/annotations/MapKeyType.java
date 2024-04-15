/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows defining the type of the key of a persistent map.
 *
 * @author Steve Ebersole
 *
 * @deprecated 6.0 will introduce a new type-safe {@code CustomType} annotation
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention(RUNTIME)
@Deprecated
public @interface MapKeyType {
	/**
	 * The map key type definition.
	 */
	Type value();
}
