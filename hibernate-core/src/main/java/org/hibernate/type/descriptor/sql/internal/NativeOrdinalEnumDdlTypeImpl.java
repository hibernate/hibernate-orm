/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.internal.EnumHelper;
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
	private static final String[] ENUM_KEYWORD = {"enum"};
	private final Dialect dialect;

	public NativeOrdinalEnumDdlTypeImpl(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public int getSqlTypeCode() {
		return ORDINAL_ENUM;
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		return type == null
				? "int"
				: dialect.getEnumTypeDeclaration(
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
		return "int";
	}
}
