/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

/**
 * A {@link JdbcType} with a fixed SQL type name.
 *
 * @see StructuredJdbcType
 * @see org.hibernate.dialect.type.OracleArrayJdbcType
 * @see org.hibernate.dialect.type.OracleNestedTableJdbcType
 */
public interface SqlTypedJdbcType extends JdbcType {

	String getSqlTypeName();
}
