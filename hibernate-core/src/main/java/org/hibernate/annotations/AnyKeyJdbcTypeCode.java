/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JdbcTypeCode} used to describe the foreign-key part of an ANY mapping.
 *
 * @see Any
 * @see AnyKeyJdbcType
 *
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface AnyKeyJdbcTypeCode {
	/**
	 * The code for the descriptor to use for the key column
	 *
	 * @see JdbcTypeCode#value
	 */
	int value();
}
