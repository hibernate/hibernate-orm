/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import static org.hibernate.type.SqlTypes.UUID;

/**
 * @author Jan Schatteman
 */
public class UuidAsBinaryJdbcType extends BinaryJdbcType {

	public static final UuidAsBinaryJdbcType INSTANCE = new UuidAsBinaryJdbcType();

	@Override
	public int getDdlTypeCode() {
		return Types.BINARY;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return UUID;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor( JavaType<X> javaType ) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract( ResultSet rs, int paramIndex, WrapperOptions options ) throws SQLException {
				final byte[] bytes = rs.getBytes( paramIndex );
				return javaType.wrap( bytes == null || bytes.length == 16 ? bytes : Arrays.copyOf( bytes, 16 ), options );
			}

			@Override
			protected X doExtract( CallableStatement statement, int index, WrapperOptions options ) throws SQLException {
				final byte[] bytes = statement.getBytes( index );
				return javaType.wrap( bytes == null || bytes.length == 16 ? bytes : Arrays.copyOf( bytes, 16 ), options );
			}

			@Override
			protected X doExtract( CallableStatement statement, String name, WrapperOptions options )
					throws SQLException {
				final byte[] bytes = statement.getBytes( name );
				return javaType.wrap( bytes == null || bytes.length == 16 ? bytes : Arrays.copyOf( bytes, 16 ), options );
			}
		};
	}

}
