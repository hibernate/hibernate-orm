/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.service.ServiceRegistry;

import java.util.Map;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER;

/**
 * Exposes useful strategy objects to user-written components, for example,
 * to a custom implementation of {@link MultiTenantConnectionProvider}.
 *
 * @since 7.1
 *
 * @author Gavin King
 */
@Incubating
public class MultiTenancy {

	public static boolean isMultiTenancyEnabled(ServiceRegistry serviceRegistry) {
		return serviceRegistry.getService( MultiTenantConnectionProvider.class ) != null;
	}

	@SuppressWarnings("unchecked")
	public static CurrentTenantIdentifierResolver<Object> getTenantIdentifierResolver(
			Map<String,Object> settings, StandardServiceRegistry registry) {
		final var currentTenantIdentifierResolver =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( CurrentTenantIdentifierResolver.class,
								settings.get( MULTI_TENANT_IDENTIFIER_RESOLVER ) );
		if ( currentTenantIdentifierResolver == null ) {
			return Helper.getBean(
					Helper.getBeanContainer( registry ),
					CurrentTenantIdentifierResolver.class,
					true,
					false,
					null
			);
		}
		else {
			return currentTenantIdentifierResolver;
		}
	}

	@SuppressWarnings("unchecked")
	public static TenantSchemaMapper<Object> getTenantSchemaMapper(
			Map<String,Object> settings, StandardServiceRegistry registry) {
		final var tenantSchemaMapper =
				registry.requireService( StrategySelector.class )
						.resolveStrategy( TenantSchemaMapper.class,
								settings.get( MULTI_TENANT_SCHEMA_MAPPER ) );
		if ( tenantSchemaMapper == null ) {
			return Helper.getBean(
					Helper.getBeanContainer( registry ),
					TenantSchemaMapper.class,
					true,
					false,
					null
			);
		}
		return tenantSchemaMapper;
	}
}
