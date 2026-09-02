/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.io.IOException;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Maps objects to and from a structured text format.
///
/// Implement this contract to integrate another JSON or XML binding library.
/// Supply a JSON implementation through
/// [org.hibernate.cfg.AvailableSettings#JSON_FORMAT_MAPPER] and an XML
/// implementation through [org.hibernate.cfg.AvailableSettings#XML_FORMAT_MAPPER].
///
/// @see org.hibernate.cfg.AvailableSettings#JSON_FORMAT_MAPPER
/// @see org.hibernate.cfg.AvailableSettings#XML_FORMAT_MAPPER
/// @see org.hibernate.boot.spi.SessionFactoryOptions#getJsonFormatMapper()
/// @see org.hibernate.boot.spi.SessionFactoryOptions#getXmlFormatMapper()
/// @see org.hibernate.type.descriptor.jdbc.JsonJdbcType
/// @see org.hibernate.type.descriptor.jdbc.XmlJdbcType
///
/// @author Christian Beikov
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface FormatMapper {

	/**
	 * Deserializes an object from the character sequence.
	 */
	<T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions);

	/**
	 * Serializes the object to a string.
	 */
	<T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions);

	/**
	 * Checks that this mapper supports a type as a source type.
	 * @param sourceType the source type
	 * @return <code>true</code> if the type is supported, false otherwise.
	 */
	default boolean supportsSourceType(Class<?> sourceType) {
		return false;
	};

	/**
	 * Checks that this mapper supports a type as a target type.
	 * @param targetType the target type
	 * @return <code>true</code> if the type is supported, false otherwise.
	 */
	default boolean supportsTargetType(Class<?> targetType) {
		return false;
	}

	default <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options) throws IOException {
		throw new UnsupportedOperationException( "Unsupportd target type " + target.getClass() );
	};

	default <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
		throw new UnsupportedOperationException( "Unsupportd source type " + source.getClass() );
	};
}
