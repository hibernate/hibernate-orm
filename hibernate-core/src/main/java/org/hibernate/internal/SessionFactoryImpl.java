/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.UnknownFilterException;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.event.spi.EventEngine;
import org.hibernate.generator.Generator;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.jpa.internal.ExceptionMapperLegacyJpaImpl;
import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.metamodel.internal.RuntimeMetamodelsImpl;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.SessionFactoryBasedWrapperOptions;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.internal.QueryEngineImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.relational.SchemaManager;
import org.hibernate.relational.internal.SchemaManagerImpl;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
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
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.hibernate.cfg.AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_VALIDATION_FACTORY;
import static org.hibernate.cfg.AvailableSettings.JPA_VALIDATION_FACTORY;
import static org.hibernate.cfg.PersistenceSettings.PERSISTENCE_UNIT_NAME;
import static org.hibernate.cfg.PersistenceSettings.SESSION_FACTORY_JNDI_NAME;
import static org.hibernate.engine.config.spi.StandardConverters.STRING;
import static org.hibernate.internal.FetchProfileHelper.getFetchProfiles;
import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
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
public class SessionFactoryImpl extends QueryParameterBindingTypeResolverImpl implements SessionFactoryImplementor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SessionFactoryImpl.class );

	private final String name;
	private final String jndiName;
	private final String uuid;

	private transient volatile Status status = Status.OPEN;

	private final transient SessionFactoryObserverChain observer = new SessionFactoryObserverChain();

	private final transient SessionFactoryOptions sessionFactoryOptions;
	private final transient Map<String,Object> settings;

	private final transient SessionFactoryServiceRegistry serviceRegistry;
	private final transient EventEngine eventEngine;//Needs to be closed!
	private final transient JdbcServices jdbcServices;
	private final transient SqlStringGenerationContext sqlStringGenerationContext;

	// todo : org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor too?

	private final transient RuntimeMetamodelsImplementor runtimeMetamodels;
	private final PersistenceUnitUtil jpaPersistenceUnitUtil;
	private final transient CacheImplementor cacheAccess;
	private final transient QueryEngine queryEngine;

	private final transient CurrentSessionContext currentSessionContext;

	// todo : move to MetamodelImpl
	private final transient Map<String, Generator> identifierGenerators;
	private final transient Map<String, FilterDefinition> filters;
	private final transient java.util.Collection<FilterDefinition> autoEnabledFilters = new HashSet<>();
	private final transient Map<String, FetchProfile> fetchProfiles;
	private final transient JavaType<Object> tenantIdentifierJavaType;

	private final transient FastSessionServices fastSessionServices;
	private final transient WrapperOptions wrapperOptions;
	private final transient SessionBuilderImpl defaultSessionOpenOptions;
	private final transient SessionBuilderImpl temporarySessionOpenOptions;
	private final transient StatelessSessionBuilder defaultStatelessOptions;
	private final transient EntityNameResolver entityNameResolver;

	private final transient SchemaManager schemaManager;

	public SessionFactoryImpl(
			final MetadataImplementor bootMetamodel,
			final SessionFactoryOptions options,
			final BootstrapContext bootstrapContext) {
		LOG.debug( "Building session factory" );
		final TypeConfiguration typeConfiguration = bootstrapContext.getTypeConfiguration();

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
		LOG.debugf( "Instantiating SessionFactory with settings: %s", settings);

		sqlStringGenerationContext = createSqlStringGenerationContext( bootMetamodel, options, jdbcServices );

		cacheAccess = serviceRegistry.getService( CacheImplementor.class );

		jpaPersistenceUnitUtil = new PersistenceUnitUtilImpl( this );

		for ( SessionFactoryObserver sessionFactoryObserver : options.getSessionFactoryObservers() ) {
			observer.addObserver( sessionFactoryObserver );
		}

		filters = new HashMap<>( bootMetamodel.getFilterDefinitions() );
		LOG.debugf( "Session factory constructed with filter configurations : %s", filters );

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
		for (Map.Entry<String, FilterDefinition> filterEntry : filters.entrySet()) {
			if (filterEntry.getValue().isAutoEnabled()) {
				autoEnabledFilters.add( filterEntry.getValue() );
			}
		}

		entityNameResolver = new CoordinatingEntityNameResolver( this, getInterceptor() );
		schemaManager = new SchemaManagerImpl( this, bootMetamodel );

		final IntegratorObserver integratorObserver = new IntegratorObserver();
		observer.addObserver( integratorObserver );
		try {
			integrate( bootMetamodel, bootstrapContext, integratorObserver );

			identifierGenerators = createGenerators( jdbcServices, sqlStringGenerationContext, bootMetamodel );
			bootMetamodel.orderColumns( false );
			bootMetamodel.validate();

			primeSecondLevelCacheRegions( bootMetamodel );

			// we build this before creating the runtime metamodel
			// because the persisters need the SqmFunctionRegistry
			// to translate SQL formulas ... but, if we fix Dialect
			// as I proposed, so that it can contribute functions
			// to the SqmFunctionRegistry before the QueryEngine is
			// created, then we can split creation of QueryEngine
			// and SqmFunctionRegistry, instantiating just the
			// registry here, and doing the engine later
			queryEngine = QueryEngineImpl.from( this, bootMetamodel );

			// create runtime metamodels (mapping and JPA)
			final RuntimeMetamodelsImpl runtimeMetamodelsImpl = new RuntimeMetamodelsImpl();
			runtimeMetamodels = runtimeMetamodelsImpl;
			final MappingMetamodelImpl mappingMetamodelImpl = new MappingMetamodelImpl( typeConfiguration, serviceRegistry );
			runtimeMetamodelsImpl.setMappingMetamodel( mappingMetamodelImpl );
			fastSessionServices = new FastSessionServices( this );
			initializeMappingModel( mappingMetamodelImpl, bootstrapContext, bootMetamodel, options );
			runtimeMetamodelsImpl.setJpaMetamodel( mappingMetamodelImpl.getJpaMetamodel() );

			// this needs to happen after the mapping metamodel is
			// completely built, since we need to use the persisters
			fetchProfiles = getFetchProfiles( bootMetamodel, runtimeMetamodels );

			defaultSessionOpenOptions = createDefaultSessionOpenOptionsIfPossible();
			temporarySessionOpenOptions = defaultSessionOpenOptions == null ? null : buildTemporarySessionOpenOptions();
			defaultStatelessOptions = defaultSessionOpenOptions == null ? null : withStatelessOptions();

			wrapperOptions = new SessionFactoryBasedWrapperOptions( this );

			currentSessionContext = buildCurrentSessionContext();

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
				LOG.debugf( "Eating error closing the SessionFactory after a failed attempt to start it" );
			}
			throw e;
		}

		LOG.debug( "Instantiated SessionFactory" );
	}

	private static void deprecationCheck(Map<String, Object> settings) {
		for ( String s:settings.keySet() ) {
			switch (s) {
				case "hibernate.hql.bulk_id_strategy.global_temporary.create_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.global_temporary.create_tables", GlobalTemporaryTableStrategy.CREATE_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.global_temporary.drop_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.global_temporary.drop_tables", GlobalTemporaryTableStrategy.DROP_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.persistent.create_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.create_tables", PersistentTableStrategy.CREATE_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.persistent.drop_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.drop_tables", PersistentTableStrategy.DROP_ID_TABLES );
				case "hibernate.hql.bulk_id_strategy.persistent.schema":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.schema", PersistentTableStrategy.SCHEMA );
				case "hibernate.hql.bulk_id_strategy.persistent.catalog":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.persistent.catalog", PersistentTableStrategy.CATALOG );
				case "hibernate.hql.bulk_id_strategy.local_temporary.drop_tables":
					DEPRECATION_LOGGER.deprecatedSetting( "hibernate.hql.bulk_id_strategy.local_temporary.drop_tables", LocalTemporaryTableStrategy.DROP_ID_TABLES );
			}
		}
	}

	private void initializeMappingModel(
			MappingMetamodelImpl mappingMetamodelImpl,
			BootstrapContext bootstrapContext,
			MetadataImplementor bootMetamodel,
			SessionFactoryOptions options) {
		final TypeConfiguration typeConfiguration = mappingMetamodelImpl.getTypeConfiguration();
		mappingMetamodelImpl.finishInitialization( runtimeModelCreationContext(
				bootstrapContext,
				bootMetamodel,
				mappingMetamodelImpl,
				typeConfiguration,
				options
		) );
	}

	private RuntimeModelCreationContext runtimeModelCreationContext(
			BootstrapContext bootstrapContext,
			MetadataImplementor bootMetamodel,
			MappingMetamodelImplementor mappingMetamodel,
			TypeConfiguration typeConfiguration,
			SessionFactoryOptions options) {
		return new RuntimeModelCreationContext() {
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
				return options;
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
			public ServiceRegistry getServiceRegistry() {
				return serviceRegistry;
			}
		};
	}

	private static Map<String, Generator> createGenerators(
			JdbcServices jdbcServices,
			SqlStringGenerationContext sqlStringGenerationContext,
			MetadataImplementor bootMetamodel) {
		final Dialect dialect = jdbcServices.getJdbcEnvironment().getDialect();
		final Map<String, Generator> generators = new HashMap<>();
		for ( PersistentClass model : bootMetamodel.getEntityBindings() ) {
			if ( !model.isInherited() ) {
				final KeyValue id = model.getIdentifier();
				final Generator generator =
						id.createGenerator( dialect, (RootClass) model, model.getIdentifierProperty() );
				if ( generator instanceof Configurable configurable ) {
					configurable.initialize( sqlStringGenerationContext );
				}
				//TODO: this isn't a great place to do this
				if ( generator.allowAssignedIdentifiers()
						&& id instanceof SimpleValue simpleValue
						&& simpleValue.getNullValue() == null ) {
					simpleValue.setNullValue( "undefined" );
				}
				generators.put( model.getEntityName(), generator );
			}
		}
		return generators;
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
		return options
				.getServiceRegistry()
				.requireService( SessionFactoryServiceRegistryFactory.class )
				// it is not great how we pass in an instance to
				// an incompletely-initialized instance here:
				.buildServiceRegistry( self, options );
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
			integrator.integrate(bootMetamodel, bootstrapContext, this );
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


	private static Map<String, Object> getSettings(SessionFactoryOptions options, SessionFactoryServiceRegistry serviceRegistry) {
		final Map<String, Object> settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		final Map<String,Object> result = new HashMap<>( settings );
		if ( !settings.containsKey( JPA_VALIDATION_FACTORY ) && !settings.containsKey( JAKARTA_VALIDATION_FACTORY ) ) {
			final Object reference = options.getValidatorFactoryReference();
			if ( reference != null ) {
				result.put( JPA_VALIDATION_FACTORY, reference );
				result.put( JAKARTA_VALIDATION_FACTORY, reference );
			}
		}
		return result;
	}

	private static String getSessionFactoryName(SessionFactoryOptions options, SessionFactoryServiceRegistry serviceRegistry) {
		final String sessionFactoryName = options.getSessionFactoryName();
		if ( sessionFactoryName != null ) {
			return sessionFactoryName;
		}

		final CfgXmlAccessService cfgXmlAccessService = serviceRegistry.requireService( CfgXmlAccessService.class );
		if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
			final String nameFromAggregation = cfgXmlAccessService.getAggregatedConfig().getSessionFactoryName();
			if ( nameFromAggregation != null ) {
				return nameFromAggregation;
			}
		}


		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		assert configurationService != null;
		return configurationService.getSetting( PersistenceSettings.PERSISTENCE_UNIT_NAME, STRING );
	}

	private String determineJndiName(
			String name,
			SessionFactoryOptions options,
			SessionFactoryServiceRegistry serviceRegistry) {
		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		assert cfgService != null;
		final String explicitJndiName = cfgService.getSetting( SESSION_FACTORY_JNDI_NAME, STRING );
		if ( isNotEmpty( explicitJndiName ) ) {
			return explicitJndiName;
		}

		final String puName = cfgService.getSetting( PERSISTENCE_UNIT_NAME, STRING );
		// do not use name for JNDI if explicitly asked not to or if name comes from JPA persistence-unit name
		final boolean nameIsNotJndiName =
				options.isSessionFactoryNameAlsoJndiName() == Boolean.FALSE
						|| isNotEmpty( puName );
		return !nameIsNotJndiName ? name : null;
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

		// todo : ultimately this code can be made more efficient when we have a better intrinsic understanding of the hierarchy as a whole

		for ( PersistentClass bootEntityDescriptor : mappingMetadata.getEntityBindings() ) {
			final AccessType accessType = AccessType.fromExternalName( bootEntityDescriptor.getCacheConcurrencyStrategy() );

			if ( accessType != null ) {
				if ( bootEntityDescriptor.isCached() ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getRootClass().getCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addEntityConfig( bootEntityDescriptor, accessType );
				}

				if ( bootEntityDescriptor instanceof RootClass
						&& bootEntityDescriptor.hasNaturalId()
						&& bootEntityDescriptor.getNaturalIdCacheRegionName() != null ) {
					regionConfigBuilders.computeIfAbsent(
							bootEntityDescriptor.getNaturalIdCacheRegionName(),
							DomainDataRegionConfigImpl.Builder::new
					)
							.addNaturalIdConfig( (RootClass) bootEntityDescriptor, accessType );
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
	public SessionImplementor openSession() throws HibernateException {
		//The defaultSessionOpenOptions can't be used in some cases; for example when using a TenantIdentifierResolver.
		if ( defaultSessionOpenOptions != null ) {
			return defaultSessionOpenOptions.openSession();
		}
		else {
			return withOptions().openSession();
		}
	}

	@Override
	public SessionImpl openTemporarySession() throws HibernateException {
		//The temporarySessionOpenOptions can't be used in some cases; for example when using a TenantIdentifierResolver.
		if ( temporarySessionOpenOptions != null ) {
			return temporarySessionOpenOptions.openSession();
		}
		else {
			return buildTemporarySessionOpenOptions().openSession();
		}
	}

	@Override
	public Session getCurrentSession() throws HibernateException {
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
		if ( defaultStatelessOptions != null ) {
			return defaultStatelessOptions.openStatelessSession();
		}
		else {
			return withStatelessOptions().openStatelessSession();
		}
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
		return runtimeMetamodels.getMappingMetamodel().getTypeConfiguration();
	}

	@Override
	public QueryEngine getQueryEngine() {
		return queryEngine;
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

	@Override @SuppressWarnings({"rawtypes","unchecked"})
	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		return (List) getJpaMetamodel().findEntityGraphsByJavaType( entityClass );
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
		if ( !getServiceRegistry().requireService( TransactionCoordinatorBuilder.class ).isJta() ) {
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

	@Override @Deprecated
	public MetamodelImplementor getMetamodel() {
		validateNotClosed();
		return (MetamodelImplementor) runtimeMetamodels.getMappingMetamodel();
	}

	@Override
	public boolean isOpen() {
		return status != Status.CLOSED;
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
		LOG.debug( "Returning a Reference to the SessionFactory" );
		return new Reference(
				SessionFactoryImpl.class.getName(),
				new StringRefAddr("uuid", getUuid()),
				SessionFactoryRegistry.ObjectFactoryImpl.class.getName(),
				null
		);
	}

	@Override
	public Type getIdentifierType(String className) throws MappingException {
		return runtimeMetamodels.getMappingMetamodel().getEntityDescriptor( className ).getIdentifierType();
	}

	@Override
	public String getIdentifierPropertyName(String className) throws MappingException {
		return runtimeMetamodels.getMappingMetamodel().getEntityDescriptor( className ).getIdentifierPropertyName();
	}

	@Override
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
		return runtimeMetamodels.getMappingMetamodel().getEntityDescriptor( className ).getPropertyType( propertyName );
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
	public void close() throws HibernateException {
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
			LOG.closing();
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
								entityPersister.getSqmMultiTableMutationStrategy().release(
										this,
										jdbcConnectionAccess
								);
							}
							if ( entityPersister.getSqmMultiTableInsertStrategy() != null ) {
								entityPersister.getSqmMultiTableInsertStrategy().release(
										this,
										jdbcConnectionAccess
								);
							}
						}
				);
				( (MappingMetamodelImpl) runtimeMetamodels.getMappingMetamodel() ).close();
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
		return fastSessionServices.transactionCoordinatorBuilder.isJta()
				? PersistenceUnitTransactionType.JTA
				: PersistenceUnitTransactionType.RESOURCE_LOCAL;

	}

	@Override
	public void addNamedQuery(String name, Query query) {
		validateNotClosed();

		// NOTE : we use Query#unwrap here (rather than direct type checking) to account for possibly wrapped
		// query implementations

		// first, handle StoredProcedureQuery
		final NamedObjectRepository namedObjectRepository = getQueryEngine().getNamedObjectRepository();
		try {
			final ProcedureCallImplementor<?> unwrapped = query.unwrap( ProcedureCallImplementor.class );
			if ( unwrapped != null ) {
				namedObjectRepository.registerCallableQueryMemento( name, unwrapped.toMemento( name ) );
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a ProcedureCallImplementor
		}

		// then try as a native-SQL or JPQL query
		try {
			final QueryImplementor<?> hibernateQuery = query.unwrap( QueryImplementor.class );
			if ( hibernateQuery != null ) {
				// create and register the proper NamedQueryDefinition...
				if ( hibernateQuery instanceof NativeQueryImplementor ) {
					namedObjectRepository.registerNativeQueryMemento(
							name,
							( (NativeQueryImplementor<?>) hibernateQuery ).toMemento( name )
					);

				}
				else {
					final NamedQueryMemento namedQueryMemento =
							( (SqmQueryImplementor<?>) hibernateQuery ).toMemento( name );
					namedObjectRepository.registerSqmQueryMemento(
							name,
							(NamedSqmQueryMemento) namedQueryMemento
					);
				}
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a native-SQL or JPQL query
		}

		// if we get here, we are unsure how to properly unwrap the incoming query to extract the needed information
		throw new PersistenceException(
				String.format(
						"Unsure how to properly unwrap given Query [%s] as basis for named query",
						query
				)
		);
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

	@Override @SuppressWarnings({"unchecked", "rawtypes"})
	public void runInTransaction(Consumer<EntityManager> work) {
		inTransaction( (Consumer) work );
	}

	@Override
	public <R> R callInTransaction(Function<EntityManager, R> work) {
		//noinspection unchecked,rawtypes
		return (R) fromTransaction( (Function) work );
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

	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
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
	public boolean containsFetchProfileDefinition(String name) {
		return fetchProfiles.containsKey( name );
	}

	@Override
	public Set<String> getDefinedFilterNames() {
		return unmodifiableSet( filters.keySet() );
	}

	@Override
	public Set<String> getDefinedFetchProfileNames() {
		return unmodifiableSet( fetchProfiles.keySet() );
	}

	@Override @Deprecated
	public Generator getGenerator(String rootEntityName) {
		return identifierGenerators.get( rootEntityName );
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
							serviceRegistry.requireService( ClassLoaderService.class )
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
	public Integer getMaximumFetchDepth() {
		return getSessionFactoryOptions().getMaximumFetchDepth();
	}

	@Override
	public ServiceRegistryImplementor getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return sessionFactoryOptions.getEntityNotFoundDelegate();
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return fetchProfiles.get( name );
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
			this.statementInspector = sessionFactoryOptions.getStatementInspector();
			this.connectionHandlingMode = sessionFactoryOptions.getPhysicalConnectionHandlingMode();
			this.autoClose = sessionFactoryOptions.isAutoCloseSessionEnabled();
			this.defaultBatchFetchSize = sessionFactoryOptions.getDefaultBatchFetchSize();
			this.subselectFetchEnabled = sessionFactoryOptions.isSubselectFetchEnabled();

			final CurrentTenantIdentifierResolver<Object> currentTenantIdentifierResolver =
					sessionFactory.getCurrentTenantIdentifierResolver();
			if ( currentTenantIdentifierResolver != null ) {
				tenantIdentifier = currentTenantIdentifierResolver.resolveCurrentTenantIdentifier();
			}
			this.jdbcTimeZone = sessionFactoryOptions.getJdbcTimeZone();
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

		@Override
		public SessionBuilderImpl statementInspector(StatementInspector statementInspector) {
			this.statementInspector = statementInspector;
			return this;
		}

		@Override
		public SessionBuilderImpl connection(Connection connection) {
			this.connection = connection;
			return this;
		}

		@Override
		public SessionBuilderImpl connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
			this.connectionHandlingMode = connectionHandlingMode;
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
		public SessionBuilderImpl eventListeners(SessionEventListener... listeners) {
			if ( this.listeners == null ) {
				this.listeners = sessionFactory.getSessionFactoryOptions()
						.getBaselineSessionEventsListenerBuilder()
						.buildBaselineList();
			}
			Collections.addAll( this.listeners, listeners );
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

			CurrentTenantIdentifierResolver<Object> tenantIdentifierResolver = sessionFactory.getCurrentTenantIdentifierResolver();
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

		@Override
		public StatelessSessionBuilder statementInspector(StatementInspector statementInspector) {
			this.statementInspector = statementInspector;
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
			return configuredInterceptor( EmptyInterceptor.INSTANCE, false, sessionFactory.getSessionFactoryOptions() );

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
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Serializing: %s", getUuid() );
		}
		out.defaultWriteObject();
		LOG.trace( "Serialized" );
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
		LOG.trace( "Deserializing" );
		in.defaultReadObject();
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Deserialized: %s", getUuid() );
		}
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
		LOG.trace( "Resolving serialized SessionFactory" );
		return locateSessionFactoryOnDeserialization( getUuid(), name );
	}

	private static SessionFactory locateSessionFactoryOnDeserialization(String uuid, String name)
			throws InvalidObjectException{
		final SessionFactory uuidResult = SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid );
		if ( uuidResult != null ) {
			LOG.debugf( "Resolved SessionFactory by UUID [%s]", uuid );
			return uuidResult;
		}

		// in case we were deserialized in a different JVM, look for an instance with the same name
		// (provided we were given a name)
		if ( name != null ) {
			final SessionFactory namedResult = SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( name );
			if ( namedResult != null ) {
				LOG.debugf( "Resolved SessionFactory by name [%s]", name );
				return namedResult;
			}
		}

		throw new InvalidObjectException( "Could not find a SessionFactory [uuid=" + uuid + ",name=" + name + "]" );
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
		LOG.trace( "Deserializing SessionFactory from Session" );
		final String uuid = ois.readUTF();
		boolean isNamed = ois.readBoolean();
		final String name = isNamed ? ois.readUTF() : null;
		return (SessionFactoryImpl) locateSessionFactoryOnDeserialization( uuid, name );
	}

	private static void maskOutSensitiveInformation(Map<String, Object> props) {
		maskOutIfSet( props, AvailableSettings.JPA_JDBC_USER );
		maskOutIfSet( props, AvailableSettings.JPA_JDBC_PASSWORD );
		maskOutIfSet( props, AvailableSettings.JAKARTA_JDBC_USER );
		maskOutIfSet( props, AvailableSettings.JAKARTA_JDBC_PASSWORD );
		maskOutIfSet( props, AvailableSettings.USER );
		maskOutIfSet( props, AvailableSettings.PASS );
	}

	private static void maskOutIfSet(Map<String, Object> props, String setting) {
		if ( props.containsKey( setting ) ) {
			props.put( setting, "****" );
		}
	}

	/**
	 * @return the {@link FastSessionServices} for this {@code SessionFactory}.
	 */
	@Override
	public FastSessionServices getFastSessionServices() {
		return this.fastSessionServices;
	}

	@Override
	public WrapperOptions getWrapperOptions() {
		return wrapperOptions;
	}

	@Override
	public SchemaManager getSchemaManager() {
		return schemaManager;
	}

	private enum Status {
		OPEN,
		CLOSING,
		CLOSED
	}
}
