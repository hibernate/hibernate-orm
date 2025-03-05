/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

/**
 * A {@link DdlType} representing a named native SQL {@code enum} type,
 * one that often <em>cannot</em> be treated as a {@code varchar}.
 *
 * @see org.hibernate.type.SqlTypes#NAMED_ENUM
 * @see Dialect#getEnumTypeDeclaration(Class)
 *
 * @author Gavin King
 */
public class NamedNativeEnumDdlTypeImpl implements DdlType {
	private final Dialect dialect;

	public NamedNativeEnumDdlTypeImpl(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public int getSqlTypeCode() {
		// note: also used for NAMED_ENUM
		return NAMED_ENUM;
	}

	@Override @SuppressWarnings("unchecked")
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		return dialect.getEnumTypeDeclaration( (Class<? extends Enum<?>>) type.getReturnedClass() );
	}

	@Override
	public String getRawTypeName() {
		return "enum";
	}

	@Override
	public String getTypeName(Long size, Integer precision, Integer scale) {
		throw new UnsupportedOperationException( "native enum type" );
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType) {
		return dialect.getEnumTypeDeclaration( (Class<? extends Enum<?>>) javaType.getJavaType() );
	}

	@Override
	public String getCastTypeName(JdbcType jdbcType, JavaType<?> javaType, Long length, Integer precision, Integer scale) {
		return dialect.getEnumTypeDeclaration( (Class<? extends Enum<?>>) javaType.getJavaType() );
	}
}
