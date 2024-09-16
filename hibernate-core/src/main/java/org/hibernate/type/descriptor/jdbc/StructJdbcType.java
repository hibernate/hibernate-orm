/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.SqlTypes;

/**
 * Descriptor for aggregate handling like {@link SqlTypes#STRUCT STRUCT}, {@link SqlTypes#JSON JSON} and {@link SqlTypes#SQLXML SQLXML}.
 */
public interface StructJdbcType extends AggregateJdbcType, SqlTypedJdbcType {

	String getStructTypeName();

	@Override
	default String getSqlTypeName() {
		return getStructTypeName();
	}
}
