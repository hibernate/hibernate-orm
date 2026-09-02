/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

/**
 * A {@link JdbcType} with a fixed SQL type name.
 *
 * @see StructuredJdbcType
 * @see org.hibernate.dialect.type.spi.OracleJdbcTypes#driverArrayConstructor(org.hibernate.service.ServiceRegistry)
 * @see org.hibernate.dialect.type.spi.OracleJdbcTypes#driverNestedTableConstructor(org.hibernate.service.ServiceRegistry)
 */
public interface SqlTypedJdbcType extends JdbcType {

	String getSqlTypeName();
}
