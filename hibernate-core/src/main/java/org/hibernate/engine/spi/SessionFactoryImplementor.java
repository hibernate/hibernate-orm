/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.Collection;
import java.util.Map;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.creation.spi.SessionBuilderImplementor;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines the internal contract between the {@link SessionFactory} and the internal
 * implementation of Hibernate.
 *
 * @see SessionFactory
 * @see org.hibernate.internal.SessionFactoryImpl
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionFactoryImplementor extends SessionFactory {
	/**
	 * The UUID assigned to this {@code SessionFactory}.
	 * <p>
	 * The value is generated as a {@link java.util.UUID}, but kept as a string.
	 *
	 * @see org.hibernate.internal.SessionFactoryRegistry#getSessionFactory
	 */
	String getUuid();

	/**
	 * Overrides {@link SessionFactory#openSession()} to widen the return type:
	 * this is useful for internal code depending on {@link SessionFactoryImplementor}
	 * as it would otherwise need to frequently resort to casting to the internal contract.
	 *
	 * @return the opened {@code Session}.
	 */
	@Override
	SessionImplementor openSession();

	/**
	 * Obtain a {@linkplain org.hibernate.SessionBuilder session builder}
	 * for creating new instances of {@link org.hibernate.Session} with
	 * certain customized options.
	 */
	@Override
	SessionBuilderImplementor withOptions();

	/**
	 * Get a non-transactional "current" session.
	 *
	 * @apiNote This is used by {@code hibernate-envers}.
	 */
	SessionImplementor openTemporarySession();

	/**
	 * Obtain the {@link CacheImplementor}.
	 */
	@Override
	CacheImplementor getCache();

	/**
	 * Obtain the {@link StatisticsImplementor}.
	 */
	@Override
	StatisticsImplementor getStatistics();

	/**
	 * Obtain the {@link TypeConfiguration}
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Obtain the {@link RuntimeMetamodelsImplementor}
	 */
	RuntimeMetamodelsImplementor getRuntimeMetamodels();

	/**
	 * Obtain the {@link MappingMetamodelImplementor}
	 */
	default MappingMetamodelImplementor getMappingMetamodel() {
		return getRuntimeMetamodels().getMappingMetamodel();
	}

	/**
	 * Obtain the {@link JpaMetamodel}
	 */
	default JpaMetamodel getJpaMetamodel() {
		return getRuntimeMetamodels().getJpaMetamodel();
	}

	/**
	 * Obtain the {@link QueryEngine}
	 */
	QueryEngine getQueryEngine();

	/**
	 * Obtain the {@link SqlTranslationEngine}
	 */
	SqlTranslationEngine getSqlTranslationEngine();

	/**
	 * Access to the {@code ServiceRegistry} for this {@code SessionFactory}.
	 *
	 * @return The factory's ServiceRegistry
	 */
	ServiceRegistryImplementor getServiceRegistry();

	/**
	 * Get the EventEngine associated with this SessionFactory
	 */
	EventEngine getEventEngine();

	/**
	 * Retrieve a {@linkplain FetchProfile fetch profile} by name.
	 *
	 * @param name The name of the profile to retrieve.
	 * @return The profile definition
	 *
	 * @deprecated Use {@link SqlTranslationEngine#getFetchProfile(String)}
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	FetchProfile getFetchProfile(String name);

	/**
	 * Get the identifier generator for the hierarchy
	 *
	 * @deprecated Only used in one place, will be removed
	 */
	@Deprecated(since = "7", forRemoval = true)
	Generator getGenerator(String rootEntityName);

	/**
	 * Obtain the {@link EntityNotFoundDelegate}
	 */
	EntityNotFoundDelegate getEntityNotFoundDelegate();

	/**
	 * Register a {@link SessionFactoryObserver} of this factory.
	 */
	void addObserver(SessionFactoryObserver observer);

	/**
	 * Obtain the {@link CustomEntityDirtinessStrategy}
	 */
	//todo make a Service ?
	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();

	/**
	 * Obtain the {@link CurrentTenantIdentifierResolver}
	 */
	//todo make a Service ?
	CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver();

	/**
	 * Object the current tenant identifier using the
	 * {@linkplain #getCurrentTenantIdentifierResolver() resolver}.
	 *
	 * @since 7.2
	 */
	@Incubating
	Object resolveTenantIdentifier();

	/**
	 * The {@link JavaType} to use for a tenant identifier.
	 *
	 * @since 6.4
	 */
	JavaType<Object> getTenantIdentifierJavaType();

	/**
	 * Access to the {@linkplain EventListenerGroups event listener groups}.
	 *
	 * @since 7.0
	 */
	@Internal @Incubating
	EventListenerGroups getEventListenerGroups();

	/**
	 * Obtain the {@link ParameterMarkerStrategy} service.
	 *
	 * @since 7.0
	 */
	@Incubating
	ParameterMarkerStrategy getParameterMarkerStrategy();

	/**
	 * Obtain the {@link JdbcServices} service.
	 *
	 * @since 7.0
	 */
	@Incubating
	JdbcValuesMappingProducerProvider getJdbcValuesMappingProducerProvider();

	/**
	 * Obtain the {@link EntityCopyObserverFactory} service.
	 *
	 * @since 7.0
	 */
	@Incubating
	EntityCopyObserverFactory getEntityCopyObserver();

	/**
	 * Obtain the {@link ClassLoaderService}.
	 *
	 * @since 7.0
	 */
	@Incubating
	ClassLoaderService getClassLoaderService();

	/**
	 * Obtain the {@link ManagedBeanRegistry} service.
	 *
	 * @since 7.0
	 */
	@Incubating
	ManagedBeanRegistry getManagedBeanRegistry();

	/**
	 * Obtain the {@link EventListenerRegistry} service.
	 *
	 * @since 7.0
	 */
	@Incubating
	EventListenerRegistry getEventListenerRegistry();

	/**
	 * Return an instance of {@link WrapperOptions} which is not backed by a session,
	 * and whose functionality is therefore incomplete.
	 *
	 * @apiNote Avoid using this operation.
	 */
	WrapperOptions getWrapperOptions();

	/**
	 * Get the {@linkplain SessionFactoryOptions options} used to build this factory.
	 */
	@Override
	SessionFactoryOptions getSessionFactoryOptions();

	/**
	 * Obtain the {@linkplain FilterDefinition definition of a filter} by name.
	 *
	 * @param filterName The name of a declared filter
	 */
	@Override
	FilterDefinition getFilterDefinition(String filterName);

	/**
	 * Obtain a collection of {@link FilterDefinition}s representing all the
	 * {@linkplain org.hibernate.annotations.FilterDef#autoEnabled auto-enabled}
	 * filters.
	 */
	Collection<FilterDefinition> getAutoEnabledFilters();

	/**
	 * Obtain the {@link JdbcServices} service.
	 */
	JdbcServices getJdbcServices();

	/**
	 * Obtain the {@link SqlStringGenerationContext}.
	 */
	SqlStringGenerationContext getSqlStringGenerationContext();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// map these to Metamodel

	@Override
	RootGraphImplementor<?> findEntityGraphByName(String name);

	@Override
	default <T> RootGraphImplementor<T> createEntityGraph(Class<T> entityType) {
		return (RootGraphImplementor<T>) SessionFactory.super.createEntityGraph( entityType );
	}

	@Override
	RootGraph<Map<String, ?>> createGraphForDynamicEntity(String entityName);

	/**
	 * The best guess entity name for an entity not in an association
	 */
	String bestGuessEntityName(Object object);

}
