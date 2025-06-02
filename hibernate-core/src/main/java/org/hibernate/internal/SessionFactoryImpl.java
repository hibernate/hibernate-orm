/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import jakarta.persistence.TypedQuery;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.UnknownFilterException;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.cfg.internal.DomainDataRegionConfigImpl;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.event.monitor.internal.EmptyEventMonitor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.generator.Generator;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.jpa.internal.ExceptionMapperLegacyJpaImpl;
import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.RuntimeMetamodelsImpl;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.internal.QueryEngineImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.internal.SqlTranslationEngineImpl;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.relational.SchemaManager;
import org.hibernate.relational.internal.SchemaManagerImpl;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQueryReference;

import static jakarta.persistence.SynchronizationType.SYNCHRONIZED;
import static java.util.Collections.addAll;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.cfg.AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS;
import static org.hibernate.internal.FetchProfileHelper.addFetchProfiles;
import static org.hibernate.internal.SessionFactorySettings.deprecationCheck;
import static org.hibernate.internal.SessionFactorySettings.determineJndiName;
import static org.hibernate.internal.SessionFactorySettings.getSessionFactoryName;
import static org.hibernate.internal.SessionFactorySettings.getSettings;
import static org.hibernate.internal.SessionFactorySettings.maskOutSensitiveInformation;
import static org.hibernate.jpa.HibernateHints.HINT_TENANT_ID;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;
import static org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;

