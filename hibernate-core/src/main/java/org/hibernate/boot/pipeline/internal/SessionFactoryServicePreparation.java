/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.action.queue.internal.support.ActionQueueFactoryService;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.monitor.internal.EmptyEventMonitor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.temporal.spi.ChangesetCoordinator;

import static org.hibernate.action.queue.internal.support.GraphBasedActionQueueFactory.buildPlanningOptions;

/// Internal handoff for values sourced from the SessionFactory service registry.
///
/// This is intentionally a construction parameter object, not retained runtime
/// state.  It keeps the service-registry construction and safe service lookups
/// out of `SessionFactoryImpl` while the constructor is being slimmed.
///
/// @since 9.0
/// @author Steve Ebersole
public record SessionFactoryServicePreparation(
		SessionFactoryServiceRegistry serviceRegistry,
		EventEngine eventEngine,
		PlanningOptions graphPlanningOptions,
		JdbcServices jdbcServices,
		CacheImplementor cacheAccess,
		ClassLoaderService classLoaderService,
		JdbcValuesMappingProducerProvider jdbcValuesMappingProducerProvider,
		ChangesetCoordinator changesetCoordinator,
		TransactionCoordinatorBuilder transactionCoordinatorBuilder,
		EntityCopyObserverFactory entityCopyObserverFactory,
		ParameterMarkerStrategy parameterMarkerStrategy,
		BatchBuilder batchBuilder,
		ManagedBeanRegistry managedBeanRegistry,
		ConnectionProvider connectionProvider,
		MultiTenantConnectionProvider<Object> multiTenantConnectionProvider,
		EventMonitor eventMonitor,
		ActionQueueFactoryService actionQueueFactoryService) {

	public static SessionFactoryServicePreparation prepare(
			SessionFactoryOptions options,
			SessionFactoryImplementor sessionFactory) {
		final var serviceRegistry = options.getServiceRegistry()
				.requireService( SessionFactoryServiceRegistryFactory.class )
				// Later migration target: avoid passing an incompletely
				// initialized SessionFactory instance into service-registry creation.
				.buildServiceRegistry( sessionFactory, options );
		final var classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		final boolean multiTenancyEnabled = options.isMultiTenancyEnabled();
		return new SessionFactoryServicePreparation(
				serviceRegistry,
				new EventEngine( options, serviceRegistry ),
				buildPlanningOptions( serviceRegistry.requireService( ConfigurationService.class ) ),
				serviceRegistry.requireService( JdbcServices.class ),
				serviceRegistry.getService( CacheImplementor.class ),
				classLoaderService,
				serviceRegistry.requireService( JdbcValuesMappingProducerProvider.class ),
				serviceRegistry.requireService( ChangesetCoordinator.class ),
				serviceRegistry.requireService( TransactionCoordinatorBuilder.class ),
				serviceRegistry.requireService( EntityCopyObserverFactory.class ),
				serviceRegistry.requireService( ParameterMarkerStrategy.class ),
				serviceRegistry.requireService( BatchBuilder.class ),
				serviceRegistry.getService( ManagedBeanRegistry.class ),
				multiTenancyEnabled ? null : serviceRegistry.requireService( ConnectionProvider.class ),
				multiTenancyEnabled ? serviceRegistry.requireService( MultiTenantConnectionProvider.class ) : null,
				loadEventMonitor( classLoaderService ),
				serviceRegistry.requireService( ActionQueueFactoryService.class )
		);
	}

	private static EventMonitor loadEventMonitor(ClassLoaderService classLoaderService) {
		final var eventMonitors = classLoaderService.loadJavaServices( EventMonitor.class );
		return eventMonitors.isEmpty() ? new EmptyEventMonitor() : eventMonitors.iterator().next();
	}
}
