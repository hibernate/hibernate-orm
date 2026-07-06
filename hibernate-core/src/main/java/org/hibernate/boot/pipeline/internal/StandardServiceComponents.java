/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal;

import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.action.queue.internal.support.ActionQueueFactoryService;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.temporal.spi.ChangesetCoordinator;

/// Internal handoff for values sourced from the standard service registry.
///
/// This is intentionally a construction parameter object, not retained runtime
/// state.  It keeps safe service lookups out of `SessionFactoryImpl` while the
/// constructor is being slimmed.
///
/// @since 9.0
/// @author Steve Ebersole
public record StandardServiceComponents(
		StandardServiceRegistry serviceRegistry,
		ConfigurationService configurationService,
		EventEngine eventEngine,
		PlanningOptions graphPlanningOptions,
		JdbcServices jdbcServices,
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
}
