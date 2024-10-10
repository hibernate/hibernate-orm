/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

import static org.hibernate.type.SqlTypes.VARBINARY_UUID;

/**
 * @author Jan Schatteman
 */
public class UUIDasVarBinaryJdbcType extends BinaryJdbcType {

	public static final UUIDasVarBinaryJdbcType INSTANCE = new UUIDasVarBinaryJdbcType();

	@Override
	public int getDdlTypeCode() {
		return Types.VARBINARY;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return VARBINARY_UUID;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor( JavaType<X> javaType ) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract( ResultSet rs, int paramIndex, WrapperOptions options ) throws SQLException {
				return javaType.wrap( Arrays.copyOf(rs.getBytes( paramIndex), 16), options );
			}

			@Override
			protected X doExtract( CallableStatement statement, int index, WrapperOptions options ) throws SQLException {
				return javaType.wrap( Arrays.copyOf(statement.getBytes(index), 16), options );
			}

			@Override
			protected X doExtract( CallableStatement statement, String name, WrapperOptions options )
					throws SQLException {
				return javaType.wrap( Arrays.copyOf(statement.getBytes(name), 16), options );
			}
		};
	}

}
