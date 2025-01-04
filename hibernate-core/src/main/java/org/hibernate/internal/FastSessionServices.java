/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

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
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
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
public class FastSessionServices extends EventListenerGroups {

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
	private final ManagedBeanRegistry managedBeanRegistry;
	private final ConfigurationService configurationService;

	FastSessionServices(ServiceRegistry serviceRegistry, SessionFactoryOptions factoryOptions) {
		super( serviceRegistry );

		//Some "hot" services:
		classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		transactionCoordinatorBuilder = serviceRegistry.getService( TransactionCoordinatorBuilder.class );
		jdbcServices = serviceRegistry.requireService( JdbcServices.class );
		entityCopyObserverFactory = serviceRegistry.requireService( EntityCopyObserverFactory.class );
		jdbcValuesMappingProducerProvider = serviceRegistry.getService( JdbcValuesMappingProducerProvider.class );
		parameterMarkerStrategy = serviceRegistry.getService( ParameterMarkerStrategy.class );
		batchBuilder = serviceRegistry.getService( BatchBuilder.class );
		managedBeanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );
		configurationService = serviceRegistry.getService( ConfigurationService.class );

		final boolean multiTenancyEnabled = factoryOptions.isMultiTenancyEnabled();
		connectionProvider =
				multiTenancyEnabled ? null : serviceRegistry.getService( ConnectionProvider.class );
		multiTenantConnectionProvider =
				multiTenancyEnabled ? serviceRegistry.requireService( MultiTenantConnectionProvider.class ) : null;

		final Collection<EventMonitor> eventMonitors = classLoaderService.loadJavaServices( EventMonitor.class );
		eventMonitor = eventMonitors.isEmpty() ? new EmptyEventMonitor() : eventMonitors.iterator().next();
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

	public ManagedBeanRegistry getManagedBeanRegistry() {
		return managedBeanRegistry;
	}

	public ConfigurationService getConfigurationService() {
		return configurationService;
	}
}
