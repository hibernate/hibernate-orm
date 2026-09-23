/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.metamodel.mapping.SqlExpressible;

/**
 * Spanner PostgreSQL DDL type for vector types.
 *
 * @since 7.2
 */
public class SpannerPostgreSQLVectorDdlType extends VectorDdlType {

	private final String baseType;

	public SpannerPostgreSQLVectorDdlType(int sqlTypeCode, String baseType, Dialect dialect) {
		super( sqlTypeCode, baseType + "[]", baseType + "[]", dialect );
		this.baseType = baseType;
	}

	@Override
	public String getTypeName(Size size, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final Integer arrayLength = size != null ? size.getArrayLength() : null;
		if ( arrayLength != null && arrayLength > 0 ) {
			return baseType + "[] VECTOR LENGTH " + arrayLength;
		}
		return baseType + "[]";
	}

	@Override
	public String getCastTypeName(Size size, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
		return baseType + "[]";
	}
}
