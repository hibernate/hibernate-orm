/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

/**
 * A {@link JdbcType} with a fixed SQL type name.
 *
 * @see StructJdbcType
 * @see org.hibernate.dialect.OracleArrayJdbcType
 * @see org.hibernate.dialect.OracleNestedTableJdbcType
 */
public interface SqlTypedJdbcType extends JdbcType {

	String getSqlTypeName();
}
