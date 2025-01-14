/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;


import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.io.IOException;

/**
 * JSON document producer.
 * Used to parse JSON documents. Implementors of this will define
 * proper callback implementations.
 *
 * @author Emmanuel Jannetti
 */

public interface JsonDocumentWriter {
	/**
	 * Callback to be called when the start of an JSON object is encountered.
	 */
	void startObject() throws IOException;

	/**
	 * Callback to be called when the end of an JSON object is encountered.
	 */
	void endObject() throws IOException;

	/**
	 * Callback to be called when the start of an array is encountered.
	 */
	void startArray();

	/**
	 * Callback to be called when the end of an array is encountered.
	 */
	void endArray();

	/**
	 * Callback to be called when the key of JSON attribute is encountered.
	 * @param key the attribute name
	 */
	void objectKey(String key);

	/**
	 * Callback to be called when null value is encountered.
	 */
	void nullValue();

	/**
	 * Callback to be called when boolean value is encountered.
	 * @param value the boolean value
	 */
	void booleanValue(boolean value);

	/**
	 * Callback to be called when string value is encountered.
	 * @param value the String value
	 */
	void stringValue(String value);

	/**
	 * Callback to be called when Number value is encountered.
	 * @param value the String value.
	 */
	void numberValue(Number value);

	/**
	 * Serialize a JSON value to the document
	 * @param value the value to be serialized
	 * @param javaType the Java type of the value
	 * @param jdbcType the JDBC type for the value to be serialized
	 * @param options the wrapping options
	 */
	void serializeJsonValue(Object value,
							JavaType<Object> javaType,
							JdbcType jdbcType,
							WrapperOptions options);
}
