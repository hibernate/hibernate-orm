/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context.spi;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.Incubating;

/**
 * Supplies
 * {@linkplain org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_CREDENTIALS_MAPPER
 * tenant-specific} credential to use to connect to the database. This feature may be
 * used on its own or in conjunction with schema-based or discriminator-based
 * multitenancy.
 * <p>When a tenant credentials mapper is set,
 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider#getConnection(String, String)}
 * is used to acquire JDBC connections. This typically requires that the JDBC {@code DataSource}
 * correctly implements the method {@link javax.sql.DataSource#getConnection(String, String)}.
 *
 * @param <T> The type of the tenant id
 *
 * @since 7.3
 *
 * @see org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_CREDENTIALS_MAPPER
 * @see javax.sql.DataSource#getConnection(String, String)
 * @see org.hibernate.engine.jdbc.connections.spi.ConnectionProvider#getConnection(String, String)
 *
 * @author Gavin King
 */
@Incubating
public interface TenantCredentialsMapper<T> {
	/**
	 * The name of the database user for data belonging to the tenant
	 * with the given identifier.
	 *
	 * @param tenantIdentifier The tenant identifier
	 * @return The name of the database schema belonging to that tenant
	 */
	@NonNull String user(@NonNull T tenantIdentifier);

	/**
	 * The password of the database user for data belonging to the tenant
	 * with the given identifier.
	 *
	 * @param tenantIdentifier The tenant identifier
	 * @return The name of the database schema belonging to that tenant
	 */
	@NonNull String password(@NonNull T tenantIdentifier);
}
