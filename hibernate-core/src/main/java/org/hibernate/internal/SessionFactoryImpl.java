/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityHandler;
import jakarta.persistence.EntityListenerRegistration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.Statement;
import jakarta.persistence.StatementReference;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.sql.ResultSetMapping;
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
import org.hibernate.StatementObserver;
import org.hibernate.UnknownFilterException;
import org.hibernate.action.queue.spi.ActionQueueFactory;
import org.hibernate.action.queue.spi.PlanningOptions;
import org.hibernate.boot.pipeline.internal.SessionFactoryConstructionPlan;
import org.hibernate.boot.pipeline.internal.SessionFactoryConstructionPlanBuilder;
import org.hibernate.boot.pipeline.internal.ResolvedMappingImplementor;
import org.hibernate.boot.pipeline.internal.SessionFactoryIntegratorLifecycle;
import org.hibernate.boot.pipeline.internal.SessionFactoryRuntimeComponents;
import org.hibernate.boot.pipeline.internal.StandardServiceComponents;
import org.hibernate.boot.pipeline.spi.SessionFactoryConstructionIdentity;
import org.hibernate.boot.pipeline.spi.ResolvedSessionFactorySettings;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.model.internal.GeneratorBinder;
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
import org.hibernate.engine.creation.internal.options.StatefulOptions;
import org.hibernate.engine.creation.internal.options.StatelessOptions;
import org.hibernate.engine.creation.spi.SessionBuilderImplementor;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.generator.Generator;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;
import org.hibernate.mapping.GeneratorSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.internal.RuntimeModelHandoffResolvers;
import org.hibernate.metamodel.internal.RuntimeMappingHandoff;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.SessionFactoryAccess;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.named.spi.NamedObjectRepository;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.spi.SqlTranslationEngine;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.relational.SchemaManager;
import org.hibernate.relational.internal.SchemaManagerImpl;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.temporal.spi.ChangesetCoordinator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducerProvider;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.annotation.Annotation;
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

import static jakarta.persistence.SynchronizationType.SYNCHRONIZED;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Locale.ROOT;
import static org.hibernate.cfg.AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS;
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
 * @see org.hibernate.boot.pipeline.internal.BootstrapPipeline
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Chris Cranford
 */
// Extended by Hibernate Reactive
public class SessionFactoryImpl implements SessionFactoryImplementor {

	private final String name;
	private final String jndiName;
	private final String uuid;

	private transient volatile Status status = Status.OPEN;

	private final transient SessionFactoryObserverChain observerChain = new SessionFactoryObserverChain();
	private final transient StatementObserver statementObserver;

	private final transient SessionFactoryOptions sessionFactoryOptions;
	private final transient Map<String,Object> settings;

	private final transient SessionFactoryServiceRegistry serviceRegistry;
	private final transient StandardServiceComponents standardServiceComponents;
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
	private final transient Collection<FilterDefinition> autoEnabledFilters;
	private final transient JavaType<Object> tenantIdentifierJavaType;

	private final transient EventListenerGroups eventListenerGroups;

	private final transient WrapperOptions wrapperOptions;
	private final transient SessionBuilderImplementor defaultSessionOpenOptions;
	private final transient SessionBuilderImplementor temporarySessionOpenOptions;
	private final transient StatelessSessionBuilder defaultStatelessOptions;
	private final transient EntityNameResolver entityNameResolver;

	private final transient SchemaManager schemaManager;

	final transient ChangesetCoordinator changesetCoordinator;

	private final PlanningOptions graphPlanningOptions;
	private final transient ActionQueueFactory actionQueueFactory;

	public SessionFactoryImpl(
			final MetadataImplementor bootMetamodel,
			final SessionFactoryOptions options,
			final BootstrapContext bootstrapContext) {
		this( SessionFactoryConstructionPlanBuilder.build(
				bootMetamodel,
				options,
				bootstrapContext,
				extractRuntimeMappingHandoff( bootMetamodel )
		) );
	}

