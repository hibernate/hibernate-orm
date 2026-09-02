/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import java.util.function.IntFunction;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.internal.EnumHelper;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.type.SqlTypes.ENUM;

/**
 * A {@link DdlType} representing a SQL {@code enum} type that
 * may be treated as {@code varchar} for most purposes.
 *
 * @see org.hibernate.type.SqlTypes#ENUM
 * @see Dialect#getEnumSupport()
 *
 * @author Gavin King
 */

public class NativeEnumDdlTypeImpl implements DdlType {
	private static final String[] ENUM_KEYWORD = {"enum"};
	private final Dialect dialect;
	private final String castTypeName;
	private final IntFunction<String> parameterizedCastTypeName;

	public NativeEnumDdlTypeImpl(Dialect dialect) {
		this( dialect, "varchar", null );
	}

	public NativeEnumDdlTypeImpl(Dialect dialect, IntFunction<String> parameterizedCastTypeName) {
		this( dialect, "varchar", parameterizedCastTypeName );
	}

	public NativeEnumDdlTypeImpl(
			Dialect dialect,
			String castTypeName,
			IntFunction<String> parameterizedCastTypeName) {
		this.dialect = dialect;
		this.castTypeName = castTypeName;
		this.parameterizedCastTypeName = parameterizedCastTypeName;
	}

	@Override
	public int getSqlTypeCode() {
		return ENUM;
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		return type == null
				? "varchar(" + columnSize.getLength() + ")"
				: dialect.getEnumSupport().getTypeDeclaration(
						type.getReturnedClass().getSimpleName(),
						EnumHelper.getEnumeratedValues( type )
				);
	}

	@Override
	public String[] getRawTypeNames() {
		return ENUM_KEYWORD;
	}

	@Override
	public String getCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
		final Long length = columnSize.getLength();
		return length != null && parameterizedCastTypeName != null
				? parameterizedCastTypeName.apply( Math.toIntExact( length ) )
				: castTypeName( length );
	}

	private String castTypeName(Long length) {
		return length == null ? castTypeName : "varchar(" + length + ")";
	}
}
