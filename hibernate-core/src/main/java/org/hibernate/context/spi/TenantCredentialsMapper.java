/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context.spi;

/**
 * Obtains the tenant-specific credential to use when obtaining a JDBC connection
 * from the {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
 * for a given tenant identifier when
 * {@linkplain org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_CREDENTIALS_MAPPER
 * credentials-based multitenancy} is enabled.
 * <p>{@link javax.sql.DataSource#getConnection(String, String)} is used to acquire
 * {@linkplain java.sql.Connection JDBC connections}.
 *
 * @param <T> The type of the tenant id
 *
 * @since 7.1
 *
 * @author Gavin King
 */
public interface TenantCredentialsMapper<T> {
	/**
	 * The name of the database user for data belonging to the tenant
	 * with the given identifier.
	 *
	 * @param tenantIdentifier The tenant identifier
	 * @return The name of the database schema belonging to that tenant
	 */
	String user(T tenantIdentifier);

	/**
	 * The password of the database user for data belonging to the tenant
	 * with the given identifier.
	 *
	 * @param tenantIdentifier The tenant identifier
	 * @return The name of the database schema belonging to that tenant
	 */
	String password(T tenantIdentifier);
}
