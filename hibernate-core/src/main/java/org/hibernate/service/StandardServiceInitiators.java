/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator;
import org.hibernate.boot.internal.DefaultSessionFactoryBuilderInitiator;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.bytecode.internal.ProxyFactoryFactoryInitiator;
import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jdbc.internal.SqlStatementLoggerInitiator;
import org.hibernate.engine.jdbc.mutation.internal.MutationExecutorServiceInitiator;
import org.hibernate.engine.jndi.internal.JndiServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformResolverInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.internal.util.cache.InternalCacheFactoryInitiator;
import org.hibernate.loader.ast.internal.BatchLoaderFactoryInitiator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.query.sqm.mutation.internal.SqmMultiTableMutationStrategyProviderInitiator;
import org.hibernate.resource.beans.spi.ManagedBeanRegistryInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyInitiator;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesMappingProducerProviderInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;
import org.hibernate.tool.schema.internal.script.SqlScriptExtractorInitiator;

import static java.util.Collections.unmodifiableList;

/**
 * Central definition of the standard set of service initiators defined by Hibernate.
 *
 * @author Steve Ebersole
 */
public final class StandardServiceInitiators {
	private StandardServiceInitiators() {
	}

	public static final List<StandardServiceInitiator<?>> LIST = buildStandardServiceInitiatorList();

	private static List<StandardServiceInitiator<?>> buildStandardServiceInitiatorList() {

		// Please do not rearrange - it's useful to maintain this particular order
		// so to simplify comparisons with custom initiator lists in other projects;
		// for example, we customize this list in Hibernate Reactive and Quarkus.

		final ArrayList<StandardServiceInitiator<?>> serviceInitiators = new ArrayList<>();

		// SessionFactoryBuilderService
		serviceInitiators.add( DefaultSessionFactoryBuilderInitiator.INSTANCE );

		// BytecodeProvider
		serviceInitiators.add( BytecodeProviderInitiator.INSTANCE );

		// ProxyFactoryFactory
		serviceInitiators.add( ProxyFactoryFactoryInitiator.INSTANCE );

		// CfgXmlAccessService
		serviceInitiators.add( CfgXmlAccessServiceInitiator.INSTANCE );

		// ConfigurationService
		serviceInitiators.add( ConfigurationServiceInitiator.INSTANCE );

		// PropertyAccessStrategyResolver
		serviceInitiators.add( PropertyAccessStrategyResolverInitiator.INSTANCE );

		// SqlScriptCommandExtractor
		serviceInitiators.add( SqlScriptExtractorInitiator.INSTANCE );

		// SchemaManagementTool
		serviceInitiators.add( SchemaManagementToolInitiator.INSTANCE );

		// JdbcEnvironment
		serviceInitiators.add( JdbcEnvironmentInitiator.INSTANCE );

		// JndiService
		serviceInitiators.add( JndiServiceInitiator.INSTANCE );

		// PersisterClassResolver
		serviceInitiators.add( PersisterClassResolverInitiator.INSTANCE );

		// PersisterFactory
		serviceInitiators.add( PersisterFactoryInitiator.INSTANCE );

		// ConnectionProvider
		serviceInitiators.add( ConnectionProviderInitiator.INSTANCE );

		// MultiTenantConnectionProvider
		serviceInitiators.add( MultiTenantConnectionProviderInitiator.INSTANCE );

		// DialectResolver
		serviceInitiators.add( DialectResolverInitiator.INSTANCE );

		// DialectFactory
		serviceInitiators.add( DialectFactoryInitiator.INSTANCE );

		// MutationExecutorService
		serviceInitiators.add( MutationExecutorServiceInitiator.INSTANCE );

		// BatchBuilder
		serviceInitiators.add( BatchBuilderInitiator.INSTANCE );

		// SqlStatementLoggerInitiator
		serviceInitiators.add( SqlStatementLoggerInitiator.INSTANCE );

		// JdbcServices
		serviceInitiators.add( JdbcServicesInitiator.INSTANCE );

		// RefCursorSupport
		serviceInitiators.add( RefCursorSupportInitiator.INSTANCE );

		// JtaPlatformResolver
		serviceInitiators.add( JtaPlatformResolverInitiator.INSTANCE );

		// JtaPlatform
		serviceInitiators.add( JtaPlatformInitiator.INSTANCE );

		// SessionFactoryServiceRegistryFactory
		serviceInitiators.add( SessionFactoryServiceRegistryFactoryInitiator.INSTANCE );

		// RegionFactory
		serviceInitiators.add( RegionFactoryInitiator.INSTANCE );

		// TransactionCoordinatorBuilder
		serviceInitiators.add( TransactionCoordinatorBuilderInitiator.INSTANCE );

		// ManagedBeanRegistry
		serviceInitiators.add( ManagedBeanRegistryInitiator.INSTANCE );

		// EntityCopyObserverFactory
		serviceInitiators.add( EntityCopyObserverFactoryInitiator.INSTANCE );

		// JdbcValuesMappingProducerProvider
		serviceInitiators.add( JdbcValuesMappingProducerProviderInitiator.INSTANCE );

		// SqmMultiTableMutationStrategyProvider
		serviceInitiators.add( SqmMultiTableMutationStrategyProviderInitiator.INSTANCE );

		// ParameterMarkerStrategy
		serviceInitiators.add( ParameterMarkerStrategyInitiator.INSTANCE );
		serviceInitiators.add( BatchLoaderFactoryInitiator.INSTANCE );

		// InternalCacheFactoryService
		serviceInitiators.add( InternalCacheFactoryInitiator.INSTANCE );

		serviceInitiators.trimToSize();

		return unmodifiableList( serviceInitiators );
	}
}
