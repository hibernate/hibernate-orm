/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Specialized type mapping for generic vector {@link SqlTypes#VECTOR} SQL data type for DB2.
 */
public class DB2VectorJdbcType extends DB2FloatVectorJdbcType {

	public DB2VectorJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public String getFriendlyName() {
		return "VECTOR";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR;
	}

}
