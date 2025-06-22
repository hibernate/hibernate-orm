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
	 * Specifies a {@link MultiTenantConnectionProvider}
	 * to use. Since {@code MultiTenantConnectionProvider} is also a service, it may be configured
	 * directly via the {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}.
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_CONNECTION_PROVIDER = "hibernate.multi_tenant_connection_provider";

	/**
	 * Specifies a {@link CurrentTenantIdentifierResolver} to use,
	 * either:
	 * <ul>
	 *     <li>an instance of {@code CurrentTenantIdentifierResolver},
	 *     <li>a {@link Class} representing an class that implements {@code CurrentTenantIdentifierResolver}, or
	 *     <li>the name of a class that implements {@code CurrentTenantIdentifierResolver}.
	 * </ul>
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver)
	 *
	 * @since 4.1
	 */
	String MULTI_TENANT_IDENTIFIER_RESOLVER = "hibernate.tenant_identifier_resolver";

	/**
	 * During bootstrap, Hibernate needs access to any Connection for access to {@link java.sql.DatabaseMetaData}.
	 * <p/>
	 * This setting configures the name of the DataSource to use for this access
	 */
	String TENANT_IDENTIFIER_TO_USE_FOR_ANY_KEY = "hibernate.multi_tenant.datasource.identifier_for_any";
}
