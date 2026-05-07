/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * DDL type for vector types.
 *
 * @since 7.2
 */
public class VectorDdlType extends DdlTypeImpl {

	public VectorDdlType(int sqlTypeCode, boolean isLob, String typeNamePattern, String castTypeNamePattern, String castTypeName, Dialect dialect) {
		super( sqlTypeCode, isLob, typeNamePattern, castTypeNamePattern, castTypeName, dialect );
	}

	public VectorDdlType(int sqlTypeCode, String typeNamePattern, String castTypeNamePattern, String castTypeName, Dialect dialect) {
		super( sqlTypeCode, typeNamePattern, castTypeNamePattern, castTypeName, dialect );
	}

	public VectorDdlType(int sqlTypeCode, boolean isLob, String typeNamePattern, String castTypeName, Dialect dialect) {
		super( sqlTypeCode, isLob, typeNamePattern, castTypeName, dialect );
	}

	public VectorDdlType(int sqlTypeCode, String typeNamePattern, String castTypeName, Dialect dialect) {
		super( sqlTypeCode, typeNamePattern, castTypeName, dialect );
	}

	public VectorDdlType(int sqlTypeCode, String typeNamePattern, Dialect dialect) {
		super( sqlTypeCode, typeNamePattern, dialect );
	}

	@Override
	public String getTypeName(Size size, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final Integer arrayLength = size.getArrayLength();
		return formatTypeName(
				arrayLength == null ? null : arrayLength.longValue(),
				null,
				null
		);
	}
}
