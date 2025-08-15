/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

/**
 * @author Steve Ebersole
 */
public interface MultiTenancySettings {

	/**
	 * Specifies a {@link MultiTenantConnectionProvider} to use, either:
	 * <ul>
	 *     <li>an instance of {@code MultiTenantConnectionProvider},
	 *     <li>a {@link Class} representing a class that implements {@code MultiTenantConnectionProvider}, or
	 *     <li>the name of a class that implements {@code MultiTenantConnectionProvider}.
	 * </ul>
	 *
	 * Since {@code MultiTenantConnectionProvider} is also a service, it may be configured
	 * directly via the {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}.
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_CONNECTION_PROVIDER = "hibernate.multi_tenant_connection_provider";

	/**
	 * Specifies a {@link CurrentTenantIdentifierResolver} to use, either:
	 * <ul>
	 *     <li>an instance of {@code CurrentTenantIdentifierResolver},
	 *     <li>a {@link Class} representing a class that implements {@code CurrentTenantIdentifierResolver}, or
	 *     <li>the name of a class that implements {@code CurrentTenantIdentifierResolver}.
	 * </ul>
	 *
	 * @see CurrentTenantIdentifierResolver
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyCurrentTenantIdentifierResolver
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_IDENTIFIER_RESOLVER = "hibernate.tenant_identifier_resolver";

	/**
	 * During bootstrap, Hibernate needs access to a {@code Connection} for access
	 * to the {@link java.sql.DatabaseMetaData}. This setting configures the tenant id
	 * to use when obtaining the {@link javax.sql.DataSource} to use for this access.
	 */
	String TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY = "hibernate.multi_tenant.datasource.identifier_for_any";

	/**
	 * Specifies a {@link org.hibernate.context.spi.TenantSchemaMapper} to use, either:
	 * <ul>
	 *     <li>an instance of {@code TenantSchemaMapper},
	 *     <li>a {@link Class} representing a class that implements {@code TenantSchemaMapper}, or
	 *     <li>the name of a class that implements {@code TenantSchemaMapper}.
	 * </ul>
	 * When a tenant schema mapper is set, {@link java.sql.Connection#setSchema(String)}}
	 * is called on newly acquired JDBC connections with the schema name returned by
	 * {@link org.hibernate.context.spi.TenantSchemaMapper#schemaName}.
	 * <p>
	 * By default, there is no tenant schema mapper.
	 *
	 * @see org.hibernate.context.spi.TenantSchemaMapper
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyTenantSchemaMapper
	 *
	 * @since 7.1
	 */
	String MULTI_TENANT_SCHEMA_MAPPER = "hibernate.multi_tenant.schema_mapper";
}