/**
 * Concrete implementation of the {@link SessionFactory} API.
 * <p>
 * Exposes two interfaces:
 * <ul>
 * <li>{@link SessionFactory} to the application, and
 * <li>{@link SessionImplementor} (an SPI interface) to other subsystems.
 * </ul>
 * <p>
 * This class is thread-safe.
 *
 * @implNote This class must appear immutable to clients, even if it does
 *           all kinds of caching and pooling under the covers. It is crucial
 *           that the class is not only thread-safe, but also highly concurrent.
 *           Synchronization must be used extremely sparingly.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class SessionFactoryImpl implements SessionFactoryImplementor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SessionFactoryImpl.class );

	private final String name;
	private final String jndiName;
	private final String uuid;

	private transient volatile Status status = Status.OPEN;

	private final transient SessionFactoryObserverChain observer = new SessionFactoryObserverChain();

	private final transient SessionFactoryOptions sessionFactoryOptions;
	private final transient Map<String,Object> settings;

	private final transient SessionFactoryServiceRegistry serviceRegistry;
	private final transient EventEngine eventEngine;
	private final transient JdbcServices jdbcServices;
	private final transient SqlStringGenerationContext sqlStringGenerationContext;

	private final transient RuntimeMetamodelsImplementor runtimeMetamodels;
	private final PersistenceUnitUtil jpaPersistenceUnitUtil;
	private final transient CacheImplementor cacheAccess;
	private final transient QueryEngine queryEngine;
	private final transient SqlTranslationEngine sqlTranslationEngine;
	private final transient TypeConfiguration typeConfiguration;

	private final transient CurrentSessionContext currentSessionContext;

	private final transient Map<String, FilterDefinition> filters;
	private final transient java.util.Collection<FilterDefinition> autoEnabledFilters = new HashSet<>();
	private final transient JavaType<Object> tenantIdentifierJavaType;

	private final transient EventListenerGroups eventListenerGroups;

	private final transient WrapperOptions wrapperOptions;
	private final transient SessionBuilderImpl defaultSessionOpenOptions;
	private final transient SessionBuilderImpl temporarySessionOpenOptions;
	private final transient StatelessSessionBuilder defaultStatelessOptions;
	private final transient EntityNameResolver entityNameResolver;

	private final transient SchemaManager schemaManager;

	final transient ClassLoaderService classLoaderService;
	final transient TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	final transient ConnectionProvider connectionProvider;
	final transient MultiTenantConnectionProvider<Object> multiTenantConnectionProvider;
	final transient ManagedBeanRegistry managedBeanRegistry;
	final transient BatchBuilder batchBuilder;
	final transient EventMonitor eventMonitor;
	final transient EntityCopyObserverFactory entityCopyObserverFactory;
	final transient ParameterMarkerStrategy parameterMarkerStrategy;
	final transient JdbcValuesMappingProducerProvider jdbcValuesMappingProducerProvider;

	public SessionFactoryImpl(
			final MetadataImplementor bootMetamodel,
			final SessionFactoryOptions options,
			final BootstrapContext bootstrapContext) {
		LOG.debug( "Building session factory" );
		typeConfiguration = bootstrapContext.getTypeConfiguration();

		sessionFactoryOptions = options;

		serviceRegistry = getServiceRegistry( options, this );
		eventEngine = new EventEngine( bootMetamodel, this );

		bootMetamodel.initSessionFactory( this );

		name = getSessionFactoryName( options, serviceRegistry );
		jndiName = determineJndiName( name, options, serviceRegistry );
		uuid = options.getUuid();

		jdbcServices = serviceRegistry.requireService( JdbcServices.class );

		settings = getSettings( options, serviceRegistry );
		maskOutSensitiveInformation( settings );
		deprecationCheck( settings );
		LOG.instantiatingFactory( settings );

		sqlStringGenerationContext = createSqlStringGenerationContext( bootMetamodel, options, jdbcServices );

		cacheAccess = serviceRegistry.getService( CacheImplementor.class );

		jpaPersistenceUnitUtil = new PersistenceUnitUtilImpl( this );

		for ( SessionFactoryObserver sessionFactoryObserver : options.getSessionFactoryObservers() ) {
			observer.addObserver( sessionFactoryObserver );
		}

		filters = new HashMap<>( bootMetamodel.getFilterDefinitions() );

		final FilterDefinition tenantFilter = filters.get( TenantIdBinder.FILTER_NAME );
		if ( tenantFilter == null ) {
			tenantIdentifierJavaType = options.getDefaultTenantIdentifierJavaType();
		}
		else {
			final JdbcMapping jdbcMapping = tenantFilter.getParameterJdbcMapping( TenantIdBinder.PARAMETER_NAME );
			assert jdbcMapping != null;
			//noinspection unchecked
			tenantIdentifierJavaType = jdbcMapping.getJavaTypeDescriptor();
		}
		for ( Map.Entry<String, FilterDefinition> filterEntry : filters.entrySet() ) {
			if ( filterEntry.getValue().isAutoEnabled() ) {
				autoEnabledFilters.add( filterEntry.getValue() );
			}
		}

		entityNameResolver = new CoordinatingEntityNameResolver( this, getInterceptor() );
		schemaManager = new SchemaManagerImpl( this, bootMetamodel );

		// used for initializing the MappingMetamodelImpl
		classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		jdbcValuesMappingProducerProvider = serviceRegistry.requireService( JdbcValuesMappingProducerProvider.class );

		final IntegratorObserver integratorObserver = new IntegratorObserver();
		observer.addObserver( integratorObserver );
		try {
			integrate( bootMetamodel, bootstrapContext, integratorObserver );

			bootMetamodel.orderColumns( false );
			bootMetamodel.validate();

			primeSecondLevelCacheRegions( bootMetamodel );

			// create the empty runtime metamodels object
			final RuntimeMetamodelsImpl runtimeMetamodelsImpl = new RuntimeMetamodelsImpl( typeConfiguration );
			runtimeMetamodels = runtimeMetamodelsImpl;

			// we build this before creating the runtime metamodels
			// because the SqlAstTranslators (unnecessarily, perhaps)
			// use the SqmFunctionRegistry when rendering SQL for Loaders
			queryEngine = new QueryEngineImpl( bootMetamodel, options, runtimeMetamodels, serviceRegistry, settings, name );
			final Map<String, FetchProfile> fetchProfiles = new HashMap<>();
			sqlTranslationEngine = new SqlTranslationEngineImpl( this, typeConfiguration, fetchProfiles );

			// now actually create the mapping and JPA metamodels
			final MappingMetamodelImpl mappingMetamodelImpl = new MappingMetamodelImpl( typeConfiguration, serviceRegistry );
			runtimeMetamodelsImpl.setMappingMetamodel( mappingMetamodelImpl );
			mappingMetamodelImpl.finishInitialization(
					new ModelCreationContext( bootstrapContext, bootMetamodel, mappingMetamodelImpl, typeConfiguration ) );
			runtimeMetamodelsImpl.setJpaMetamodel( mappingMetamodelImpl.getJpaMetamodel() );

			// this needs to happen after the mapping metamodel is
			// completely built, since we need to use the persisters
			addFetchProfiles( bootMetamodel, runtimeMetamodelsImpl, fetchProfiles );

			defaultSessionOpenOptions = createDefaultSessionOpenOptionsIfPossible();
			temporarySessionOpenOptions = defaultSessionOpenOptions == null ? null : buildTemporarySessionOpenOptions();
			defaultStatelessOptions = defaultSessionOpenOptions == null ? null : withStatelessOptions();

			wrapperOptions = new SessionFactoryBasedWrapperOptions( this );

			currentSessionContext = buildCurrentSessionContext();

			// cache references to some "hot" services:
			transactionCoordinatorBuilder = serviceRegistry.requireService( TransactionCoordinatorBuilder.class );
			entityCopyObserverFactory = serviceRegistry.requireService( EntityCopyObserverFactory.class );
			parameterMarkerStrategy = serviceRegistry.requireService( ParameterMarkerStrategy.class );
			batchBuilder = serviceRegistry.requireService( BatchBuilder.class );
			managedBeanRegistry = serviceRegistry.getService( ManagedBeanRegistry.class );

			final boolean multiTenancyEnabled = options.isMultiTenancyEnabled();
			connectionProvider =
					multiTenancyEnabled ? null : serviceRegistry.requireService( ConnectionProvider.class );
			multiTenantConnectionProvider =
					multiTenancyEnabled ? serviceRegistry.requireService( MultiTenantConnectionProvider.class ) : null;

			eventMonitor = loadEventMonitor();

			eventListenerGroups = new EventListenerGroups( serviceRegistry );

			// re-scope the TypeConfiguration to this SessionFactory,
			// now that we are (almost) fully-initialized ... note,
			// we could have done this earlier, but then it's hard to
			// really know or control who's calling back to us while
			// we're in an incompletely-initialized state
			typeConfiguration.scope( this );

			observer.sessionFactoryCreated( this );
		}
		catch ( Exception e ) {
			disintegrate( e, integratorObserver );

			try {
				close();
			}
			catch (Exception closeException) {
				LOG.trace( "Eating error closing factory after failed instantiation" );
			}
			throw e;
		}

		LOG.debug( "Instantiated factory" );
	}

	private EventMonitor loadEventMonitor() {
		final var eventMonitors = classLoaderService.loadJavaServices( EventMonitor.class );
		return eventMonitors.isEmpty() ? new EmptyEventMonitor() : eventMonitors.iterator().next();
	}

	private static SqlStringGenerationContext createSqlStringGenerationContext(
			MetadataImplementor bootMetamodel,
			SessionFactoryOptions options,
			JdbcServices jdbcServices) {
		return SqlStringGenerationContextImpl.fromExplicit(
				jdbcServices.getJdbcEnvironment(),
				bootMetamodel.getDatabase(),
				options.getDefaultCatalog(),
				options.getDefaultSchema()
		);
	}

	private static SessionFactoryServiceRegistry getServiceRegistry(
			SessionFactoryOptions options,
			SessionFactoryImplementor self) {
		return options.getServiceRegistry()
				.requireService( SessionFactoryServiceRegistryFactory.class )
				// it is not great how we pass a reference to
				// an incompletely-initialized instance here:
				.buildServiceRegistry( self, options );
	}

	@Override
	public EventListenerGroups getEventListenerGroups() {
		return eventListenerGroups;
	}

	@Override
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return parameterMarkerStrategy;
	}

	@Override
	public JdbcValuesMappingProducerProvider getJdbcValuesMappingProducerProvider() {
		return jdbcValuesMappingProducerProvider;
	}

	@Override
	public EntityCopyObserverFactory getEntityCopyObserver() {
		return entityCopyObserverFactory;
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return managedBeanRegistry;
	}

	@Override
	public EventListenerRegistry getEventListenerRegistry() {
		return eventEngine.getListenerRegistry();
	}

	class IntegratorObserver implements SessionFactoryObserver {
		private final ArrayList<Integrator> integrators = new ArrayList<>();
		@Override
		public void sessionFactoryClosed(SessionFactory factory) {
			for ( Integrator integrator : integrators ) {
				integrator.disintegrate( SessionFactoryImpl.this, SessionFactoryImpl.this.serviceRegistry );
			}
			integrators.clear();
		}
	}

	private void integrate(MetadataImplementor bootMetamodel, BootstrapContext bootstrapContext, IntegratorObserver integratorObserver) {
		for ( Integrator integrator : serviceRegistry.requireService( IntegratorService.class ).getIntegrators() ) {
			integrator.integrate( bootMetamodel, bootstrapContext, this );
			integratorObserver.integrators.add( integrator );
		}
	}

	private void disintegrate(Exception startupException, IntegratorObserver integratorObserver) {
		for ( Integrator integrator : integratorObserver.integrators ) {
			try {
				integrator.disintegrate( this, serviceRegistry );
			}
			catch (Throwable ex) {
				startupException.addSuppressed( ex );
			}
		}
		integratorObserver.integrators.clear();
	}


	private SessionBuilderImpl createDefaultSessionOpenOptionsIfPossible() {
		final CurrentTenantIdentifierResolver<Object> tenantIdResolver = getCurrentTenantIdentifierResolver();
		if ( tenantIdResolver == null ) {
			return withOptions();
		}
		else {
			//Don't store a default SessionBuilder when a CurrentTenantIdentifierResolver is provided
			return null;
		}
	}

	private SessionBuilderImpl buildTemporarySessionOpenOptions() {
		return withOptions()
				.autoClose( false )
				.flushMode( FlushMode.MANUAL )
				.connectionHandlingMode( DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT );
	}

	private void primeSecondLevelCacheRegions(MetadataImplementor mappingMetadata) {
		final Map<String, DomainDataRegionConfigImpl.Builder> regionConfigBuilders = new ConcurrentHashMap<>();

		// TODO: ultimately this code can be made more efficient when we have
		//       a better intrinsic understanding of the hierarchy as a whole

		for ( PersistentClass bootEntityDescriptor : mappingMetadata.getEntityBindings() ) {
			final AccessType accessType =
					AccessType.fromExternalName( bootEntityDescriptor.getCacheConcurrencyStrategy() );

			if ( accessType != null ) {
				if ( bootEntityDescriptor.isCached() ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getRootClass().getCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addEntityConfig( bootEntityDescriptor, accessType );
				}

				if ( bootEntityDescriptor instanceof RootClass rootClass
						&& bootEntityDescriptor.hasNaturalId()
						&& bootEntityDescriptor.getNaturalIdCacheRegionName() != null ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getNaturalIdCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addNaturalIdConfig( rootClass, accessType );
				}
			}
		}

		for ( Collection collection : mappingMetadata.getCollectionBindings() ) {
			final AccessType accessType = AccessType.fromExternalName( collection.getCacheConcurrencyStrategy() );
			if ( accessType != null ) {
				regionConfigBuilders.computeIfAbsent(
						collection.getCacheRegionName(),
						DomainDataRegionConfigImpl.Builder::new
				)
						.addCollectionConfig( collection, accessType );
			}
		}

		final Set<DomainDataRegionConfig> regionConfigs;
		if ( regionConfigBuilders.isEmpty() ) {
			regionConfigs = emptySet();
		}
		else {
			regionConfigs = new HashSet<>();
			for ( DomainDataRegionConfigImpl.Builder builder : regionConfigBuilders.values() ) {
				regionConfigs.add( builder.build() );
			}
		}

		getCache().prime( regionConfigs );
	}

	@Override
	public SessionImplementor openSession() {
		// The defaultSessionOpenOptions can't be used in some cases;
		// for example when using a TenantIdentifierResolver.
		return defaultSessionOpenOptions != null
				? defaultSessionOpenOptions.openSession()
				: withOptions().openSession();
	}

	@Override
	public SessionImpl openTemporarySession() {
		// The temporarySessionOpenOptions can't be used in some cases;
		// for example when using a TenantIdentifierResolver.
		return temporarySessionOpenOptions != null
				? temporarySessionOpenOptions.openSession()
				: buildTemporarySessionOpenOptions().openSession();
	}

	@Override
	public Session getCurrentSession() {
		if ( currentSessionContext == null ) {
			throw new HibernateException( "No CurrentSessionContext configured" );
		}
		return currentSessionContext.currentSession();
	}

	@Override
	public SessionBuilderImpl withOptions() {
		return new SessionBuilderImpl( this );
	}

	@Override
	public StatelessSessionBuilder withStatelessOptions() {
		return new StatelessSessionBuilderImpl( this );
	}

	@Override
	public StatelessSession openStatelessSession() {
		return defaultStatelessOptions != null
				? defaultStatelessOptions.openStatelessSession()
				: withStatelessOptions().openStatelessSession();
	}

	@Override
	public StatelessSession openStatelessSession(Connection connection) {
		return withStatelessOptions().connection( connection ).openStatelessSession();
	}

	@Override
	public void addObserver(SessionFactoryObserver observer) {
		this.observer.addObserver( observer );
	}

	@Override
	public Map<String, Object> getProperties() {
		validateNotClosed();
		return settings;
	}

	protected void validateNotClosed() {
		if ( status == Status.CLOSED ) {
			throw new IllegalStateException( "EntityManagerFactory is closed" );
		}
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getJndiName() {
		return jndiName;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public QueryEngine getQueryEngine() {
		return queryEngine;
	}

	@Override
	public SqlTranslationEngine getSqlTranslationEngine() {
		return sqlTranslationEngine;
	}

	@Override
	public EventEngine getEventEngine() {
		return eventEngine;
	}

	@Override
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	public SqlStringGenerationContext getSqlStringGenerationContext() {
		return sqlStringGenerationContext;
	}

	@Override
	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		return getJpaMetamodel().findEntityGraphsByJavaType( entityClass );
	}

	// todo : (5.2) review synchronizationType, persistenceContextType, transactionType usage

	@Override
	public Session createEntityManager() {
		validateNotClosed();
		return buildEntityManager( SYNCHRONIZED, null );
	}

	private <K,V> Session buildEntityManager(final SynchronizationType synchronizationType, final Map<K,V> map) {
		assert status != Status.CLOSED;

		SessionBuilderImplementor builder = withOptions();
		builder.autoJoinTransactions( synchronizationType == SYNCHRONIZED );

		if ( map != null ) {
			//noinspection SuspiciousMethodCalls
			final Object tenantIdHint = map.get( HINT_TENANT_ID );
			if ( tenantIdHint != null ) {
				builder = (SessionBuilderImplementor) builder.tenantIdentifier( tenantIdHint );
			}
		}

		final Session session = builder.openSession();
		if ( map != null ) {
			for ( Map.Entry<K, V> o : map.entrySet() ) {
				final K key = o.getKey();
				if ( key instanceof String string ) {
					if ( !HINT_TENANT_ID.equals( string ) ) {
						session.setProperty( string, o.getValue() );
					}
				}
			}
		}
		return session;
	}

	@Override @SuppressWarnings("unchecked")
	public Session createEntityManager(Map map) {
		validateNotClosed();
		return buildEntityManager( SYNCHRONIZED, map );
	}

	@Override
	public Session createEntityManager(SynchronizationType synchronizationType) {
		validateNotClosed();
		errorIfResourceLocalDueToExplicitSynchronizationType();
		return buildEntityManager( synchronizationType, null );
	}

	private void errorIfResourceLocalDueToExplicitSynchronizationType() {
		// JPA requires that we throw IllegalStateException in cases where:
		//		1) the PersistenceUnitTransactionType (TransactionCoordinator) is non-JTA
		//		2) an explicit SynchronizationType is specified
		if ( !transactionCoordinatorBuilder.isJta() ) {
			throw new IllegalStateException(
					"Illegal attempt to specify a SynchronizationType when building an EntityManager from an " +
							"EntityManagerFactory defined as RESOURCE_LOCAL (as opposed to JTA)"
			);
		}
	}

	@Override @SuppressWarnings("unchecked")
	public Session createEntityManager(SynchronizationType synchronizationType, Map map) {
		validateNotClosed();
		errorIfResourceLocalDueToExplicitSynchronizationType();
		return buildEntityManager( synchronizationType, map );
	}

	@Override
	public NodeBuilder getCriteriaBuilder() {
		validateNotClosed();
		return queryEngine.getCriteriaBuilder();
	}

	@Override
	public MappingMetamodel getMetamodel() {
		validateNotClosed();
		return runtimeMetamodels.getMappingMetamodel();
	}

	@Override
	public boolean isOpen() {
		return status != Status.CLOSED;
	}

	@Override
	public RootGraph<Map<String, ?>> createGraphForDynamicEntity(String entityName) {
		final EntityDomainType<?> entity = getJpaMetamodel().entity( entityName );
		if ( entity.getRepresentationMode() != RepresentationMode.MAP ) {
			throw new IllegalArgumentException( "Entity '" + entityName + "' is not a dynamic entity" );
		}
		@SuppressWarnings("unchecked") //Safe, because we just checked
		final EntityDomainType<Map<String, ?>> dynamicEntity = (EntityDomainType<Map<String, ?>>) entity;
		return new RootGraphImpl<>( null, dynamicEntity );
	}

	@Override
	public RootGraphImplementor<?> findEntityGraphByName(String name) {
		return getJpaMetamodel().findEntityGraphByName( name );
	}

	@Override
	public String bestGuessEntityName(Object object) {
		final LazyInitializer initializer = extractLazyInitializer( object );
		if ( initializer != null ) {
			// it is possible for this method to be called during flush processing,
			// so make certain that we do not accidentally initialize an uninitialized proxy
			if ( initializer.isUninitialized() ) {
				return initializer.getEntityName();
			}
			object = initializer.getImplementation();
		}
		return entityNameResolver.resolveEntityName( object );
	}

	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return sessionFactoryOptions;
	}

	public Interceptor getInterceptor() {
		return sessionFactoryOptions.getInterceptor();
	}

	@Override
	public Reference getReference() {
		// from javax.naming.Referenceable
		LOG.debug( "Returning a Reference to the factory" );
		return new Reference(
				SessionFactoryImpl.class.getName(),
				new StringRefAddr( "uuid", getUuid() ),
				SessionFactoryRegistry.ObjectFactoryImpl.class.getName(),
				null
		);
	}

	/**
	 * Closes the session factory, releasing all held resources.
	 *
	 * <ol>
	 * <li>cleans up used cache regions and "stops" the cache provider.
	 * <li>close the JDBC connection
	 * <li>remove the JNDI binding
	 * </ol>
	 *
	 * Note: Be aware that the sessionFactory instance still can
	 * be a "heavy" object memory wise after close() has been called.  Thus
	 * it is important to not keep referencing the instance to let the garbage
	 * collector release the memory.
	 */
	@Override
	public void close() {
		synchronized (this) {
			if ( status != Status.OPEN ) {
				if ( getSessionFactoryOptions().getJpaCompliance().isJpaClosedComplianceEnabled() ) {
					throw new IllegalStateException( "EntityManagerFactory is already closed" );
				}

				LOG.trace( "Already closed" );
				return;
			}

			status = Status.CLOSING;
		}

		try {
			LOG.closingFactory();
			observer.sessionFactoryClosing( this );

		// NOTE : the null checks below handle cases where close is called from
		//		a failed attempt to create the SessionFactory

			if ( cacheAccess != null ) {
				cacheAccess.close();
			}

			if ( runtimeMetamodels != null && runtimeMetamodels.getMappingMetamodel() != null ) {
				final JdbcConnectionAccess jdbcConnectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
				runtimeMetamodels.getMappingMetamodel().forEachEntityDescriptor(
						entityPersister -> {
							if ( entityPersister.getSqmMultiTableMutationStrategy() != null ) {
								entityPersister.getSqmMultiTableMutationStrategy()
										.release( this, jdbcConnectionAccess );
							}
							if ( entityPersister.getSqmMultiTableInsertStrategy() != null ) {
								entityPersister.getSqmMultiTableInsertStrategy()
										.release( this, jdbcConnectionAccess );
							}
						}
				);
//				runtimeMetamodels.getMappingMetamodel().close();
			}

			if ( queryEngine != null ) {
				queryEngine.close();
			}
			if ( eventEngine != null ) {
				eventEngine.stop();
			}
		}
		finally {
			status = Status.CLOSED;
		}

		observer.sessionFactoryClosed( this );
		serviceRegistry.destroy();
	}

	@Override
	public CacheImplementor getCache() {
		validateNotClosed();
		return cacheAccess;
	}

	@Override
	public PersistenceUnitUtil getPersistenceUnitUtil() {
		validateNotClosed();
		return jpaPersistenceUnitUtil;
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionCoordinatorBuilder.isJta()
				? PersistenceUnitTransactionType.JTA
				: PersistenceUnitTransactionType.RESOURCE_LOCAL;

	}

	private NamedObjectRepository getNamedObjectRepository() {
		validateNotClosed();
		return getQueryEngine().getNamedObjectRepository();
	}

	@Override
	public void addNamedQuery(String name, Query query) {
		getNamedObjectRepository().registerNamedQuery( name, query );
	}

	@Override
	public <R> TypedQueryReference<R> addNamedQuery(String name, TypedQuery<R> query) {
		return getNamedObjectRepository().registerNamedQuery( name, query );
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		if ( type.isInstance( this ) ) {
			return type.cast( this );
		}

		if ( type.isInstance( serviceRegistry ) ) {
			return type.cast( serviceRegistry );
		}

		if ( type.isInstance( jdbcServices ) ) {
			return type.cast( jdbcServices );
		}

		if ( type.isInstance( cacheAccess ) ) {
			return type.cast( cacheAccess );
		}

		if ( type.isInstance( runtimeMetamodels ) ) {
			return type.cast( runtimeMetamodels );
		}

		if ( type.isInstance( runtimeMetamodels.getJpaMetamodel() ) ) {
			return type.cast( runtimeMetamodels.getJpaMetamodel() );
		}

		if ( type.isInstance( runtimeMetamodels.getMappingMetamodel() ) ) {
			return type.cast( runtimeMetamodels.getMappingMetamodel() );
		}

		if ( type.isInstance( queryEngine ) ) {
			return type.cast( queryEngine );
		}

		throw new PersistenceException( "Hibernate cannot unwrap EntityManagerFactory as '" + type.getName() + "'" );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		getMappingMetamodel().addNamedEntityGraph( graphName, (RootGraphImplementor<T>) entityGraph );
	}

	@Override
	public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
		return queryEngine.getNamedObjectRepository().getNamedQueries( resultType );
	}

	@Override
	public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityType) {
		return getJpaMetamodel().getNamedEntityGraphs( entityType );
	}

	@Override
	public void runInTransaction(Consumer<EntityManager> work) {
		inTransaction( work );
	}

	@Override
	public <R> R callInTransaction(Function<EntityManager, R> work) {
		return fromTransaction( work );
	}

	@Override
	public boolean isClosed() {
		return status == Status.CLOSED;
	}

	private transient StatisticsImplementor statistics;

	public StatisticsImplementor getStatistics() {
		if ( statistics == null ) {
			statistics = serviceRegistry.requireService( StatisticsImplementor.class );
		}
		return statistics;
	}

	public FilterDefinition getFilterDefinition(String filterName) {
		final FilterDefinition filterDefinition = filters.get( filterName );
		if ( filterDefinition == null ) {
			throw new UnknownFilterException( filterName );
		}
		return filterDefinition;
	}

	@Override
	public java.util.Collection<FilterDefinition> getAutoEnabledFilters() {
		return autoEnabledFilters;
	}

	@Override
	public Set<String> getDefinedFilterNames() {
		return unmodifiableSet( filters.keySet() );
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return sqlTranslationEngine.getFetchProfile( name );
	}

	@Override
	public boolean containsFetchProfileDefinition(String name) {
		return sqlTranslationEngine.containsFetchProfileDefinition( name );
	}

	@Override
	public Set<String> getDefinedFetchProfileNames() {
		return sqlTranslationEngine.getDefinedFetchProfileNames();
	}

	@Override @Deprecated
	public Generator getGenerator(String rootEntityName) {
		return null;
	}

	private boolean canAccessTransactionManager() {
		try {
			return serviceRegistry.requireService( JtaPlatform.class ).retrieveTransactionManager() != null;
		}
		catch ( Exception e ) {
			return false;
		}
	}

	private CurrentSessionContext buildCurrentSessionContext() {
		final String sessionContextType = (String) settings.get( CURRENT_SESSION_CONTEXT_CLASS );
		// for backward-compatibility
		if ( sessionContextType == null ) {
			if ( canAccessTransactionManager() ) {
				return createSessionContext( "jta" );
			}
			else {
				return null;
			}
		}
		else {
			return createSessionContext( sessionContextType );
		}
	}

	private CurrentSessionContext createSessionContext(String sessionContextType) {
		switch ( sessionContextType ) {
			case "jta":
//			if ( ! transactionFactory().compatibleWithJtaSynchronization() ) {
//				LOG.autoFlushWillNotWork();
//			}
				return new JTASessionContext(this);
			case "thread":
				return new ThreadLocalSessionContext(this);
			case "managed":
				return new ManagedSessionContext(this);
			default:
				try {
					return (CurrentSessionContext)
							getClassLoaderService()
									.classForName( sessionContextType )
									.getConstructor( new Class[]{ SessionFactoryImplementor.class } )
									.newInstance( this );
				}
				catch ( Throwable t ) {
					LOG.unableToConstructCurrentSessionContext( sessionContextType, t );
					return null;
				}
		}
	}

	@Override
	public RuntimeMetamodelsImplementor getRuntimeMetamodels() {
		return runtimeMetamodels;

	}

	@Override
	public JpaMetamodelImplementor getJpaMetamodel() {
		return runtimeMetamodels.getJpaMetamodel();
	}

	@Override
	public ServiceRegistryImplementor getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return sessionFactoryOptions.getEntityNotFoundDelegate();
	}

	/**
	 * @deprecated use {@link #configuredInterceptor(Interceptor, boolean, SessionFactoryOptions)}
	 */
	@Deprecated
	public static Interceptor configuredInterceptor(Interceptor interceptor, SessionFactoryOptions options) {
		return configuredInterceptor( interceptor, false, options );
	}

	public static Interceptor configuredInterceptor(Interceptor interceptor, boolean explicitNoInterceptor, SessionFactoryOptions options) {
		// NOTE : DO NOT return EmptyInterceptor.INSTANCE from here as a "default for the Session"
		// 		we "filter" that one out here.  The return from here should represent the
		//		explicitly configured Interceptor (if one).  Return null from here instead; Session
		//		will handle it

		if ( interceptor != null && interceptor != EmptyInterceptor.INSTANCE ) {
			return interceptor;
		}

		// prefer the SessionFactory-scoped interceptor, prefer that to any Session-scoped interceptor prototype
		final Interceptor optionsInterceptor = options.getInterceptor();
		if ( optionsInterceptor != null && optionsInterceptor != EmptyInterceptor.INSTANCE ) {
			return optionsInterceptor;
		}

		// If explicitly asking for no interceptor and there is no SessionFactory-scoped interceptors, then
		// no need to inherit from the configured stateless session ones.
		if ( explicitNoInterceptor ) {
			return null;
		}

		// then check the Session-scoped interceptor prototype
		final Supplier<? extends Interceptor> statelessInterceptorImplementorSupplier =
				options.getStatelessInterceptorImplementorSupplier();
		if ( statelessInterceptorImplementorSupplier != null ) {
			return statelessInterceptorImplementorSupplier.get();
		}

		return null;
	}

	public static class SessionBuilderImpl implements SessionBuilderImplementor, SessionCreationOptions {
		private static final Logger log = CoreLogging.logger( SessionBuilderImpl.class );

		private final SessionFactoryImpl sessionFactory;
		private Interceptor interceptor;
		private StatementInspector statementInspector;
		private Connection connection;
		private PhysicalConnectionHandlingMode connectionHandlingMode;
		private boolean autoJoinTransactions = true;
		private FlushMode flushMode;
		private boolean autoClose;
		private boolean autoClear;
		private Object tenantIdentifier;
		private boolean identifierRollback;
		private TimeZone jdbcTimeZone;
		private boolean explicitNoInterceptor;
		private final int defaultBatchFetchSize;
		private final boolean subselectFetchEnabled;

		// Lazy: defaults can be built by invoking the builder in fastSessionServices.defaultSessionEventListeners
		// (Need a fresh build for each Session as the listener instances can't be reused across sessions)
		// Only initialize of the builder is overriding the default.
		private List<SessionEventListener> listeners;

		//todo : expose setting
		private final SessionOwnerBehavior sessionOwnerBehavior = SessionOwnerBehavior.LEGACY_NATIVE;

		public SessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;

			// set up default builder values...
			final SessionFactoryOptions sessionFactoryOptions = sessionFactory.getSessionFactoryOptions();
			statementInspector = sessionFactoryOptions.getStatementInspector();
			connectionHandlingMode = sessionFactoryOptions.getPhysicalConnectionHandlingMode();
			autoClose = sessionFactoryOptions.isAutoCloseSessionEnabled();
			defaultBatchFetchSize = sessionFactoryOptions.getDefaultBatchFetchSize();
			subselectFetchEnabled = sessionFactoryOptions.isSubselectFetchEnabled();
			identifierRollback = sessionFactoryOptions.isIdentifierRollbackEnabled();

			final CurrentTenantIdentifierResolver<Object> currentTenantIdentifierResolver =
					sessionFactory.getCurrentTenantIdentifierResolver();
			if ( currentTenantIdentifierResolver != null ) {
				tenantIdentifier = currentTenantIdentifierResolver.resolveCurrentTenantIdentifier();
			}
			jdbcTimeZone = sessionFactoryOptions.getJdbcTimeZone();
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SessionCreationOptions

		@Override
		public ExceptionMapper getExceptionMapper() {
			return sessionOwnerBehavior == SessionOwnerBehavior.LEGACY_JPA
					? ExceptionMapperLegacyJpaImpl.INSTANCE
					: null;
		}

		@Override
		public boolean shouldAutoJoinTransactions() {
			return autoJoinTransactions;
		}

		@Override
		public FlushMode getInitialSessionFlushMode() {
			return flushMode;
		}

		@Override
		public boolean isSubselectFetchEnabled() {
			return subselectFetchEnabled;
		}

		@Override
		public int getDefaultBatchFetchSize() {
			return defaultBatchFetchSize;
		}

		@Override
		public boolean shouldAutoClose() {
			return autoClose;
		}

		@Override
		public boolean shouldAutoClear() {
			return autoClear;
		}

		@Override
		public Connection getConnection() {
			return connection;
		}

		@Override
		public Interceptor getInterceptor() {
			return configuredInterceptor( interceptor, explicitNoInterceptor, sessionFactory.getSessionFactoryOptions() );
		}

		@Override
		public StatementInspector getStatementInspector() {
			return statementInspector;
		}

		@Override
		public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
			return connectionHandlingMode;
		}

		@Override
		public String getTenantIdentifier() {
			if ( tenantIdentifier == null ) {
				return null;
			}
			return sessionFactory.getTenantIdentifierJavaType().toString( tenantIdentifier );
		}

		@Override
		public Object getTenantIdentifierValue() {
			return tenantIdentifier;
		}

		@Override
		public boolean isIdentifierRollbackEnabled() {
			return identifierRollback;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return jdbcTimeZone;
		}

		@Override
		public List<SessionEventListener> getCustomSessionEventListener() {
			return listeners;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SessionBuilder

		@Override
		public SessionImpl openSession() {
			log.tracef( "Opening Hibernate Session.  tenant=%s", tenantIdentifier );
			return new SessionImpl( sessionFactory, this );
		}

		@Override
		public SessionBuilderImpl interceptor(Interceptor interceptor) {
			this.interceptor = interceptor;
			this.explicitNoInterceptor = false;
			return this;
		}

		@Override
		public SessionBuilderImpl noInterceptor() {
			this.interceptor = EmptyInterceptor.INSTANCE;
			this.explicitNoInterceptor = true;
			return this;
		}

		@Override @Deprecated
		public SessionBuilderImpl statementInspector(StatementInspector statementInspector) {
			this.statementInspector = statementInspector;
			return this;
		}

		@Override
		public SessionBuilder statementInspector(UnaryOperator<String> operator) {
			this.statementInspector = operator::apply;
			return this;
		}

		@Override
		public SessionBuilderImpl connection(Connection connection) {
			this.connection = connection;
			return this;
		}

		@Override @Deprecated
		public SessionBuilderImpl connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
			this.connectionHandlingMode = connectionHandlingMode;
			return this;
		}

		@Override
		public SessionBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
			this.connectionHandlingMode = PhysicalConnectionHandlingMode.interpret( acquisitionMode, releaseMode);
			return this;
		}

		@Override
		public SessionBuilderImpl autoJoinTransactions(boolean autoJoinTransactions) {
			this.autoJoinTransactions = autoJoinTransactions;
			return this;
		}

		@Override
		public SessionBuilderImpl autoClose(boolean autoClose) {
			this.autoClose = autoClose;
			return this;
		}

		@Override
		public SessionBuilderImpl autoClear(boolean autoClear) {
			this.autoClear = autoClear;
			return this;
		}

		@Override
		public SessionBuilderImpl flushMode(FlushMode flushMode) {
			this.flushMode = flushMode;
			return this;
		}

		@Override @Deprecated(forRemoval = true)
		public SessionBuilderImpl tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}

		@Override
		public SessionBuilderImpl tenantIdentifier(Object tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}

		@Override
		public SessionBuilder identifierRollback(boolean identifierRollback) {
			this.identifierRollback = identifierRollback;
			return this;
		}

		@Override
		public SessionBuilderImpl eventListeners(SessionEventListener... listeners) {
			if ( this.listeners == null ) {
				final var baselineListeners =
						sessionFactory.getSessionFactoryOptions().buildSessionEventListeners();
				this.listeners = new ArrayList<>( baselineListeners.length + listeners.length );
				addAll( this.listeners, baselineListeners );
			}
			addAll( this.listeners, listeners );
			return this;
		}

		@Override
		public SessionBuilderImpl clearEventListeners() {
			if ( listeners == null ) {
				//Needs to initialize explicitly to an empty list as otherwise "null" implies the default listeners will be applied
				this.listeners = new ArrayList<>( 3 );
			}
			else {
				listeners.clear();
			}
			return this;
		}

		@Override
		public SessionBuilderImpl jdbcTimeZone(TimeZone timeZone) {
			jdbcTimeZone = timeZone;
			return this;
		}
	}

	public static class StatelessSessionBuilderImpl implements StatelessSessionBuilder, SessionCreationOptions {
		private final SessionFactoryImpl sessionFactory;
		private StatementInspector statementInspector;
		private Connection connection;
		private Object tenantIdentifier;

		public StatelessSessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;
			this.statementInspector = sessionFactory.getSessionFactoryOptions().getStatementInspector();

			final var tenantIdentifierResolver = sessionFactory.getCurrentTenantIdentifierResolver();
			if ( tenantIdentifierResolver != null ) {
				tenantIdentifier = tenantIdentifierResolver.resolveCurrentTenantIdentifier();
			}
		}

		@Override
		public StatelessSession openStatelessSession() {
			return new StatelessSessionImpl( sessionFactory, this );
		}

		@Override
		public StatelessSessionBuilder connection(Connection connection) {
			this.connection = connection;
			return this;
		}

		@Override @Deprecated
		public StatelessSessionBuilder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}

		@Override
		public StatelessSessionBuilder tenantIdentifier(Object tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}

		@Override @Deprecated
		public StatelessSessionBuilder statementInspector(StatementInspector statementInspector) {
			this.statementInspector = statementInspector;
			return this;
		}

		@Override
		public StatelessSessionBuilder statementInspector(UnaryOperator<String> operator) {
			this.statementInspector = operator::apply;
			return this;
		}

		@Override
		public boolean shouldAutoJoinTransactions() {
			return true;
		}

		@Override
		public FlushMode getInitialSessionFlushMode() {
			return FlushMode.ALWAYS;
		}

		@Override
		public boolean isSubselectFetchEnabled() {
			return false;
		}

		@Override
		public int getDefaultBatchFetchSize() {
			return -1;
		}

		@Override
		public boolean shouldAutoClose() {
			return false;
		}

		@Override
		public boolean shouldAutoClear() {
			return false;
		}

		@Override
		public Connection getConnection() {
			return connection;
		}

		@Override
		public Interceptor getInterceptor() {
			return configuredInterceptor( EmptyInterceptor.INSTANCE, false,
					sessionFactory.getSessionFactoryOptions() );
		}

		@Override
		public boolean isIdentifierRollbackEnabled() {
			// identifier rollback not yet implemented for StatelessSessions
			return false;
		}

		@Override
		public StatementInspector getStatementInspector() {
			return statementInspector;
		}

		@Override
		public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
			return sessionFactory.getSessionFactoryOptions().getPhysicalConnectionHandlingMode();
		}

		@Override
		public String getTenantIdentifier() {
			return tenantIdentifier == null ? null
					: sessionFactory.getTenantIdentifierJavaType().toString( tenantIdentifier );
		}

		@Override
		public Object getTenantIdentifierValue() {
			return tenantIdentifier;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return sessionFactory.getSessionFactoryOptions().getJdbcTimeZone();
		}

		@Override
		public List<SessionEventListener> getCustomSessionEventListener() {
			return null;
		}

		@Override
		public ExceptionMapper getExceptionMapper() {
			return null;
		}
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return getSessionFactoryOptions().getCustomEntityDirtinessStrategy();
	}

	@Override
	public CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver() {
		return getSessionFactoryOptions().getCurrentTenantIdentifierResolver();
	}

	@Override
	public JavaType<Object> getTenantIdentifierJavaType() {
		return tenantIdentifierJavaType;
	}


	// Serialization handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Custom serialization hook used when the factory is directly serialized
	 *
	 * @param out The stream into which the object is being serialized.
	 *
	 * @throws IOException Can be thrown by the stream
	 */
	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		LOG.serializingFactory( getUuid() );
		out.defaultWriteObject();
		LOG.trace( "Serialized factory" );
	}

	/**
	 * Custom serialization hook used when the factory is directly deserialized.
	 *
	 * @param in The stream from which the object is being deserialized.
	 *
	 * @throws IOException Can be thrown by the stream
	 * @throws ClassNotFoundException Again, can be thrown by the stream
	 */
	@Serial
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		LOG.trace( "Deserializing factory" );
		in.defaultReadObject();
		LOG.deserializedFactory( getUuid() );
	}

	/**
	 * Custom serialization hook used when the factory is directly deserialized.
	 * <p>
	 * Here we resolve the uuid/name read from the stream previously to resolve
	 * the {@code SessionFactory} instance to use based on the registrations with
	 * the {@link SessionFactoryRegistry}.
	 *
	 * @return The resolved factory to use.
	 *
	 * @throws InvalidObjectException Thrown if we could not resolve the factory by uuid/name.
	 */
	@Serial
	private Object readResolve() throws InvalidObjectException {
		LOG.trace( "Resolving serialized factory" );
		return locateSessionFactoryOnDeserialization( getUuid(), name );
	}

	private static SessionFactory locateSessionFactoryOnDeserialization(String uuid, String name)
			throws InvalidObjectException{
		final SessionFactory uuidResult = SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid );
		if ( uuidResult != null ) {
			LOG.debug( "Resolved factory by UUID: " + uuid );
			return uuidResult;
		}

		// in case we were deserialized in a different JVM, look for an instance with the same name
		// (provided we were given a name)
		if ( name != null ) {
			final SessionFactory namedResult = SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( name );
			if ( namedResult != null ) {
				LOG.debug( "Resolved factory by name: " + name );
				return namedResult;
			}
		}

		throw new InvalidObjectException( "No SessionFactory with uuid [" + uuid + "] and name [" + name + "]" );
	}

	/**
	 * Custom serialization hook used during {@code Session} serialization.
	 *
	 * @param oos The stream to which to write the factory
	 * @throws IOException Indicates problems writing out the serial data stream
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeUTF( getUuid() );
		oos.writeBoolean( name != null );
		if ( name != null ) {
			oos.writeUTF( name );
		}
	}

	/**
	 * Custom deserialization hook used during {@code Session} deserialization.
	 *
	 * @param ois The stream from which to "read" the factory
	 * @return The deserialized factory
	 * @throws IOException indicates problems reading back serial data stream
	 */
	static SessionFactoryImpl deserialize(ObjectInputStream ois) throws IOException {
		LOG.trace( "Resolving factory from deserialized session" );
		final String uuid = ois.readUTF();
		boolean isNamed = ois.readBoolean();
		final String name = isNamed ? ois.readUTF() : null;
		return (SessionFactoryImpl) locateSessionFactoryOnDeserialization( uuid, name );
	}

	@Override
	public WrapperOptions getWrapperOptions() {
		return wrapperOptions;
	}

	@Override
	public SchemaManager getSchemaManager() {
		return schemaManager;
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return getRuntimeMetamodels().getMappingMetamodel();
	}

	private enum Status {
		OPEN,
		CLOSING,
		CLOSED
	}

	private class ModelCreationContext implements RuntimeModelCreationContext, GeneratorSettings {
		final Map<String, Generator> generators;
		private final BootstrapContext bootstrapContext;
		private final MetadataImplementor bootMetamodel;
		private final MappingMetamodelImplementor mappingMetamodel;
		private final TypeConfiguration typeConfiguration;

		private ModelCreationContext(
				BootstrapContext bootstrapContext,
				MetadataImplementor bootMetamodel,
				MappingMetamodelImplementor mappingMetamodel,
				TypeConfiguration typeConfiguration) {
			this.bootstrapContext = bootstrapContext;
			this.bootMetamodel = bootMetamodel;
			this.mappingMetamodel = mappingMetamodel;
			this.typeConfiguration = typeConfiguration;
			generators = new HashMap<>();
		}

		@Override
		public BootstrapContext getBootstrapContext() {
			return bootstrapContext;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			// this is bad, we're not yet fully-initialized
			return SessionFactoryImpl.this;
		}

		@Override
		public MetadataImplementor getBootModel() {
			return bootMetamodel;
		}

		@Override
		public MappingMetamodelImplementor getDomainModel() {
			return mappingMetamodel;
		}

		@Override
		public CacheImplementor getCache() {
			return cacheAccess;
		}

		@Override
		public Map<String, Object> getSettings() {
			return settings;
		}

		@Override
		public Dialect getDialect() {
			return jdbcServices.getDialect();
		}

		@Override
		public SqmFunctionRegistry getFunctionRegistry() {
			return queryEngine.getSqmFunctionRegistry();
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public SessionFactoryOptions getSessionFactoryOptions() {
			return sessionFactoryOptions;
		}

		@Override
		public JdbcServices getJdbcServices() {
			return jdbcServices;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return serviceRegistry;
		}

		@Override
		public Map<String, Generator> getGenerators() {
			return generators;
		}

		@Override
		public GeneratorSettings getGeneratorSettings() {
			return this;
		}

		@Override
		public String getDefaultCatalog() {
			return sessionFactoryOptions.getDefaultCatalog();
		}

		@Override
		public String getDefaultSchema() {
			return sessionFactoryOptions.getDefaultSchema();
		}

		@Override
		public SqlStringGenerationContext getSqlStringGenerationContext() {
			return sqlStringGenerationContext;
		}
	}
}
