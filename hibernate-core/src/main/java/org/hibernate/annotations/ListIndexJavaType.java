/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.type.descriptor.java.BasicJavaType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form of {@link JavaType} for describing the column mapping
 * for the index of a {@code List} or array.
 *
 * @since 6.0
 */
@Inherited
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface ListIndexJavaType {
	/**
	 * The descriptor to use for the list-index
	 *
	 * @see JavaType#value
	 */
	Class<? extends BasicJavaType<?>> value();
}
