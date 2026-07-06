/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.action.queue.internal.support.ActionQueueFactoryService;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.monitor.internal.EmptyEventMonitor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.temporal.spi.ChangesetCoordinator;

import static org.hibernate.action.queue.internal.support.GraphBasedActionQueueFactory.buildPlanningOptions;

/// Builds standard-service-backed components used by SessionFactory
/// construction.
///
/// @since 9.0
/// @author Steve Ebersole
public final class StandardServiceComponentsBuilder {
	private StandardServiceComponentsBuilder() {
	}

	public static StandardServiceComponents build(SessionFactoryOptions options) {
		final var serviceRegistry = options.getServiceRegistry();
		final var classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		final var configurationService = serviceRegistry.requireService( ConfigurationService.class );
		final boolean multiTenancyEnabled = options.isMultiTenancyEnabled();
		return new StandardServiceComponents(
				serviceRegistry,
				configurationService,
				new EventEngine( options, serviceRegistry ),
				buildPlanningOptions( configurationService ),
				serviceRegistry.requireService( JdbcServices.class ),
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
