/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the mapping of a single {@linkplain Any any}-valued
 * discriminator value to its corresponding entity type.
 * <p>
 * This annotation may be applied:
 * <ul>
 * <li>directly to a field or property annotated {@link Any}, or
 * <li>indirectly, as a meta-annotation, to a second annotation
 *     which is then applied to various fields or properties
 *     annotated {@link Any} with the same target entity type.
 * </ul>
 *
 * @see Any
 * @see AnyDiscriminator
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
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
