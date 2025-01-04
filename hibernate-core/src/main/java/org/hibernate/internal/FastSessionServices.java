/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.monitor.internal.EmptyEventMonitor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;

import java.util.Collection;

/**
 * Internal component.
 * <p>
 * Collects services that any Session implementation will likely need for faster access
 * and reduced allocations. Conceptually this acts as an immutable caching intermediary
 * between Session and ServiceRegistry.
 * <p>
 * Designed to be immutable, shared across Session instances, and created infrequently,
 * possibly only once per SessionFactory.
 * <p>
 * If the Session is requiring to retrieve (or compute) anything from the SessionFactory,
 * and this computation would result in the same outcome for any Session created on this
 * same SessionFactory, then it belongs in a final field of this class.
 * <p>
 * Finally, consider also limiting the size of each Session: some fields could be good
 * candidates to be replaced with access via this object.
 *
 * @author Sanne Grinovero
 */
public final class FastSessionServices extends EventListenerGroups {

	private final ConnectionProvider connectionProvider;
	private final MultiTenantConnectionProvider<Object> multiTenantConnectionProvider;
	private final ClassLoaderService classLoaderService;
	private final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final EventMonitor eventMonitor;
	private final JdbcServices jdbcServices;
	private final EntityCopyObserverFactory entityCopyObserverFactory;
	private final BatchBuilder batchBuilder;
	private final ParameterMarkerStrategy parameterMarkerStrategy;
	private final JdbcValuesMappingProducerProvider jdbcValuesMappingProducerProvider;

	FastSessionServices(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory.getServiceRegistry() );
		final ServiceRegistry serviceRegistry = sessionFactory.getServiceRegistry();

		//Some "hot" services:
		this.classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		this.transactionCoordinatorBuilder = serviceRegistry.getService( TransactionCoordinatorBuilder.class );
		this.jdbcServices = serviceRegistry.requireService( JdbcServices.class );
		this.entityCopyObserverFactory = serviceRegistry.requireService( EntityCopyObserverFactory.class );
		this.jdbcValuesMappingProducerProvider = serviceRegistry.getService( JdbcValuesMappingProducerProvider.class );
		this.parameterMarkerStrategy = serviceRegistry.getService( ParameterMarkerStrategy.class );
		this.batchBuilder = serviceRegistry.getService( BatchBuilder.class );

		final boolean multiTenancyEnabled = sessionFactory.getSessionFactoryOptions().isMultiTenancyEnabled();
		this.connectionProvider =
				multiTenancyEnabled ? null : serviceRegistry.getService( ConnectionProvider.class );
		this.multiTenantConnectionProvider =
				multiTenancyEnabled ? serviceRegistry.requireService( MultiTenantConnectionProvider.class ) : null;

		final Collection<EventMonitor> eventMonitors = classLoaderService.loadJavaServices( EventMonitor.class );
		this.eventMonitor = eventMonitors.isEmpty() ? new EmptyEventMonitor() : eventMonitors.iterator().next();
	}

	public JdbcValuesMappingProducerProvider getJdbcValuesMappingProducerProvider() {
		return jdbcValuesMappingProducerProvider;
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public MultiTenantConnectionProvider<Object> getMultiTenantConnectionProvider() {
		return multiTenantConnectionProvider;
	}

	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return transactionCoordinatorBuilder;
	}

	public EventMonitor getEventMonitor() {
		return eventMonitor;
	}

	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	public EntityCopyObserverFactory getEntityCopyObserverFactory() {
		return entityCopyObserverFactory;
	}

	public BatchBuilder getBatchBuilder() {
		return batchBuilder;
	}

	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return parameterMarkerStrategy;
	}
}
