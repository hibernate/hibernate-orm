/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context.spi;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.Incubating;

/**
 * Obtains the name of a database schema for a given tenant identifier when
 * {@linkplain org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_SCHEMA_MAPPER
 * schema-based multitenancy} is enabled.
 *
 * @param <T> The type of the tenant id
 *
 * @since 7.1
 *
 * @author Gavin King
 */
@Incubating
public interface TenantSchemaMapper<T> {
	/**
	 * The name of the database schema for data belonging to the tenant with the
	 * given identifier.
	 * <p>
	 * Called when {@value org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_SCHEMA_MAPPER}
	 * is enabled.
	 *
	 * @param tenantIdentifier The tenant identifier
	 * @return The name of the database schema belonging to that tenant
	 *
	 * @see org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_SCHEMA_MAPPER
	 */
	@NonNull String schemaName(@NonNull T tenantIdentifier);
}
