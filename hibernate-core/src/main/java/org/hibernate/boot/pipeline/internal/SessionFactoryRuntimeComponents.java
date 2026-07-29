/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.CacheFactory;
import org.hibernate.engine.extension.spi.ExtensionIntegrationService;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationResolver;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.stat.spi.StatisticsFactory;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/// Internal product for constructor values that can be prepared before the
/// `SessionFactoryImpl` instance exists.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryRuntimeComponents(
		TypeConfiguration typeConfiguration,
		ModelsContext modelsContext,
		ClassLoaderService classLoaderService,
		ClassLoaderAccess classLoaderAccess,
		ManagedBeanRegistry managedBeanRegistry,
		ManagedTypeRepresentationResolver representationStrategySelector,
		NativeQueryInterpreter nativeQueryInterpreter,
		ExtensionIntegrationService extensionIntegrationService,
		CacheFactory cacheFactory,
		Set<DomainDataRegionConfig> cacheRegionConfigs,
		StatisticsFactory statisticsFactory,
		SqlStringGenerationContext sqlStringGenerationContext,
		StatementObserver statementObserver,
		SessionFactoryObserver[] sessionFactoryObservers,
		Map<String, FilterDefinition> filterDefinitions,
		Collection<FilterDefinition> autoEnabledFilters,
		JavaType<Object> tenantIdentifierJavaType) {

	public SessionFactoryRuntimeComponents {
		cacheRegionConfigs = Set.copyOf( cacheRegionConfigs );
		sessionFactoryObservers = sessionFactoryObservers == null
				? new SessionFactoryObserver[0]
				: sessionFactoryObservers.clone();
	}

	@Override
	public SessionFactoryObserver[] sessionFactoryObservers() {
		return sessionFactoryObservers.clone();
	}
}
