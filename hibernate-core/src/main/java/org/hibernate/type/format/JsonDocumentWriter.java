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
 * Implementation of this inteface will used to build a JSON document.
 * Implementation example is {@link StringJsonDocumentWriter }
 * @author Emmanuel Jannetti
 */

public interface JsonDocumentWriter {
	/**
	 * Starts a new JSON Objects.
	 */
	void startObject();

	/**
	 * Ends a new JSON Objects
	 */
	void endObject();

	/**
	 * Starts a new JSON array.
	 * @throws IOException an I/O error roccured while starting the object.
	 */
	void startArray();

	/**
	 * Ends a new JSON array.
	 * @throws IOException an I/O error roccured while starting the object.
	 */
	void endArray();

	/**
	 * Adds a new JSON element name.
	 * @param key the element name.
	 * @throws IOException an I/O error roccured while starting the object.
	 */
	void objectKey(String key);

	/**
	 * Adds a new JSON element null value.
	 * @throws IOException an I/O error roccured while starting the object.
	 */
	void nullValue();

	/**
	 * Adds a new JSON element boolean value.
	 * @param value the element boolean name.
	 */
	void booleanValue(boolean value);

	/**
	 * Adds a new JSON element string value.
	 * @param value the element string name.
	 */
	void stringValue(String value);

	/**
	 * Adds a new JSON element Number value.
	 * @param value the element Number name.
	 */
	void numberValue(Number value);

	/**
	 * Adds a JSON value to the document
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