	public SessionFactoryImpl(
			final MetadataImplementor bootMetamodel,
			final SessionFactoryOptions options,
			final BootstrapContext bootstrapContext,
			final SessionFactoryRuntimeComponents runtimeComponents) {
		this( new SessionFactoryConstructionPlan(
				bootMetamodel,
				null,
				null,
				options,
				bootstrapContext,
				extractRuntimeMappingHandoff( bootMetamodel ),
				runtimeComponents
		) );
	}

	public SessionFactoryImpl(
			final MetadataImplementor bootMetamodel,
			final SessionFactoryOptions options,
			final BootstrapContext bootstrapContext,
			final ResolvedSessionFactorySettings resolvedSettings,
			final SessionFactoryConstructionIdentity identity,
			final SessionFactoryRuntimeComponents runtimeComponents) {
		this( new SessionFactoryConstructionPlan(
				bootMetamodel,
				resolvedSettings,
				identity,
				options,
				bootstrapContext,
				extractRuntimeMappingHandoff( bootMetamodel ),
				runtimeComponents
		) );
	}

	private static RuntimeMappingHandoff extractRuntimeMappingHandoff(MetadataImplementor bootMetamodel) {
		if ( bootMetamodel instanceof ResolvedMappingImplementor resolvedMappingImplementor ) {
			return resolvedMappingImplementor.getResolvedMapping().runtimeMappingHandoff();
		}
		throw new IllegalArgumentException(
				"SessionFactory construction requires resolved mapping exposing a runtime mapping handoff"
		);
	}

