/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.internal.EnumHelper;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.type.SqlTypes.ORDINAL_ENUM;

/**
 * A {@link DdlType} representing a SQL {@code enum} type that
 * may be treated as {@code int} for most purposes.
 *
 * @see org.hibernate.type.SqlTypes#ORDINAL_ENUM
 * @see Dialect#getEnumTypeDeclaration(Class)
 */

public class NativeOrdinalEnumDdlTypeImpl implements DdlType {
	private final Dialect dialect;

	public NativeOrdinalEnumDdlTypeImpl(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public int getSqlTypeCode() {
		return ORDINAL_ENUM;
	}

	@Override @SuppressWarnings("unchecked")
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		return dialect.getEnumTypeDeclaration(
				type.getReturnedClass().getSimpleName(),
				EnumHelper.getEnumeratedValues( type )
		);
	}

	@Override
	public String getRawTypeName() {
		// this
		return "enum";
	}

	@Override
	public String getTypeName(Long size, Integer precision, Integer scale) {
		return "int";
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType) {
		return "int";
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType, Long length, Integer precision, Integer scale) {
		return getTypeName( length, precision, scale );
	}
}
