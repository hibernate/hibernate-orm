/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.type.SqlTypes.NAMED_ORDINAL_ENUM;

/**
 * A {@link DdlType} representing a named native SQL {@code enum} type,
 * one that often <em>cannot</em> be treated as a {@code int}.
 *
 * @see org.hibernate.type.SqlTypes#NAMED_ORDINAL_ENUM
 * @see Dialect#getEnumTypeDeclaration(Class)
 *
 * @author Gavin King
 */
public class NamedNativeOrdinalEnumDdlTypeImpl implements DdlType {

	private static final String[] ENUM_KEYWORD = {"enum"};

	public NamedNativeOrdinalEnumDdlTypeImpl(Dialect dialect) {
	}

	@Override
	public int getSqlTypeCode() {
		return NAMED_ORDINAL_ENUM;
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		return type.getReturnedClass().getSimpleName();
	}

	@Override
	public String[] getRawTypeNames() {
		return ENUM_KEYWORD;
	}

	@Override
	public String getCastTypeName(Size columnSize, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
		return type.getJdbcMapping().getJavaTypeDescriptor().getJavaTypeClass().getSimpleName();
	}
}
