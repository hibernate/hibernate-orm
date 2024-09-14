/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.usertype.CompositeUserType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a custom {@link CompositeUserType} for the annotated attribute
 * mapping. Just like the {@link Type @Type} annotation, this annotation may
 * be applied:
 * <ul>
 * <li>directly to a property or field of an entity to specify the custom
 *     type of the property or field,
 * <li>indirectly, as a meta-annotation of an annotation type that is then
 *     applied to various properties and fields, or
 * <li>by default, via a {@linkplain CompositeTypeRegistration registration}.
 * </ul>
 *
 * @see CompositeUserType
 * @see CompositeTypeRegistration
 * @see Type
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface CompositeType {
	/**
	 * The custom type implementor class
	 */
	Class<? extends CompositeUserType<?>> value();
}
