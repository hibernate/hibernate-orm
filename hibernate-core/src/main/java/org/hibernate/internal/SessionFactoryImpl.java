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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import jakarta.persistence.TypedQuery;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
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
import org.hibernate.engine.creation.internal.SessionBuilderImpl;
import org.hibernate.engine.creation.internal.StatelessSessionBuilderImpl;
import org.hibernate.engine.creation.spi.SessionBuilderImplementor;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
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
import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.RuntimeMetamodelsImpl;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.proxy.EntityNotFoundDelegate;
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

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQueryReference;

import static jakarta.persistence.SynchronizationType.SYNCHRONIZED;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.cfg.AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS;
import static org.hibernate.internal.FetchProfileHelper.addFetchProfiles;
import static org.hibernate.internal.SessionFactoryLogging.SESSION_FACTORY_LOGGER;
import static org.hibernate.internal.SessionFactorySettings.determineJndiName;
import static org.hibernate.internal.SessionFactorySettings.getMaskedSettings;
import static org.hibernate.internal.SessionFactorySettings.getSessionFactoryName;
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
	private final transient Collection<FilterDefinition> autoEnabledFilters = new ArrayList<>();
	private final transient JavaType<Object> tenantIdentifierJavaType;

	private final transient EventListenerGroups eventListenerGroups;

	private final transient WrapperOptions wrapperOptions;
	private final transient SessionBuilderImplementor defaultSessionOpenOptions;
	private final transient SessionBuilderImplementor temporarySessionOpenOptions;
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
SESSION_FACTORY_LOGGER.buildingSessionFactory();
		typeConfiguration = bootstrapContext.getTypeConfiguration();

		sessionFactoryOptions = options;

		serviceRegistry = getServiceRegistry( options, this );
		eventEngine = new EventEngine( bootMetamodel, this );

		bootMetamodel.initSessionFactory( this );

		name = getSessionFactoryName( options, serviceRegistry );
		jndiName = determineJndiName( name, options, serviceRegistry );
		uuid = options.getUuid();

		jdbcServices = serviceRegistry.requireService( JdbcServices.class );

		settings = getMaskedSettings( options, serviceRegistry );
		SESSION_FACTORY_LOGGER.instantiatingFactory( uuid, settings );

		sqlStringGenerationContext = createSqlStringGenerationContext( bootMetamodel, options, jdbcServices );

		cacheAccess = serviceRegistry.getService( CacheImplementor.class );

		jpaPersistenceUnitUtil = new PersistenceUnitUtilImpl( this );

		for ( var sessionFactoryObserver : options.getSessionFactoryObservers() ) {
			observer.addObserver( sessionFactoryObserver );
		}

		filters = new HashMap<>( bootMetamodel.getFilterDefinitions() );

		tenantIdentifierJavaType = tenantIdentifierType( options );

		for ( var filter : filters.values() ) {
			if ( filter.isAutoEnabled() ) {
				autoEnabledFilters.add( filter );
			}
		}

		entityNameResolver = new CoordinatingEntityNameResolver( this, getInterceptor() );
		schemaManager = new SchemaManagerImpl( this, bootMetamodel );

		// used for initializing the MappingMetamodelImpl
		classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		jdbcValuesMappingProducerProvider = serviceRegistry.requireService( JdbcValuesMappingProducerProvider.class );

		final var integratorObserver = new IntegratorObserver();
		observer.addObserver( integratorObserver );
		try {
			integrate( bootMetamodel, bootstrapContext, integratorObserver );

			bootMetamodel.orderColumns( false );
			bootMetamodel.validate();

			primeSecondLevelCacheRegions( bootMetamodel );

			// create the empty runtime metamodels object
			final var runtimeMetamodelsImpl = new RuntimeMetamodelsImpl( typeConfiguration );
			runtimeMetamodels = runtimeMetamodelsImpl;

			// we build this before creating the runtime metamodels
			// because the SqlAstTranslators (unnecessarily, perhaps)
			// use the SqmFunctionRegistry when rendering SQL for Loaders
			queryEngine = new QueryEngineImpl( bootMetamodel, options, runtimeMetamodels, serviceRegistry, settings, name );
			final Map<String, FetchProfile> fetchProfiles = new HashMap<>();
			sqlTranslationEngine = new SqlTranslationEngineImpl( this, typeConfiguration, fetchProfiles );

			// now actually create the mapping and JPA metamodels
			final var mappingMetamodelImpl = new MappingMetamodelImpl( typeConfiguration, serviceRegistry );
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
				SESSION_FACTORY_LOGGER.eatingErrorClosingFactoryAfterFailedInstantiation();
			}
			throw e;
		}

		SESSION_FACTORY_LOGGER.instantiatedFactory( uuid );
	}

	private JavaType<Object> tenantIdentifierType(SessionFactoryOptions options) {
		final var tenantFilter = filters.get( TenantIdBinder.FILTER_NAME );
		if ( tenantFilter == null ) {
			return options.getDefaultTenantIdentifierJavaType();
		}
		else {
			final var jdbcMapping = tenantFilter.getParameterJdbcMapping( TenantIdBinder.PARAMETER_NAME );
			assert jdbcMapping != null;
			//noinspection unchecked
			return jdbcMapping.getJavaTypeDescriptor();
		}
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
			for ( var integrator : integrators ) {
				integrator.disintegrate( SessionFactoryImpl.this, SessionFactoryImpl.this.serviceRegistry );
			}
			integrators.clear();
		}
	}

	private void integrate(MetadataImplementor bootMetamodel, BootstrapContext bootstrapContext, IntegratorObserver integratorObserver) {
		for ( var integrator : serviceRegistry.requireService( IntegratorService.class ).getIntegrators() ) {
			integrator.integrate( bootMetamodel, bootstrapContext, this );
			integratorObserver.integrators.add( integrator );
		}
	}

	private void disintegrate(Exception startupException, IntegratorObserver integratorObserver) {
		for ( var integrator : integratorObserver.integrators ) {
			try {
				integrator.disintegrate( this, serviceRegistry );
			}
			catch (Throwable ex) {
				startupException.addSuppressed( ex );
			}
		}
		integratorObserver.integrators.clear();
	}


	private SessionBuilderImplementor createDefaultSessionOpenOptionsIfPossible() {
		final var tenantIdResolver = getCurrentTenantIdentifierResolver();
		// Don't store a default SessionBuilder when a CurrentTenantIdentifierResolver is provided
		return tenantIdResolver == null ? withOptions() : null;
	}

	private SessionBuilderImplementor buildTemporarySessionOpenOptions() {
		return withOptions()
				.autoClose( false )
				.flushMode( FlushMode.MANUAL )
				.connectionHandlingMode( DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT );
	}

	private void primeSecondLevelCacheRegions(MetadataImplementor mappingMetadata) {
		final Map<String, DomainDataRegionConfigImpl.Builder> regionConfigBuilders = new ConcurrentHashMap<>();

		// TODO: ultimately this code can be made more efficient when we have
		//       a better intrinsic understanding of the hierarchy as a whole

		for ( var bootEntityDescriptor : mappingMetadata.getEntityBindings() ) {
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

		for ( var collection : mappingMetadata.getCollectionBindings() ) {
			final AccessType accessType =
					AccessType.fromExternalName( collection.getCacheConcurrencyStrategy() );
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
			for ( var builder : regionConfigBuilders.values() ) {
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
	public SessionImplementor openTemporarySession() {
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
	public SessionBuilderImplementor withOptions() {
		return new SessionBuilderImpl( this ) {
			@Override
			protected SessionImplementor createSession() {
				return new SessionImpl( SessionFactoryImpl.this, this );
			}
		};
	}

	@Override
	public StatelessSessionBuilder withStatelessOptions() {
		return new StatelessSessionBuilderImpl( this ) {
			@Override
			protected StatelessSessionImplementor createStatelessSession() {
				return new StatelessSessionImpl( SessionFactoryImpl.this, this );
			}
		};
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

	@Override
	public Session createEntityManager() {
		validateNotClosed();
		return buildEntityManager( SYNCHRONIZED, null );
	}

	private Session buildEntityManager(SynchronizationType synchronizationType, Map<?,?> map) {
		assert status != Status.CLOSED;

		var builder = withOptions();
		builder.autoJoinTransactions( synchronizationType == SYNCHRONIZED );

		if ( map != null ) {
			final Object tenantIdHint = map.get( HINT_TENANT_ID );
			if ( tenantIdHint != null ) {
				builder = builder.tenantIdentifier( tenantIdHint );
			}
		}

		final var session = builder.openSession();
		if ( map != null ) {
			for ( var entry : map.entrySet() ) {
				if ( entry.getKey() instanceof String string ) {
					if ( !HINT_TENANT_ID.equals( string ) ) {
						session.setProperty( string, entry.getValue() );
					}
				}
			}
		}
		return session;
	}

	public Session createEntityManager(Map<?,?> map) {
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

	@Override
	public Session createEntityManager(SynchronizationType synchronizationType, Map<?,?> map) {
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
		final var entity = getJpaMetamodel().entity( entityName );
		if ( entity.getRepresentationMode() != RepresentationMode.MAP ) {
			throw new IllegalArgumentException( "Entity '" + entityName + "' is not a dynamic entity" );
		}
		@SuppressWarnings("unchecked") //Safe, because we just checked
		final var dynamicEntity = (EntityDomainType<Map<String, ?>>) entity;
		return new RootGraphImpl<>( null, dynamicEntity );
	}

	@Override
	public RootGraphImplementor<?> findEntityGraphByName(String name) {
		return getJpaMetamodel().findEntityGraphByName( name );
	}

	@Override
	public String bestGuessEntityName(Object object) {
		final var initializer = extractLazyInitializer( object );
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
		SESSION_FACTORY_LOGGER.returningReferenceToFactory();
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
				SESSION_FACTORY_LOGGER.alreadyClosed();
				return;
			}
			status = Status.CLOSING;
		}

		try {
			SESSION_FACTORY_LOGGER.closingFactory( uuid );
			observer.sessionFactoryClosing( this );

			// NOTE: the null checks below handle cases where close is called
			//		 from a failed attempt to create the SessionFactory

			if ( cacheAccess != null ) {
				cacheAccess.close();
			}

			if ( runtimeMetamodels != null && runtimeMetamodels.getMappingMetamodel() != null ) {
				final var jdbcConnectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
				runtimeMetamodels.getMappingMetamodel().forEachEntityDescriptor(
						entityPersister -> {
							final var mutationStrategy = entityPersister.getSqmMultiTableMutationStrategy();
							final var insertStrategy = entityPersister.getSqmMultiTableInsertStrategy();
							if ( mutationStrategy != null ) {
								mutationStrategy.release( this, jdbcConnectionAccess );
							}
							if ( insertStrategy != null ) {
								insertStrategy.release( this, jdbcConnectionAccess );
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
		getJpaMetamodel().addNamedEntityGraph( graphName, (RootGraphImplementor<T>) entityGraph );
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
		final var filterDefinition = filters.get( filterName );
		if ( filterDefinition == null ) {
			throw new UnknownFilterException( filterName );
		}
		return filterDefinition;
	}

	@Override
	public Collection<FilterDefinition> getAutoEnabledFilters() {
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
				catch ( Throwable throwable ) {
					SESSION_FACTORY_LOGGER.unableToConstructCurrentSessionContext( sessionContextType, throwable );
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

	public Object resolveTenantIdentifier() {
		final var resolver = getCurrentTenantIdentifierResolver();
		return resolver != null
				? resolver.resolveCurrentTenantIdentifier()
				: null;
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

	boolean connectionProviderHandlesConnectionReadOnly() {
		return multiTenantConnectionProvider != null
				? multiTenantConnectionProvider.handlesConnectionReadOnly()
				: connectionProvider.handlesConnectionReadOnly();
	}

	boolean connectionProviderHandlesConnectionSchema() {
		return multiTenantConnectionProvider != null
				? multiTenantConnectionProvider.handlesConnectionSchema()
				: connectionProvider.handlesConnectionSchema();
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
		SESSION_FACTORY_LOGGER.serializingFactory( uuid );
		out.defaultWriteObject();
		SESSION_FACTORY_LOGGER.serializedFactory();
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
		SESSION_FACTORY_LOGGER.deserializingFactory();
		in.defaultReadObject();
		SESSION_FACTORY_LOGGER.deserializedFactory( uuid );
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
		SESSION_FACTORY_LOGGER.trace( "Resolving serialized factory" );
		return locateSessionFactoryOnDeserialization( uuid, name );
	}

	private static SessionFactory locateSessionFactoryOnDeserialization(String uuid, String name)
			throws InvalidObjectException{
		final var uuidResult = SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid );
		if ( uuidResult != null ) {
			if ( SESSION_FACTORY_LOGGER.isTraceEnabled() ) {
				SESSION_FACTORY_LOGGER.resolvedFactoryByUuid( uuid );
			}
			return uuidResult;
		}

		// in case we were deserialized in a different JVM, look for an instance with the same name
		// (provided we were given a name)
		if ( name != null ) {
			final var namedResult = SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( name );
			if ( namedResult != null ) {
				if ( SESSION_FACTORY_LOGGER.isTraceEnabled() ) {
					SESSION_FACTORY_LOGGER.resolvedFactoryByName( name );
				}
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
		SESSION_FACTORY_LOGGER.resolvingFactoryFromDeserializedSession();
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

		@Override
		public Generator getOrCreateIdGenerator(String rootName, PersistentClass persistentClass) {
			final var existing = getGenerators().get( rootName );
			if ( existing != null ) {
				return existing;
			}
			else {
				final var idGenerator =
						persistentClass.getIdentifier()
								// returns the cached Generator if it was already created
								.createGenerator(
										getDialect(),
										persistentClass.getRootClass(),
										persistentClass.getIdentifierProperty(),
										getGeneratorSettings()
								);
				getGenerators().put( rootName, idGenerator );
				return idGenerator;
			}
		}
	}
}
