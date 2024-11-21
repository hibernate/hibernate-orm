/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.OracleJsonArrayBlobJdbcType;

/**
 * Specialized type mapping for {@code JSON} and the JSON SQL data type for Oracle.
 *
 * @author Christian Beikov
 */
public class OracleJsonArrayJdbcType extends OracleJsonArrayBlobJdbcType {

	public OracleJsonArrayJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public String toString() {
		return "OracleJsonJdbcType";
	}

	@Override
	public String getCheckCondition(String columnName, JavaType<?> javaType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		// No check constraint necessary, because the JSON DDL type is already OSON encoded
		return null;
	}
}
