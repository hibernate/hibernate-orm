/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.sql.SQLException;

/**
 * Adapter for JSON value on given JDBC types.
 * @author emmanuel Jannetti
 */
public interface JsonValueJDBCTypeAdapter {
	/**
	 * Gets an Object out of a JSON document reader according to a given types.
	 * @param jdbcJavaType the desired JavaType for the return Object.
	 * @param jdbcType the desired JdbcType for the return Object.
	 * @param source the JSON document reader from which to get the value to be translated.
	 * @param options the wrapping option
	 * @return the translated value.
	 * @throws SQLException if translation failed.
	 */
	Object fromValue(
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType,
			JsonDocumentReader source,
			WrapperOptions options) throws SQLException;

	/**
	 * Gets an Object out of a JSON document reader according to a given types.
	 * This method is called when the current available value in the reader is a numeric one.
	 * @param jdbcJavaType the desired JavaType for the return Object.
	 * @param jdbcType the desired JdbcType for the return Object.
	 * @param source the JSON document reader from which to get the value to be translated.
	 * @param options the wrapping option
	 * @return the translated value.
	 * @throws SQLException if translation failed.
	 */
	Object fromNumericValue(JavaType<?> jdbcJavaType,
							JdbcType jdbcType,
							JsonDocumentReader source,
							WrapperOptions options) throws SQLException;

}
