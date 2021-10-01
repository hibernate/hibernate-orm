/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the mapping for a single any-valued discriminator value
 * to the corresponding entity
 *
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention( RUNTIME )
@Repeatable(AnyDiscriminatorValues.class)
public @interface AnyDiscriminatorValue {
	/**
	 * The discriminator value
	 */
	String discriminator();

	/**
	 * The corresponding entity
	 */
	Class<?> entity();
}
