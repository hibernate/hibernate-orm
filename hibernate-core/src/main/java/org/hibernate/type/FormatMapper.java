/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A mapper for mapping objects to and from a format.
 *
 * @see org.hibernate.cfg.AvailableSettings#JSON_FORMAT_MAPPER
 *
 * @author Christian Beikov
 */
public interface FormatMapper {

	/**
	 * Deserializes an object from the character sequence.
	 */
	<T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions);

	/**
	 * Serializes the object to a string.
	 */
	<T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions);
}
