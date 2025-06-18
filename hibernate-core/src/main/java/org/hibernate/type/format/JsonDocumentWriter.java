/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;


import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * JSON document producer.
 * Implementation of this inteface will used to build a JSON document.
 * Implementation example is {@link StringJsonDocumentWriter }
 * @author Emmanuel Jannetti
 */

public interface JsonDocumentWriter {
	/**
	 * Starts a new JSON Objects.
	 * @return this instance
	 */
	JsonDocumentWriter startObject();

	/**
	 * Ends a new JSON Objects
	 * @return this instance
	 */
	JsonDocumentWriter endObject();

	/**
	 * Starts a new JSON array.
	 * @return this instance
	 * @throws IOException an I/O error roccured while starting the object.
	 */
	JsonDocumentWriter startArray();

	/**
	 * Ends a new JSON array.
	 * @return this instance
	 * @throws IOException an I/O error roccured while starting the object.
	 */
	JsonDocumentWriter endArray();

	/**
	 * Adds a new JSON element name.
	 * @param key the element name.
	 * @return this instance
	 * @throws IllegalArgumentException key name does not follow JSON specification.
	 * @throws IOException an I/O error occurred while starting the object.
	 */
	JsonDocumentWriter objectKey(String key);

	/**
	 * Adds a new JSON element null value.
	 * @return this instance
	 * @throws IOException an I/O error roccured while starting the object.
	 */
	JsonDocumentWriter nullValue();

	/**
	 * Adds a new JSON element boolean value.
	 * @return this instance
	 * @param value the element boolean name.
	 */
	JsonDocumentWriter booleanValue(boolean value);

	/**
	 * Adds a new JSON element string value.
	 * @return this instance
	 * @param value the element string name.
	 */
	JsonDocumentWriter stringValue(String value);

	/**
	 * Adds a JSON value to the document
	 * @param value the value to be serialized
	 * @param javaType the Java type of the value
	 * @param jdbcType the JDBC type for the value to be serialized
	 * @param options the wrapping options
	 * @return this instance
	 */
	<T> JsonDocumentWriter serializeJsonValue(Object value,
							JavaType<T> javaType,
							JdbcType jdbcType,
							WrapperOptions options);

	/**
	 * Returns {@code true} if this writer always expands properties to nested JSON objects,
	 * which is useful for obtaining a verbose representation Hibernate data in JSON format.
	 *
	 * @return {@code true} if properties should be expanded, {@code false} otherwise
	 */
	@Internal
	default boolean expandProperties() {
		return false;
	}

	/**
	 * Tracks the provided {@code entity} instance and invokes the {@code action} with either
	 * {@code true} if the entity was not already encountered or {@code false} otherwise.
	 *
	 * @param entity the entity instance to track
	 * @param entityType the type of the entity instance
	 * @param action the action to invoke while tracking the entity
	 */
	@Internal
	default void trackingEntity(Object entity, EntityMappingType entityType, Consumer<Boolean> action) throws IOException {
		action.accept( true );
	}
}
