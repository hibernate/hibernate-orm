/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.StatementObserver;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.IgnoredStatementObserver;
import org.hibernate.type.descriptor.java.JavaType;

/// Builds constructor-ready runtime components that do not require the
/// SessionFactory instance or SessionFactory service registry.
///
/// @since 9.0
/// @author Steve Ebersole
public final class SessionFactoryRuntimeComponentsBuilder {
	private SessionFactoryRuntimeComponentsBuilder() {
	}

	public static SessionFactoryRuntimeComponents build(
			MetadataImplementor metadata,
			ResolvedSessionFactorySettings settings,
			BootstrapContext bootstrapContext) {
		final var filterDefinitions = new HashMap<>( metadata.getFilterDefinitions() );
		return new SessionFactoryRuntimeComponents(
				bootstrapContext.getTypeConfiguration(),
				statementObserver( settings.statementObserver() ),
				settings.sessionFactoryObservers(),
				filterDefinitions,
				autoEnabledFilters( filterDefinitions ),
				tenantIdentifierType( filterDefinitions, settings.defaultTenantIdentifierJavaType() )
		);
	}

	public static SessionFactoryRuntimeComponents build(
			MetadataImplementor metadata,
			SessionFactoryOptions options,
			BootstrapContext bootstrapContext) {
		final var filterDefinitions = new HashMap<>( metadata.getFilterDefinitions() );
		return new SessionFactoryRuntimeComponents(
				bootstrapContext.getTypeConfiguration(),
				statementObserver( options.getStatementObserver() ),
				options.getSessionFactoryObservers(),
				filterDefinitions,
				autoEnabledFilters( filterDefinitions ),
				tenantIdentifierType( filterDefinitions, options.getDefaultTenantIdentifierJavaType() )
		);
	}

	private static StatementObserver statementObserver(StatementObserver statementObserver) {
		return statementObserver == null ? IgnoredStatementObserver.IGNORE : statementObserver;
	}

	private static Collection<FilterDefinition> autoEnabledFilters(Map<String, FilterDefinition> filterDefinitions) {
		final var autoEnabledFilters = new ArrayList<FilterDefinition>();
		for ( var filter : filterDefinitions.values() ) {
			if ( filter.isAutoEnabled() ) {
				autoEnabledFilters.add( filter );
			}
		}
		return autoEnabledFilters;
	}

	private static JavaType<Object> tenantIdentifierType(
			Map<String, FilterDefinition> filterDefinitions,
			JavaType<Object> defaultTenantIdentifierJavaType) {
		final var tenantFilter = filterDefinitions.get( TenantIdBinder.FILTER_NAME );
		if ( tenantFilter == null ) {
			return defaultTenantIdentifierJavaType;
		}
		else {
			final var jdbcMapping = tenantFilter.getParameterJdbcMapping( TenantIdBinder.PARAMETER_NAME );
			assert jdbcMapping != null;
			//NOTE: this is completely unsound
			//noinspection unchecked
			return (JavaType<Object>) jdbcMapping.getJavaTypeDescriptor();
		}
	}
}
