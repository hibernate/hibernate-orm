/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.OracleJsonBlobJdbcType;

/**
 * Specialized type mapping for {@code JSON} and the JSON SQL data type for Oracle.
 *
 * @author Christian Beikov
 */
public class OracleJsonJdbcType extends OracleJsonBlobJdbcType {
	/**
	 * Singleton access
	 */
	public static final OracleJsonJdbcType INSTANCE = new OracleJsonJdbcType( null );

	protected OracleJsonJdbcType(EmbeddableMappingType embeddableMappingType) {
		super( embeddableMappingType );
	}

	@Override
	public int getDdlTypeCode() {
		return SqlTypes.JSON;
	}

	@Override
	public String toString() {
		return "OracleJsonJdbcType";
	}

	@Override
	public AggregateJdbcType resolveAggregateJdbcType(
			EmbeddableMappingType mappingType,
			String sqlType,
			RuntimeModelCreationContext creationContext) {
		return new OracleJsonJdbcType( mappingType );
	}

	@Override
	public String getCheckCondition(String columnName, JavaType<?> javaType, BasicValueConverter<?, ?> converter, Dialect dialect) {
		// No check constraint necessary, because the JSON DDL type is already OSON encoded
		return null;
	}
}
