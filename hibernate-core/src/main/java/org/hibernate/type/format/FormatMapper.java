/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.Incubating;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.io.IOException;

/**
 * A mapper for mapping objects to and from a format.
 * <ul>
 * <li>A {@code FormatMapper} for JSON may be selected using the configuration
 *     property {@value org.hibernate.cfg.AvailableSettings#JSON_FORMAT_MAPPER}.
 * <li>A {@code FormatMapper} for XML may be selected using the configuration
 *     property {@value org.hibernate.cfg.AvailableSettings#XML_FORMAT_MAPPER}.
 * </ul>
 *
 *
 * @see org.hibernate.cfg.AvailableSettings#JSON_FORMAT_MAPPER
 * @see org.hibernate.cfg.AvailableSettings#XML_FORMAT_MAPPER
 *
 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJsonFormatMapper()
 * @see org.hibernate.boot.spi.SessionFactoryOptions#getXmlFormatMapper()
 *
 * @see org.hibernate.type.descriptor.jdbc.JsonJdbcType
 * @see org.hibernate.type.descriptor.jdbc.XmlJdbcType
 *
 * @author Christian Beikov
 */
@Incubating
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
	boolean supportsSourceType(Class<?> sourceType);

	/**
	 * Checks that this mapper supports a type as a target type.
	 * @param targetType the target type
	 * @return <code>true</code> if the type is supported, false otherwise.
	 */
	boolean supportsTargetType(Class<?> targetType);

	<T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options) throws IOException;

	<T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException;
}
