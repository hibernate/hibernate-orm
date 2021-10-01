/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import org.hibernate.usertype.UserType;

/**
 * Form of {@link CustomType} for use with map-keys
 *
 * @since 6.0
 */
public @interface MapKeyCustomType {
	/**
	 * The custom type implementor class
	 *
	 * @see CustomType#value
	 */
	Class<? extends UserType<?>> value();

	/**
	 * Parameters for the custom type
	 *
	 * @see CustomType#parameters
	 */
	Parameter[] parameters() default {};
}