	public SessionFactoryImpl(final SessionFactoryConstructionPlan constructionPlan) {
		final var bootMetamodel = constructionPlan.metadata();
		final var options = constructionPlan.options();
		final var bootstrapContext = constructionPlan.bootstrapContext();
		final var identity = constructionPlan.identity();
		final var runtimeComponents = constructionPlan.runtimeComponents();
		final var plannedStandardServiceComponents = constructionPlan.standardServiceComponents();
		final var sessionFactoryReference = constructionPlan.sessionFactoryReference();
		sessionFactoryReference.injectSessionFactory( this );

		SESSION_FACTORY_LOGGER.buildingSessionFactory();
		typeConfiguration = runtimeComponents.typeConfiguration();

		sessionFactoryOptions = options;

		statementObserver = runtimeComponents.statementObserver();

		standardServiceComponents = plannedStandardServiceComponents;
		serviceRegistry = standardServiceComponents.serviceRegistry()
				.requireService( SessionFactoryServiceRegistryFactory.class )
				.buildServiceRegistry( sessionFactoryReference, options );
		eventEngine = standardServiceComponents.eventEngine();
		graphPlanningOptions = standardServiceComponents.graphPlanningOptions();

		bootMetamodel.initSessionFactory( this );

		name = identity == null ? getSessionFactoryName( options, serviceRegistry ) : identity.name();
		jndiName = identity == null ? determineJndiName( name, options, serviceRegistry ) : identity.jndiName();
		uuid = identity == null ? options.getUuid() : identity.uuid();

		jdbcServices = standardServiceComponents.jdbcServices();

		settings = getMaskedSettings( options, serviceRegistry );
		SESSION_FACTORY_LOGGER.instantiatingFactory( uuid, settings );

		sqlStringGenerationContext = createSqlStringGenerationContext( bootMetamodel, options, jdbcServices );

		cacheAccess = serviceRegistry.getService( CacheImplementor.class );

		jpaPersistenceUnitUtil = new PersistenceUnitUtilImpl( this );

		for ( var sessionFactoryObserver : runtimeComponents.sessionFactoryObservers() ) {
			observerChain.addObserver( sessionFactoryObserver );
		}

		filters = runtimeComponents.filterDefinitions();
		autoEnabledFilters = runtimeComponents.autoEnabledFilters();
		tenantIdentifierJavaType = runtimeComponents.tenantIdentifierJavaType();

		entityNameResolver = new CoordinatingEntityNameResolver( this, getInterceptor() );
		schemaManager = new SchemaManagerImpl( this, bootMetamodel );

		changesetCoordinator = standardServiceComponents.changesetCoordinator();

		final var integratorLifecycle = new SessionFactoryIntegratorLifecycle( sessionFactoryReference, serviceRegistry );
		observerChain.addObserver( integratorLifecycle );
		try {
			integratorLifecycle.integrate( bootMetamodel, bootstrapContext );

			bootMetamodel.orderColumns( false );
			bootMetamodel.validate();

			primeSecondLevelCacheRegions( bootMetamodel );

			wrapperOptions = new SessionFactoryBasedWrapperOptions( this );
			final var inFlightModel = SessionFactoryModelBuilder.buildInFlight(
					bootMetamodel,
					options,
					typeConfiguration,
					wrapperOptions,
					serviceRegistry,
					settings,
					name
			);
			runtimeMetamodels = inFlightModel.runtimeMetamodels();
			queryEngine = inFlightModel.queryEngine();
			sqlTranslationEngine = inFlightModel.sqlTranslationEngine();

			// Mapping model initialization calls back through this incomplete
			// factory for query and SQL infrastructure, so these fields must be
			// assigned before the model is finished.
			inFlightModel.finishModelInitialization(
					bootMetamodel,
					new ModelCreationContext(
							bootstrapContext,
							bootMetamodel,
							inFlightModel.mappingMetamodel(),
							typeConfiguration,
							graphPlanningOptions,
							constructionPlan.runtimeMappingHandoff(),
							queryEngine,
							wrapperOptions,
							changesetCoordinator,
							sessionFactoryReference
					)
			);

			defaultSessionOpenOptions = createDefaultSessionOpenOptionsIfPossible();
			temporarySessionOpenOptions = defaultSessionOpenOptions == null ? null : buildTemporarySessionOpenOptions();
			defaultStatelessOptions = defaultSessionOpenOptions == null ? null : withStatelessOptions();

			currentSessionContext = buildCurrentSessionContext();

			eventListenerGroups = new EventListenerGroups( eventEngine.getListenerRegistry() );

			// re-scope the TypeConfiguration to this SessionFactory,
			// now that we are (almost) fully-initialized ... note,
			// we could have done this earlier, but then it's hard to
			// really know or control who's calling back to us while
			// we're in an incompletely-initialized state
			typeConfiguration.scope( this );

			if ( inFlightModel.jpaMetamodel() instanceof JpaMetamodelImpl jpaMetamodel ) {
				jpaMetamodel.populateStaticMetamodelResultSetMappings( bootMetamodel, this );
			}

			actionQueueFactory = standardServiceComponents.actionQueueFactoryService().buildActionQueueFactory( this );

			observerChain.sessionFactoryCreated( this );
		}
		catch ( Exception e ) {
			integratorLifecycle.disintegrate( e );

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

	@Override
	@Nonnull
	public EventListenerGroups getEventListenerGroups() {
		return eventListenerGroups;
	}

	@Override
	@Nonnull
	public <E> EntityListenerRegistration addListener(
			@Nonnull Class<E> entityClass,
			@Nonnull Class<? extends Annotation> callbackType,
			@Nonnull Consumer<? super E> callback) {
		final var type = CallbackType.fromCallbackAnnotation( callbackType );
		final List<EntityListenerRegistration> registrations = new ArrayList<>();
		getMappingMetamodel().forEachEntityDescriptor( persister -> {
			final var mappedClass = persister.getMappedClass();
			if ( mappedClass != null && entityClass.isAssignableFrom( mappedClass ) ) {
				registrations.add( addListener( entityClass, persister, type, callback ) );
			}
		} );
		if ( registrations.isEmpty() ) {
			throw new IllegalArgumentException( "Not an entity type or supertype of an entity type: " + entityClass.getName() );
		}
		return registrations.size() == 1
				? registrations.get( 0 )
				: () -> registrations.forEach( EntityListenerRegistration::cancel );
	}

	private static <E> EntityListenerRegistration addListener(
			Class<E> entityClass,
			EntityPersister persister,
			CallbackType callbackType,
			Consumer<? super E> callback) {
		return persister.getEntityCallbacks().addListener(
				callbackType,
				entity -> callback.accept( entityClass.cast( entity ) )
		);
	}

	@Override
	@Nonnull
	public ActionQueueFactory getActionQueueFactory() {
		return actionQueueFactory;
	}

	@Override
	public ParameterMarkerStrategy getParameterMarkerStrategy() {
		return standardServiceComponents.parameterMarkerStrategy();
	}

	@Nonnull
	@Override
	public JdbcValuesMappingProducerProvider getJdbcValuesMappingProducerProvider() {
		return standardServiceComponents.jdbcValuesMappingProducerProvider();
	}

	@Override
	@Nonnull
	public EntityCopyObserverFactory getEntityCopyObserver() {
		return standardServiceComponents.entityCopyObserverFactory();
	}

	@Override
	@Nonnull
	public ClassLoaderService getClassLoaderService() {
		return standardServiceComponents.classLoaderService();
	}

	@Override
	@Nullable
	public ManagedBeanRegistry getManagedBeanRegistry() {
		return standardServiceComponents.managedBeanRegistry();
	}

	@Override
	@Nonnull
	public EventListenerRegistry getEventListenerRegistry() {
		return eventEngine.getListenerRegistry();
	}

	@Override
	@Nonnull
	public PlanningOptions getGraphPlanningOptions() {
		return graphPlanningOptions;
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
	@Nonnull
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
	@Nonnull
	public Session getCurrentSession() {
		if ( currentSessionContext == null ) {
			throw new HibernateException( "No CurrentSessionContext configured" );
		}
		return currentSessionContext.currentSession();
	}

	@Override
	@Nonnull
	public SessionBuilderImplementor withOptions() {
		return new SessionBuilderImpl( this ) {
			@Override
			protected SessionImplementor createSession(StatefulOptions options) {
				return new SessionImpl( SessionFactoryImpl.this, options );
			}
		};
	}

	@Override
	@Nonnull
	public StatelessSessionBuilder withStatelessOptions() {
		return new StatelessSessionBuilderImpl( this ) {
			@Override
			protected StatelessSessionImplementor createStatelessSession(StatelessOptions options) {
				return new StatelessSessionImpl( SessionFactoryImpl.this, options );
			}
		};
	}

	@Override
	@Nonnull
	public StatelessSession openStatelessSession() {
		return defaultStatelessOptions != null
				? defaultStatelessOptions.openStatelessSession()
				: withStatelessOptions().openStatelessSession();
	}

	@Override
	@Nonnull
	public StatelessSession openStatelessSession(@Nonnull Connection connection) {
		return withStatelessOptions().connection( connection ).openStatelessSession();
	}

	@Override
	public void addObserver(@Nonnull SessionFactoryObserver observer) {
		observerChain.addObserver( observer );
	}

	@Override
	@Nonnull
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
	@Nonnull
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	@Nonnull
	public QueryEngine getQueryEngine() {
		return queryEngine;
	}

	@Override
	@Nonnull
	public SqlTranslationEngine getSqlTranslationEngine() {
		return sqlTranslationEngine;
	}

	@Override
	@Nonnull
	public EventEngine getEventEngine() {
		return eventEngine;
	}

	@Override
	@Nonnull
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	@Nonnull
	public SqlStringGenerationContext getSqlStringGenerationContext() {
		return sqlStringGenerationContext;
	}

	@Override
	@Nonnull
	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(@Nonnull Class<T> entityClass) {
		return getJpaMetamodel().findEntityGraphsByJavaType( entityClass );
	}

	@Override
	@Nonnull
	public Session createEntityManager(@Nullable EntityManager.CreationOption... options) {
		validateNotClosed();

		final var optionsCollector = new StatefulOptions( this );
		optionsCollector.autoJoinTransactions( true );

		if ( options != null ) {
			for ( var option : options ) {
				if ( option instanceof SynchronizationType synchronizationType ) {
					errorIfResourceLocalDueToExplicitSynchronizationType();
					optionsCollector.autoJoinTransactions( synchronizationType == SYNCHRONIZED );
				}
				else {
					optionsCollector.apply( option );
				}
			}
		}

		return new SessionImpl( this, optionsCollector );
	}

	private Session buildEntityManager(SynchronizationType synchronizationType, Map<?,?> map) {
		assert status != Status.CLOSED;

		final var optionsCollector = new StatefulOptions( this );
		optionsCollector.autoJoinTransactions( synchronizationType == SYNCHRONIZED );

		if ( map != null ) {
			final Object tenantIdHint = map.get( HINT_TENANT_ID );
			if ( tenantIdHint != null ) {
				optionsCollector.tenantIdentifier( tenantIdHint );
			}
		}

		final var session = new SessionImpl( this, optionsCollector );
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

	@Nonnull
	public Session createEntityManager(@Nullable Map<?,?> map) {
		validateNotClosed();
		return buildEntityManager( SYNCHRONIZED, map );
	}

	@Override
	@Nonnull
	public Session createEntityManager(@Nonnull SynchronizationType synchronizationType) {
		validateNotClosed();
		errorIfResourceLocalDueToExplicitSynchronizationType();
		return buildEntityManager( synchronizationType, null );
	}

	private void errorIfResourceLocalDueToExplicitSynchronizationType() {
		// JPA requires that we throw IllegalStateException in cases where:
		//		1) the PersistenceUnitTransactionType (TransactionCoordinator) is non-JTA
		//		2) an explicit SynchronizationType is specified
		if ( !standardServiceComponents.transactionCoordinatorBuilder().isJta() ) {
			throw new IllegalStateException(
					"Illegal attempt to specify a SynchronizationType when building an EntityManager from an " +
							"EntityManagerFactory defined as RESOURCE_LOCAL (as opposed to JTA)"
			);
		}
	}

	@Override
	@Nonnull
	public Session createEntityManager(@Nonnull SynchronizationType synchronizationType, @Nullable Map<?,?> map) {
		validateNotClosed();
		errorIfResourceLocalDueToExplicitSynchronizationType();
		return buildEntityManager( synchronizationType, map );
	}

	private StatelessSession createEntityAgent() {
		validateNotClosed();
		return openStatelessSession();
	}

	@Override
	@Nonnull
	public StatelessSession createEntityAgent(@Nullable EntityAgent.CreationOption... options) {
		validateNotClosed();
		if ( CollectionHelper.isEmpty( options ) ) {
			return openStatelessSession();
		}

		final var optionsCollector = new StatelessOptions( this );
		optionsCollector.apply( options );

		return new StatelessSessionImpl( this, optionsCollector );
	}

	@Override
	@Nonnull
	public EntityAgent createEntityAgent(@Nullable Map<?, ?> map) {
		var agent = createEntityAgent();
		if ( map != null ) {
			map.forEach( (key,val) -> {
				if ( key instanceof String prop ) {
					agent.setProperty( prop, val );
				}
			} );
		}
		return agent;
	}

	@Override
	@Nonnull
	public NodeBuilder getCriteriaBuilder() {
		validateNotClosed();
		return queryEngine.getCriteriaBuilder();
	}

	@Override
	@Nonnull
	public MappingMetamodel getMetamodel() {
		validateNotClosed();
		return runtimeMetamodels.getMappingMetamodel();
	}

	@Override
	public boolean isOpen() {
		return status != Status.CLOSED;
	}

	@Override
	@Nonnull
	public RootGraphImplementor<Map<String, ?>> createGraphForDynamicEntity(@Nonnull String entityName) {
		final var entity = getJpaMetamodel().entity( entityName );
		if ( entity.getRepresentationMode() != RepresentationMode.MAP ) {
			throw new IllegalArgumentException( "Entity '" + entityName + "' is not a dynamic entity" );
		}
		@SuppressWarnings("unchecked") //Safe, because we just checked
		final var dynamicEntity = (EntityDomainType<Map<String, ?>>) entity;
		return new RootGraphImpl<>( null, dynamicEntity );
	}

	@Override
	@Nullable
	public RootGraphImplementor<?> findEntityGraphByName(@Nonnull String name) {
		return getJpaMetamodel().findEntityGraphByName( name );
	}

	@Override
	@Nullable
	public String bestGuessEntityName(@Nonnull Object object) {
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
	@Nonnull
	public SessionFactoryOptions getSessionFactoryOptions() {
		return sessionFactoryOptions;
	}

	@Override
	public @Nonnull StatementObserver getStatementObserver() {
		return statementObserver;
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
			observerChain.sessionFactoryClosing( this );

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

		observerChain.sessionFactoryClosed( this );
		serviceRegistry.destroy();
	}

	@Override
	@Nonnull
	public CacheImplementor getCache() {
		validateNotClosed();
		return cacheAccess;
	}

	@Override
	@Nonnull
	public PersistenceUnitUtil getPersistenceUnitUtil() {
		validateNotClosed();
		return jpaPersistenceUnitUtil;
	}

	@Override
	@Nonnull
	public PersistenceUnitTransactionType getTransactionType() {
		return standardServiceComponents.transactionCoordinatorBuilder().isJta()
				? PersistenceUnitTransactionType.JTA
				: PersistenceUnitTransactionType.RESOURCE_LOCAL;

	}

	private NamedObjectRepository getNamedObjectRepository() {
		validateNotClosed();
		return getQueryEngine().getNamedObjectRepository();
	}

	@Override
	@Nonnull
	public Map<String, StatementReference> getNamedStatements() {
		return getNamedObjectRepository().getNamedMutations();
	}

	@Override
	public void addNamedQuery(@Nonnull String name, @Nonnull Query query) {
		if ( query instanceof TypedQuery<?> typedQuery ) {
			addNamedQuery( name, typedQuery );
		}
		else if ( query instanceof Statement statement ) {
			addNamedStatement( name, statement );
		}
		else if ( query instanceof MutationOrSelectionQuery statementOrTypedQuery ) {
			if ( statementOrTypedQuery.isMutationQuery() ) {
				addNamedStatement( name, statementOrTypedQuery.asMutationQuery() );
			}
			else {
				addNamedQuery( name, statementOrTypedQuery.asSelectionQuery() );
			}
		}
		else {
			throw new HibernateException( String.format( ROOT,
					"Unknown query type for registering as named query (%s) : %s",
					name,
					query
			) );
		}
	}

	@Override
	@Nonnull
	public <R> TypedQueryReference<R> addNamedQuery(@Nonnull String name, @Nonnull TypedQuery<R> query) {
		return getNamedObjectRepository().registerNamedQuery( name, query );
	}

	@Override
	@Nonnull
	public StatementReference addNamedStatement(@Nonnull String name, @Nonnull Statement statement) {
		return getNamedObjectRepository().registerNamedMutation( name, statement );
	}

	@Override
	public <T> void addNamedEntityGraph(@Nonnull String graphName, @Nonnull EntityGraph<T> entityGraph) {
		getJpaMetamodel().addNamedEntityGraph( graphName, (RootGraphImplementor<T>) entityGraph );
	}

	@Override
	@Nonnull
	public <R> Map<String, TypedQueryReference<R>> getNamedQueries(@Nonnull Class<R> resultType) {
		return queryEngine.getNamedObjectRepository().getNamedQueries( resultType );
	}
	@Override
	@Nonnull
	public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(@Nonnull Class<E> entityType) {
		return getJpaMetamodel().getNamedEntityGraphs( entityType );
	}

	@Override
	@Nonnull
	public <R> Map<String, ResultSetMapping<R>> getResultSetMappings(@Nonnull Class<R> resultType) {
		final var result = new HashMap<String, ResultSetMapping<R>>();
		getNamedObjectRepository().visitResultSetMappingMementos( (memento) -> {
			if ( memento.canBeTreatedAsResultSetMapping( resultType, this ) ) {
				result.put( memento.getName(), memento.toJpaMapping( this ) );
			}
		} );
		return result;
	}


	@Override
	@Nonnull
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
	public void runInTransaction(@Nonnull Consumer<EntityManager> work) {
		inTransaction( work );
	}

	@Override
	public <H extends EntityHandler> void runInTransaction(
			@Nonnull Class<H> handlerType, @Nonnull Consumer<H> consumer) {
		if ( EntityManager.class.isAssignableFrom( handlerType ) ) {
			//noinspection unchecked
			inTransaction( (Consumer<EntityManager>) consumer );
		}
		else if ( EntityAgent.class.isAssignableFrom( handlerType ) ) {
			//noinspection unchecked
			inStatelessTransaction( (Consumer<EntityAgent>) consumer );
		}
		else {
			throw new IllegalArgumentException( "Unknown EntityHandler type passed : " + handlerType.getName() );
		}
	}

	@Override
	public <R> R callInTransaction(@Nonnull Function<EntityManager, R> work) {
		return fromTransaction( work );
	}

	@Override
	public <R, H extends EntityHandler> R callInTransaction(
			@Nonnull Class<H> handlerType, @Nonnull Function<H, R> function) {
		if ( EntityManager.class.isAssignableFrom( handlerType ) ) {
			//noinspection unchecked
			return fromTransaction( (Function<EntityManager,R>) function );
		}
		else if ( EntityAgent.class.isAssignableFrom( handlerType ) ) {
			//noinspection unchecked
			return fromStatelessTransaction( (Function<EntityAgent,R>) function );
		}
		else {
			throw new IllegalArgumentException( "Unknown EntityHandler type passed : " + handlerType.getName() );
		}
	}

	@Override
	public boolean isClosed() {
		return status == Status.CLOSED;
	}

	private transient StatisticsImplementor statistics;

	@Override
	@Nonnull
	public StatisticsImplementor getStatistics() {
		if ( statistics == null ) {
			statistics = serviceRegistry.requireService( StatisticsImplementor.class );
		}
		return statistics;
	}

	@Override
	@Nonnull
	public FilterDefinition getFilterDefinition(@Nonnull String filterName) {
		final var filterDefinition = filters.get( filterName );
		if ( filterDefinition == null ) {
			throw new UnknownFilterException( filterName );
		}
		return filterDefinition;
	}

	@Override
	@Nonnull
	public Collection<FilterDefinition> getAutoEnabledFilters() {
		return autoEnabledFilters;
	}

	@Override
	@Nonnull
	public Set<String> getDefinedFilterNames() {
		return unmodifiableSet( filters.keySet() );
	}

	@Override
	public boolean containsFetchProfileDefinition(@Nonnull String name) {
		return sqlTranslationEngine.containsFetchProfileDefinition( name );
	}

	@Override
	@Nonnull
	public Set<String> getDefinedFetchProfileNames() {
		return sqlTranslationEngine.getDefinedFetchProfileNames();
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
	@Nonnull
	public RuntimeMetamodelsImplementor getRuntimeMetamodels() {
		return runtimeMetamodels;

	}

	@Override
	@Nonnull
	public JpaMetamodelImplementor getJpaMetamodel() {
		return runtimeMetamodels.getJpaMetamodel();
	}

	@Override
	@Nonnull
	public ServiceRegistryImplementor getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	@Nonnull
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
	@Nonnull
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return getSessionFactoryOptions().getCustomEntityDirtinessStrategy();
	}

	@Override
	@Nullable
	public CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver() {
		return getSessionFactoryOptions().getCurrentTenantIdentifierResolver();
	}

	@Override
	public JavaType<Object> getTenantIdentifierJavaType() {
		return tenantIdentifierJavaType;
	}

	boolean connectionProviderHandlesConnectionReadOnly() {
		return standardServiceComponents.multiTenantConnectionProvider() != null
				? standardServiceComponents.multiTenantConnectionProvider().handlesConnectionReadOnly()
				: standardServiceComponents.connectionProvider().handlesConnectionReadOnly();
	}

	boolean connectionProviderHandlesConnectionSchema() {
		return standardServiceComponents.multiTenantConnectionProvider() != null
				? standardServiceComponents.multiTenantConnectionProvider().handlesConnectionSchema()
				: standardServiceComponents.connectionProvider().handlesConnectionSchema();
	}

	TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return standardServiceComponents.transactionCoordinatorBuilder();
	}

	BatchBuilder getBatchBuilder() {
		return standardServiceComponents.batchBuilder();
	}

	ConnectionProvider getConnectionProvider() {
		return standardServiceComponents.connectionProvider();
	}

	MultiTenantConnectionProvider<Object> getMultiTenantConnectionProvider() {
		return standardServiceComponents.multiTenantConnectionProvider();
	}

	EventMonitor getEventMonitor() {
		return standardServiceComponents.eventMonitor();
	}

	@Override
	@Nonnull
	public ChangesetCoordinator getChangesetCoordinator() {
		return changesetCoordinator;
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
		SESSION_FACTORY_LOGGER.resolvingSerializedFactory();
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
	@Nonnull
	public WrapperOptions getWrapperOptions() {
		return wrapperOptions;
	}

	@Override
	@Nonnull
	public SchemaManager getSchemaManager() {
		return schemaManager;
	}

	@Override
	@Nonnull
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
		private final PlanningOptions graphPlanningOptions;
		private final RuntimeMappingHandoff runtimeMappingHandoff;
		private final QueryEngine queryEngine;
		private final WrapperOptions wrapperOptions;
		private final ChangesetCoordinator changesetCoordinator;
		private final SessionFactoryAccess sessionFactoryAccess;
		private final RuntimeModelHandoffResolvers handoffResolvers;

		private ModelCreationContext(
				BootstrapContext bootstrapContext,
				MetadataImplementor bootMetamodel,
				MappingMetamodelImplementor mappingMetamodel,
				TypeConfiguration typeConfiguration,
				PlanningOptions graphPlanningOptions,
				RuntimeMappingHandoff runtimeMappingHandoff,
				QueryEngine queryEngine,
				WrapperOptions wrapperOptions,
				ChangesetCoordinator changesetCoordinator,
				SessionFactoryAccess sessionFactoryAccess) {
			this.bootstrapContext = bootstrapContext;
			this.bootMetamodel = bootMetamodel;
			this.mappingMetamodel = mappingMetamodel;
			this.typeConfiguration = typeConfiguration;
			this.graphPlanningOptions = graphPlanningOptions;
			this.runtimeMappingHandoff = runtimeMappingHandoff;
			this.queryEngine = queryEngine;
			this.wrapperOptions = wrapperOptions;
			this.changesetCoordinator = changesetCoordinator;
			this.sessionFactoryAccess = sessionFactoryAccess;
			this.handoffResolvers = RuntimeModelHandoffResolvers.create( runtimeMappingHandoff );
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
		public SessionFactoryAccess getSessionFactoryAccess() {
			return sessionFactoryAccess;
		}

		@Override
		public MetadataImplementor getBootModel() {
			return bootMetamodel;
		}

		@Override
		public RuntimeMappingHandoff getRuntimeMappingHandoff() {
			return runtimeMappingHandoff;
		}

		@Override
		public RuntimeModelHandoffResolvers getHandoffResolvers() {
			return handoffResolvers;
		}

		@Override
		public MappingMetamodelImplementor getDomainModel() {
			return mappingMetamodel;
		}

		@Override
		public CacheImplementor getCache() {
			return cacheAccess;
		}

		public PlanningOptions getGraphPlanningOptions() {
			return graphPlanningOptions;
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
		public WrapperOptions getWrapperOptions() {
			return wrapperOptions;
		}

		@Override
		public ChangesetCoordinator getChangesetCoordinator() {
			return changesetCoordinator;
		}

		@Override
		public Generator getOrCreateIdGenerator(String rootName, PersistentClass persistentClass) {
			final var existing = getGenerators().get( rootName );
			if ( existing != null ) {
				return existing;
			}
			else {
				final var idGenerator = GeneratorBinder.createIdentifierGenerator(
						persistentClass.getIdentifier(),
						getDialect(),
						persistentClass.getRootClass(),
						persistentClass.getIdentifierProperty(),
						getGeneratorSettings(),
						getBootModel().getDatabase(),
						getServiceRegistry(),
						getServiceRegistry().requireService( PropertyAccessStrategyResolver.class )
				);
				getGenerators().put( rootName, idGenerator );
				return idGenerator;
			}
		}
	}
}
