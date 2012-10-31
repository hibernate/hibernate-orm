/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.Cache;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.spi.CacheImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.Association;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;
import org.hibernate.service.config.spi.ConfigurationService;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.jndi.spi.JndiService;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;


/**
 * Concrete implementation of the <tt>SessionFactory</tt> interface. Has the following
 * responsibilities
 * <ul>
 * <li>caches configuration settings (immutably)
 * <li>caches "compiled" mappings ie. <tt>EntityPersister</tt>s and
 *     <tt>CollectionPersister</tt>s (immutable)
 * <li>caches "compiled" queries (memory sensitive cache)
 * <li>manages <tt>PreparedStatement</tt>s
 * <li> delegates JDBC <tt>Connection</tt> management to the <tt>ConnectionProvider</tt>
 * <li>factory for instances of <tt>SessionImpl</tt>
 * </ul>
 * This class must appear immutable to clients, even if it does all kinds of caching
 * and pooling under the covers. It is crucial that the class is not only thread
 * safe, but also highly concurrent. Synchronization must be used extremely sparingly.
 *
 * @see org.hibernate.service.jdbc.connections.spi.ConnectionProvider
 * @see org.hibernate.Session
 * @see org.hibernate.hql.spi.QueryTranslator
 * @see org.hibernate.persister.entity.EntityPersister
 * @see org.hibernate.persister.collection.CollectionPersister
 * @author Gavin King
 */
public final class SessionFactoryImpl
		implements SessionFactoryImplementor {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SessionFactoryImpl.class.getName());
	private static final IdentifierGenerator UUID_GENERATOR = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();

	private final String name;
	private final String uuid;

	private final transient Map<String,EntityPersister> entityPersisters;
	private final transient Map<String,ClassMetadata> classMetadata;
	private final transient Map<String,CollectionPersister> collectionPersisters;
	private final transient Map<String,CollectionMetadata> collectionMetadata;
	private final transient Map<String,Set<String>> collectionRolesByEntityParticipant;
	private final transient Map<String,IdentifierGenerator> identifierGenerators;
	private final transient Map<String, NamedQueryDefinition> namedQueries;
	private final transient Map<String, NamedSQLQueryDefinition> namedSqlQueries;
	private final transient Map<String, ResultSetMappingDefinition> sqlResultSetMappings;
	private final transient Map<String, FilterDefinition> filters;
	private final transient Map<String, FetchProfile> fetchProfiles;
	private final transient Map<String,String> imports;
	private final transient SessionFactoryServiceRegistry serviceRegistry;
	private final transient JdbcServices jdbcServices;
	private final transient Dialect dialect;
	private final transient Settings settings;
	private final transient Properties properties;
	private transient SchemaExport schemaExport;
	private final transient CurrentSessionContext currentSessionContext;
	private final transient SQLFunctionRegistry sqlFunctionRegistry;
	private final transient SessionFactoryObserverChain observer = new SessionFactoryObserverChain();
	private final transient ConcurrentHashMap<EntityNameResolver,Object> entityNameResolvers = new ConcurrentHashMap<EntityNameResolver, Object>();
	private final transient QueryPlanCache queryPlanCache;
	private final transient CacheImplementor cacheAccess;
	private transient boolean isClosed = false;
	private final transient TypeResolver typeResolver;
	private final transient TypeHelper typeHelper;
	private final transient TransactionEnvironment transactionEnvironment;
	private final transient SessionFactoryOptions sessionFactoryOptions;
	private final transient CustomEntityDirtinessStrategy customEntityDirtinessStrategy;
	private final transient CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

	@SuppressWarnings( {"unchecked", "ThrowableResultOfMethodCallIgnored"})
	public SessionFactoryImpl(
			final Configuration cfg,
			Mapping mapping,
			ServiceRegistry serviceRegistry,
			Settings settings,
			SessionFactoryObserver observer) throws HibernateException {
			LOG.debug( "Building session factory" );

		sessionFactoryOptions = new SessionFactoryOptions() {
			private EntityNotFoundDelegate entityNotFoundDelegate;

			@Override
			public Interceptor getInterceptor() {
				return cfg.getInterceptor();
			}

			@Override
			public EntityNotFoundDelegate getEntityNotFoundDelegate() {
				if ( entityNotFoundDelegate == null ) {
					if ( cfg.getEntityNotFoundDelegate() != null ) {
						entityNotFoundDelegate = cfg.getEntityNotFoundDelegate();
					}
					else {
						entityNotFoundDelegate = new EntityNotFoundDelegate() {
							public void handleEntityNotFound(String entityName, Serializable id) {
								throw new ObjectNotFoundException( id, entityName );
							}
						};
					}
				}
				return entityNotFoundDelegate;
			}
		};

		this.settings = settings;

		this.properties = new Properties();
		this.properties.putAll( cfg.getProperties() );

		this.serviceRegistry = serviceRegistry.getService( SessionFactoryServiceRegistryFactory.class ).buildServiceRegistry(
				this,
				cfg
		);
        this.jdbcServices = this.serviceRegistry.getService( JdbcServices.class );
        this.dialect = this.jdbcServices.getDialect();
		this.cacheAccess = this.serviceRegistry.getService( CacheImplementor.class );
		final RegionFactory regionFactory = cacheAccess.getRegionFactory();
		this.sqlFunctionRegistry = new SQLFunctionRegistry( getDialect(), cfg.getSqlFunctions() );
		if ( observer != null ) {
			this.observer.addObserver( observer );
		}

		this.typeResolver = cfg.getTypeResolver().scope( this );
		this.typeHelper = new TypeLocatorImpl( typeResolver );

		this.filters = new HashMap<String, FilterDefinition>();
		this.filters.putAll( cfg.getFilterDefinitions() );

		LOG.debugf( "Session factory constructed with filter configurations : %s", filters );
		LOG.debugf( "Instantiating session factory with properties: %s", properties );


		this.queryPlanCache = new QueryPlanCache( this );

		// todo : everything above here consider implementing as standard SF service.  specifically: stats, caches, types, function-reg

		class IntegratorObserver implements SessionFactoryObserver {
			private ArrayList<Integrator> integrators = new ArrayList<Integrator>();

			@Override
			public void sessionFactoryCreated(SessionFactory factory) {
			}

			@Override
			public void sessionFactoryClosed(SessionFactory factory) {
				for ( Integrator integrator : integrators ) {
					integrator.disintegrate( SessionFactoryImpl.this, SessionFactoryImpl.this.serviceRegistry );
				}
			}
		}

		final IntegratorObserver integratorObserver = new IntegratorObserver();
		this.observer.addObserver( integratorObserver );
		for ( Integrator integrator : serviceRegistry.getService( IntegratorService.class ).getIntegrators() ) {
			integrator.integrate( cfg, this, this.serviceRegistry );
			integratorObserver.integrators.add( integrator );
		}

		//Generators:

		identifierGenerators = new HashMap();
		Iterator classes = cfg.getClassMappings();
		while ( classes.hasNext() ) {
			PersistentClass model = (PersistentClass) classes.next();
			if ( !model.isInherited() ) {
				IdentifierGenerator generator = model.getIdentifier().createIdentifierGenerator(
						cfg.getIdentifierGeneratorFactory(),
						getDialect(),
				        settings.getDefaultCatalogName(),
				        settings.getDefaultSchemaName(),
				        (RootClass) model
				);
				identifierGenerators.put( model.getEntityName(), generator );
			}
		}


		///////////////////////////////////////////////////////////////////////
		// Prepare persisters and link them up with their cache
		// region/access-strategy

		final String cacheRegionPrefix = settings.getCacheRegionPrefix() == null ? "" : settings.getCacheRegionPrefix() + ".";

		entityPersisters = new HashMap();
		Map entityAccessStrategies = new HashMap();
		Map<String,ClassMetadata> classMeta = new HashMap<String,ClassMetadata>();
		classes = cfg.getClassMappings();
		while ( classes.hasNext() ) {
			final PersistentClass model = (PersistentClass) classes.next();
			model.prepareTemporaryTables( mapping, getDialect() );
			final String cacheRegionName = cacheRegionPrefix + model.getRootClass().getCacheRegionName();
			// cache region is defined by the root-class in the hierarchy...
			EntityRegionAccessStrategy accessStrategy = ( EntityRegionAccessStrategy ) entityAccessStrategies.get( cacheRegionName );
			if ( accessStrategy == null && settings.isSecondLevelCacheEnabled() ) {
				final AccessType accessType = AccessType.fromExternalName( model.getCacheConcurrencyStrategy() );
				if ( accessType != null ) {
					LOG.tracef( "Building shared cache region for entity data [%s]", model.getEntityName() );
					EntityRegion entityRegion = regionFactory.buildEntityRegion( cacheRegionName, properties, CacheDataDescriptionImpl.decode( model ) );
					accessStrategy = entityRegion.buildAccessStrategy( accessType );
					entityAccessStrategies.put( cacheRegionName, accessStrategy );
					cacheAccess.addCacheRegion( cacheRegionName, entityRegion );
				}
			}
			
			NaturalIdRegionAccessStrategy naturalIdAccessStrategy = null;
			if ( model.hasNaturalId() && model.getNaturalIdCacheRegionName() != null ) {
				final String naturalIdCacheRegionName = cacheRegionPrefix + model.getNaturalIdCacheRegionName();
				naturalIdAccessStrategy = ( NaturalIdRegionAccessStrategy ) entityAccessStrategies.get( naturalIdCacheRegionName );
				
				if ( naturalIdAccessStrategy == null && settings.isSecondLevelCacheEnabled() ) {
					final CacheDataDescriptionImpl cacheDataDescription = CacheDataDescriptionImpl.decode( model );
					
					NaturalIdRegion naturalIdRegion = null;
					try {
						naturalIdRegion = regionFactory.buildNaturalIdRegion( naturalIdCacheRegionName, properties,
								cacheDataDescription );
					}
					catch ( UnsupportedOperationException e ) {
						LOG.warnf(
								"Shared cache region factory [%s] does not support natural id caching; " +
										"shared NaturalId caching will be disabled for not be enabled for %s",
								regionFactory.getClass().getName(),
								model.getEntityName()
						);
					}
					
					if (naturalIdRegion != null) {
						naturalIdAccessStrategy = naturalIdRegion.buildAccessStrategy( regionFactory.getDefaultAccessType() );
						entityAccessStrategies.put( naturalIdCacheRegionName, naturalIdAccessStrategy );
						cacheAccess.addCacheRegion(  naturalIdCacheRegionName, naturalIdRegion );
					}
				}
			}
			
			EntityPersister cp = serviceRegistry.getService( PersisterFactory.class ).createEntityPersister(
					model,
					accessStrategy,
					naturalIdAccessStrategy,
					this,
					mapping
			);
			entityPersisters.put( model.getEntityName(), cp );
			classMeta.put( model.getEntityName(), cp.getClassMetadata() );
		}
		this.classMetadata = Collections.unmodifiableMap(classMeta);

		Map<String,Set<String>> tmpEntityToCollectionRoleMap = new HashMap<String,Set<String>>();
		collectionPersisters = new HashMap<String,CollectionPersister>();
		Map<String,CollectionMetadata> tmpCollectionMetadata = new HashMap<String,CollectionMetadata>();
		Iterator collections = cfg.getCollectionMappings();
		while ( collections.hasNext() ) {
			Collection model = (Collection) collections.next();
			final String cacheRegionName = cacheRegionPrefix + model.getCacheRegionName();
			final AccessType accessType = AccessType.fromExternalName( model.getCacheConcurrencyStrategy() );
			CollectionRegionAccessStrategy accessStrategy = null;
			if ( accessType != null && settings.isSecondLevelCacheEnabled() ) {
				LOG.tracev( "Building shared cache region for collection data [{0}]", model.getRole() );
				CollectionRegion collectionRegion = regionFactory.buildCollectionRegion( cacheRegionName, properties, CacheDataDescriptionImpl
						.decode( model ) );
				accessStrategy = collectionRegion.buildAccessStrategy( accessType );
				entityAccessStrategies.put( cacheRegionName, accessStrategy );
				cacheAccess.addCacheRegion( cacheRegionName, collectionRegion );
			}
			CollectionPersister persister = serviceRegistry.getService( PersisterFactory.class ).createCollectionPersister(
					cfg,
					model,
					accessStrategy,
					this
			) ;
			collectionPersisters.put( model.getRole(), persister );
			tmpCollectionMetadata.put( model.getRole(), persister.getCollectionMetadata() );
			Type indexType = persister.getIndexType();
			if ( indexType != null && indexType.isAssociationType() && !indexType.isAnyType() ) {
				String entityName = ( ( AssociationType ) indexType ).getAssociatedEntityName( this );
				Set roles = tmpEntityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet();
					tmpEntityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType.isAssociationType() && !elementType.isAnyType() ) {
				String entityName = ( ( AssociationType ) elementType ).getAssociatedEntityName( this );
				Set roles = tmpEntityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet();
					tmpEntityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}
		collectionMetadata = Collections.unmodifiableMap( tmpCollectionMetadata );
		Iterator itr = tmpEntityToCollectionRoleMap.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			entry.setValue( Collections.unmodifiableSet( ( Set ) entry.getValue() ) );
		}
		collectionRolesByEntityParticipant = Collections.unmodifiableMap( tmpEntityToCollectionRoleMap );

		//Named Queries:
		namedQueries = new HashMap<String, NamedQueryDefinition>( cfg.getNamedQueries() );
		namedSqlQueries = new HashMap<String, NamedSQLQueryDefinition>( cfg.getNamedSQLQueries() );
		sqlResultSetMappings = new HashMap<String, ResultSetMappingDefinition>( cfg.getSqlResultSetMappings() );
		imports = new HashMap<String,String>( cfg.getImports() );

		// after *all* persisters and named queries are registered
		Iterator iter = entityPersisters.values().iterator();
		while ( iter.hasNext() ) {
			final EntityPersister persister = ( ( EntityPersister ) iter.next() );
			persister.postInstantiate();
			registerEntityNameResolvers( persister );

		}
		iter = collectionPersisters.values().iterator();
		while ( iter.hasNext() ) {
			final CollectionPersister persister = ( ( CollectionPersister ) iter.next() );
			persister.postInstantiate();
		}

		//JNDI + Serialization:

		name = settings.getSessionFactoryName();
		try {
			uuid = (String) UUID_GENERATOR.generate(null, null);
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not generate UUID");
		}
		SessionFactoryRegistry.INSTANCE.addSessionFactory(
				uuid,
				name,
				settings.isSessionFactoryNameAlsoJndiName(),
				this,
				serviceRegistry.getService( JndiService.class )
		);

		LOG.debug( "Instantiated session factory" );

		settings.getMultiTableBulkIdStrategy().prepare(
				jdbcServices,
				buildLocalConnectionAccess(),
				cfg.createMappings(),
				cfg.buildMapping(),
				properties
		);


		if ( settings.isAutoCreateSchema() ) {
			new SchemaExport( serviceRegistry, cfg )
					.setImportSqlCommandExtractor( serviceRegistry.getService( ImportSqlCommandExtractor.class ) )
					.create( false, true );
		}
		if ( settings.isAutoUpdateSchema() ) {
			new SchemaUpdate( serviceRegistry, cfg ).execute( false, true );
		}
		if ( settings.isAutoValidateSchema() ) {
			new SchemaValidator( serviceRegistry, cfg ).validate();
		}
		if ( settings.isAutoDropSchema() ) {
			schemaExport = new SchemaExport( serviceRegistry, cfg )
					.setImportSqlCommandExtractor( serviceRegistry.getService( ImportSqlCommandExtractor.class ) );
		}

		currentSessionContext = buildCurrentSessionContext();

		//checking for named queries
		if ( settings.isNamedQueryStartupCheckingEnabled() ) {
			final Map<String,HibernateException> errors = checkNamedQueries();
			if ( ! errors.isEmpty() ) {
				StringBuilder failingQueries = new StringBuilder( "Errors in named queries: " );
				String sep = "";
				for ( Map.Entry<String,HibernateException> entry : errors.entrySet() ) {
					LOG.namedQueryError( entry.getKey(), entry.getValue() );
					failingQueries.append( entry.getKey() ).append( sep );
					sep = ", ";
				}
				throw new HibernateException( failingQueries.toString() );
			}
		}

		// this needs to happen after persisters are all ready to go...
		this.fetchProfiles = new HashMap();
		itr = cfg.iterateFetchProfiles();
		while ( itr.hasNext() ) {
			final org.hibernate.mapping.FetchProfile mappingProfile =
					( org.hibernate.mapping.FetchProfile ) itr.next();
			final FetchProfile fetchProfile = new FetchProfile( mappingProfile.getName() );
			for ( org.hibernate.mapping.FetchProfile.Fetch mappingFetch : mappingProfile.getFetches() ) {
				// resolve the persister owning the fetch
				final String entityName = getImportedClassName( mappingFetch.getEntity() );
				final EntityPersister owner = entityName == null
						? null
						: entityPersisters.get( entityName );
				if ( owner == null ) {
					throw new HibernateException(
							"Unable to resolve entity reference [" + mappingFetch.getEntity()
									+ "] in fetch profile [" + fetchProfile.getName() + "]"
					);
				}

				// validate the specified association fetch
				Type associationType = owner.getPropertyType( mappingFetch.getAssociation() );
				if ( associationType == null || !associationType.isAssociationType() ) {
					throw new HibernateException( "Fetch profile [" + fetchProfile.getName() + "] specified an invalid association" );
				}

				// resolve the style
				final Fetch.Style fetchStyle = Fetch.Style.parse( mappingFetch.getStyle() );

				// then construct the fetch instance...
				fetchProfile.addFetch( new Association( owner, mappingFetch.getAssociation() ), fetchStyle );
				((Loadable) owner).registerAffectingFetchProfile( fetchProfile.getName() );
			}
			fetchProfiles.put( fetchProfile.getName(), fetchProfile );
		}

		this.customEntityDirtinessStrategy = determineCustomEntityDirtinessStrategy();
		this.currentTenantIdentifierResolver = determineCurrentTenantIdentifierResolver( cfg.getCurrentTenantIdentifierResolver() );
		this.transactionEnvironment = new TransactionEnvironmentImpl( this );
		this.observer.sessionFactoryCreated( this );
	}

	private JdbcConnectionAccess buildLocalConnectionAccess() {
		return new JdbcConnectionAccess() {
			@Override
			public Connection obtainConnection() throws SQLException {
				return settings.getMultiTenancyStrategy() == MultiTenancyStrategy.NONE
						? serviceRegistry.getService( ConnectionProvider.class ).getConnection()
						: serviceRegistry.getService( MultiTenantConnectionProvider.class ).getAnyConnection();
			}

			@Override
			public void releaseConnection(Connection connection) throws SQLException {
				if ( settings.getMultiTenancyStrategy() == MultiTenancyStrategy.NONE ) {
					serviceRegistry.getService( ConnectionProvider.class ).closeConnection( connection );
				}
				else {
					serviceRegistry.getService( MultiTenantConnectionProvider.class ).releaseAnyConnection( connection );
				}
			}

			@Override
			public boolean supportsAggressiveRelease() {
				return false;
			}
		};
	}

	@SuppressWarnings({ "unchecked" })
	private CustomEntityDirtinessStrategy determineCustomEntityDirtinessStrategy() {
		CustomEntityDirtinessStrategy defaultValue = new CustomEntityDirtinessStrategy() {
			@Override
			public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
				return false;
			}

			@Override
			public boolean isDirty(Object entity, EntityPersister persister, Session session) {
				return false;
			}

			@Override
			public void resetDirty(Object entity, EntityPersister persister, Session session) {
			}

			@Override
			public void findDirty(
					Object entity,
					EntityPersister persister,
					Session session,
					DirtyCheckContext dirtyCheckContext) {
				// todo : implement proper method body
			}
		};
		return serviceRegistry.getService( ConfigurationService.class ).getSetting(
				AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY,
				CustomEntityDirtinessStrategy.class,
				defaultValue
		);
	}

	@SuppressWarnings({ "unchecked" })
	private CurrentTenantIdentifierResolver determineCurrentTenantIdentifierResolver(
			CurrentTenantIdentifierResolver explicitResolver) {
		if ( explicitResolver != null ) {
			return explicitResolver;
		}
		return serviceRegistry.getService( ConfigurationService.class )
				.getSetting(
						AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
						CurrentTenantIdentifierResolver.class,
						null
				);

	}

	@SuppressWarnings( {"ThrowableResultOfMethodCallIgnored"})
	public SessionFactoryImpl(
			MetadataImplementor metadata,
			SessionFactoryOptions sessionFactoryOptions,
			SessionFactoryObserver observer) throws HibernateException {
		LOG.debug( "Building session factory" );

		this.sessionFactoryOptions = sessionFactoryOptions;

		this.properties = createPropertiesFromMap(
				metadata.getServiceRegistry().getService( ConfigurationService.class ).getSettings()
		);

		// TODO: these should be moved into SessionFactoryOptions
		this.settings = new SettingsFactory().buildSettings(
				properties,
				metadata.getServiceRegistry()
		);

		this.serviceRegistry =
				metadata.getServiceRegistry()
						.getService( SessionFactoryServiceRegistryFactory.class )
						.buildServiceRegistry( this, metadata );

		this.jdbcServices = this.serviceRegistry.getService( JdbcServices.class );
		this.dialect = this.jdbcServices.getDialect();
		this.cacheAccess = this.serviceRegistry.getService( CacheImplementor.class );

		// TODO: get SQL functions from JdbcServices (HHH-6559)
		//this.sqlFunctionRegistry = new SQLFunctionRegistry( this.jdbcServices.getSqlFunctions() );
		this.sqlFunctionRegistry = new SQLFunctionRegistry( this.dialect, new HashMap<String, SQLFunction>() );

		// TODO: get SQL functions from a new service
		// this.sqlFunctionRegistry = new SQLFunctionRegistry( getDialect(), cfg.getSqlFunctions() );

		if ( observer != null ) {
			this.observer.addObserver( observer );
		}

		this.typeResolver = metadata.getTypeResolver().scope( this );
		this.typeHelper = new TypeLocatorImpl( typeResolver );

		this.filters = new HashMap<String, FilterDefinition>();
		for ( FilterDefinition filterDefinition : metadata.getFilterDefinitions() ) {
			filters.put( filterDefinition.getFilterName(), filterDefinition );
		}

		LOG.debugf( "Session factory constructed with filter configurations : %s", filters );
		LOG.debugf( "Instantiating session factory with properties: %s", properties );

		this.queryPlanCache = new QueryPlanCache( this );

		class IntegratorObserver implements SessionFactoryObserver {
			private ArrayList<Integrator> integrators = new ArrayList<Integrator>();

			@Override
			public void sessionFactoryCreated(SessionFactory factory) {
			}

			@Override
			public void sessionFactoryClosed(SessionFactory factory) {
				for ( Integrator integrator : integrators ) {
					integrator.disintegrate( SessionFactoryImpl.this, SessionFactoryImpl.this.serviceRegistry );
				}
			}
		}

        final IntegratorObserver integratorObserver = new IntegratorObserver();
        this.observer.addObserver(integratorObserver);
        for (Integrator integrator : serviceRegistry.getService(IntegratorService.class).getIntegrators()) {
            integrator.integrate(metadata, this, this.serviceRegistry);
            integratorObserver.integrators.add(integrator);
        }


		//Generators:

		identifierGenerators = new HashMap<String,IdentifierGenerator>();
		for ( EntityBinding entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding.isRoot() ) {
				identifierGenerators.put(
						entityBinding.getEntity().getName(),
						entityBinding.getHierarchyDetails().getEntityIdentifier().getIdentifierGenerator()
				);
			}
		}

		///////////////////////////////////////////////////////////////////////
		// Prepare persisters and link them up with their cache
		// region/access-strategy

		StringBuilder stringBuilder = new StringBuilder();
		if ( settings.getCacheRegionPrefix() != null) {
			stringBuilder
					.append( settings.getCacheRegionPrefix() )
					.append( '.' );
		}
		final String cacheRegionPrefix = stringBuilder.toString();

		entityPersisters = new HashMap<String,EntityPersister>();
		Map<String, RegionAccessStrategy> entityAccessStrategies = new HashMap<String, RegionAccessStrategy>();
		Map<String,ClassMetadata> classMeta = new HashMap<String,ClassMetadata>();
		for ( EntityBinding model : metadata.getEntityBindings() ) {
			// TODO: should temp table prep happen when metadata is being built?
			//model.prepareTemporaryTables( metadata, getDialect() );
			// cache region is defined by the root-class in the hierarchy...
			EntityBinding rootEntityBinding = metadata.getRootEntityBinding( model.getEntity().getName() );
			EntityRegionAccessStrategy accessStrategy = null;
			if ( settings.isSecondLevelCacheEnabled() &&
					rootEntityBinding.getHierarchyDetails().getCaching() != null &&
					model.getHierarchyDetails().getCaching() != null &&
					model.getHierarchyDetails().getCaching().getAccessType() != null ) {
				final String cacheRegionName = cacheRegionPrefix + rootEntityBinding.getHierarchyDetails().getCaching().getRegion();
				accessStrategy = EntityRegionAccessStrategy.class.cast( entityAccessStrategies.get( cacheRegionName ) );
				if ( accessStrategy == null ) {
					final AccessType accessType = model.getHierarchyDetails().getCaching().getAccessType();
					if ( LOG.isTraceEnabled() ) {
						LOG.tracev( "Building cache for entity data [{0}]", model.getEntity().getName() );
					}
					EntityRegion entityRegion = settings.getRegionFactory().buildEntityRegion(
							cacheRegionName, properties, CacheDataDescriptionImpl.decode( model )
					);
					accessStrategy = entityRegion.buildAccessStrategy( accessType );
					entityAccessStrategies.put( cacheRegionName, accessStrategy );
					cacheAccess.addCacheRegion( cacheRegionName, entityRegion );
				}
			}
			EntityPersister cp = serviceRegistry.getService( PersisterFactory.class ).createEntityPersister(
					model, accessStrategy, this, metadata
			);
			entityPersisters.put( model.getEntity().getName(), cp );
			classMeta.put( model.getEntity().getName(), cp.getClassMetadata() );
		}
		this.classMetadata = Collections.unmodifiableMap(classMeta);

		Map<String,Set<String>> tmpEntityToCollectionRoleMap = new HashMap<String,Set<String>>();
		collectionPersisters = new HashMap<String,CollectionPersister>();
		Map<String, CollectionMetadata> tmpCollectionMetadata = new HashMap<String, CollectionMetadata>();
		for ( PluralAttributeBinding model : metadata.getCollectionBindings() ) {
			if ( model.getAttribute() == null ) {
				throw new IllegalStateException( "No attribute defined for a AbstractPluralAttributeBinding: " +  model );
			}
			if ( model.getAttribute().isSingular() ) {
				throw new IllegalStateException(
						"AbstractPluralAttributeBinding has a Singular attribute defined: " + model.getAttribute().getName()
				);
			}
			final String cacheRegionName = cacheRegionPrefix + model.getCaching().getRegion();
			final AccessType accessType = model.getCaching().getAccessType();
			CollectionRegionAccessStrategy accessStrategy = null;
			if ( accessType != null && settings.isSecondLevelCacheEnabled() ) {
				if ( LOG.isTraceEnabled() ) {
					LOG.tracev( "Building cache for collection data [{0}]", model.getAttribute().getRole() );
				}
				CollectionRegion collectionRegion = settings.getRegionFactory().buildCollectionRegion(
						cacheRegionName, properties, CacheDataDescriptionImpl.decode( model )
				);
				accessStrategy = collectionRegion.buildAccessStrategy( accessType );
				entityAccessStrategies.put( cacheRegionName, accessStrategy );
				cacheAccess.addCacheRegion(  cacheRegionName, collectionRegion );
			}
			CollectionPersister persister = serviceRegistry
					.getService( PersisterFactory.class )
					.createCollectionPersister( metadata, model, accessStrategy, this );
			collectionPersisters.put( model.getAttribute().getRole(), persister );
			tmpCollectionMetadata.put( model.getAttribute().getRole(), persister.getCollectionMetadata() );
			Type indexType = persister.getIndexType();
			if ( indexType != null && indexType.isAssociationType() && !indexType.isAnyType() ) {
				String entityName = ( ( AssociationType ) indexType ).getAssociatedEntityName( this );
				Set<String> roles = tmpEntityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<String>();
					tmpEntityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType.isAssociationType() && !elementType.isAnyType() ) {
				String entityName = ( ( AssociationType ) elementType ).getAssociatedEntityName( this );
				Set<String> roles = tmpEntityToCollectionRoleMap.get( entityName );
				if ( roles == null ) {
					roles = new HashSet<String>();
					tmpEntityToCollectionRoleMap.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}
		collectionMetadata = Collections.unmodifiableMap( tmpCollectionMetadata );
		for ( Map.Entry<String, Set<String>> entry : tmpEntityToCollectionRoleMap.entrySet() ) {
			entry.setValue( Collections.unmodifiableSet( entry.getValue() ) );
		}
		collectionRolesByEntityParticipant = Collections.unmodifiableMap( tmpEntityToCollectionRoleMap );

		//Named Queries:
		namedQueries = new HashMap<String,NamedQueryDefinition>();
		for ( NamedQueryDefinition namedQueryDefinition :  metadata.getNamedQueryDefinitions() ) {
			namedQueries.put( namedQueryDefinition.getName(), namedQueryDefinition );
		}
		namedSqlQueries = new HashMap<String, NamedSQLQueryDefinition>();
		for ( NamedSQLQueryDefinition namedNativeQueryDefinition: metadata.getNamedNativeQueryDefinitions() ) {
			namedSqlQueries.put( namedNativeQueryDefinition.getName(), namedNativeQueryDefinition );
		}
		sqlResultSetMappings = new HashMap<String, ResultSetMappingDefinition>();
		for( ResultSetMappingDefinition resultSetMappingDefinition : metadata.getResultSetMappingDefinitions() ) {
			sqlResultSetMappings.put( resultSetMappingDefinition.getName(), resultSetMappingDefinition );
		}
		imports = new HashMap<String,String>();
		for ( Map.Entry<String,String> importEntry : metadata.getImports() ) {
			imports.put( importEntry.getKey(), importEntry.getValue() );
		}

		// after *all* persisters and named queries are registered
		Iterator iter = entityPersisters.values().iterator();
		while ( iter.hasNext() ) {
			final EntityPersister persister = ( ( EntityPersister ) iter.next() );
			persister.postInstantiate();
			registerEntityNameResolvers( persister );

		}
		iter = collectionPersisters.values().iterator();
		while ( iter.hasNext() ) {
			final CollectionPersister persister = ( ( CollectionPersister ) iter.next() );
			persister.postInstantiate();
		}

		//JNDI + Serialization:

		name = settings.getSessionFactoryName();
		try {
			uuid = (String) UUID_GENERATOR.generate(null, null);
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not generate UUID");
		}
		SessionFactoryRegistry.INSTANCE.addSessionFactory(
				uuid, 
				name,
				settings.isSessionFactoryNameAlsoJndiName(),
				this,
				serviceRegistry.getService( JndiService.class )
		);

		LOG.debug("Instantiated session factory");

		if ( settings.isAutoCreateSchema() ) {
			new SchemaExport( metadata )
					.setImportSqlCommandExtractor( serviceRegistry.getService( ImportSqlCommandExtractor.class ) )
					.create( false, true );
		}

		if ( settings.isAutoDropSchema() ) {
			schemaExport = new SchemaExport( metadata )
					.setImportSqlCommandExtractor( serviceRegistry.getService( ImportSqlCommandExtractor.class ) );
		}

		currentSessionContext = buildCurrentSessionContext();

		//checking for named queries
		if ( settings.isNamedQueryStartupCheckingEnabled() ) {
			final Map<String,HibernateException> errors = checkNamedQueries();
			if ( ! errors.isEmpty() ) {
				StringBuilder failingQueries = new StringBuilder( "Errors in named queries: " );
				String sep = "";
				for ( Map.Entry<String,HibernateException> entry : errors.entrySet() ) {
					LOG.namedQueryError( entry.getKey(), entry.getValue() );
					failingQueries.append( entry.getKey() ).append( sep );
					sep = ", ";
				}
				throw new HibernateException( failingQueries.toString() );
			}
		}

		// this needs to happen after persisters are all ready to go...
		this.fetchProfiles = new HashMap<String,FetchProfile>();
		for ( org.hibernate.metamodel.binding.FetchProfile mappingProfile : metadata.getFetchProfiles() ) {
			final FetchProfile fetchProfile = new FetchProfile( mappingProfile.getName() );
			for ( org.hibernate.metamodel.binding.FetchProfile.Fetch mappingFetch : mappingProfile.getFetches() ) {
				// resolve the persister owning the fetch
				final String entityName = getImportedClassName( mappingFetch.getEntity() );
				final EntityPersister owner = entityName == null ? null : entityPersisters.get( entityName );
				if ( owner == null ) {
					throw new HibernateException(
							"Unable to resolve entity reference [" + mappingFetch.getEntity()
									+ "] in fetch profile [" + fetchProfile.getName() + "]"
					);
				}

				// validate the specified association fetch
				Type associationType = owner.getPropertyType( mappingFetch.getAssociation() );
				if ( associationType == null || ! associationType.isAssociationType() ) {
					throw new HibernateException( "Fetch profile [" + fetchProfile.getName() + "] specified an invalid association" );
				}

				// resolve the style
				final Fetch.Style fetchStyle = Fetch.Style.parse( mappingFetch.getStyle() );

				// then construct the fetch instance...
				fetchProfile.addFetch( new Association( owner, mappingFetch.getAssociation() ), fetchStyle );
				( ( Loadable ) owner ).registerAffectingFetchProfile( fetchProfile.getName() );
			}
			fetchProfiles.put( fetchProfile.getName(), fetchProfile );
		}

		this.customEntityDirtinessStrategy = determineCustomEntityDirtinessStrategy();
		this.currentTenantIdentifierResolver = determineCurrentTenantIdentifierResolver( null );
		this.transactionEnvironment = new TransactionEnvironmentImpl( this );
		this.observer.sessionFactoryCreated( this );
	}

	@SuppressWarnings( {"unchecked"} )
	private static Properties createPropertiesFromMap(Map map) {
		Properties properties = new Properties();
		properties.putAll( map );
		return properties;
	}

	public Session openSession() throws HibernateException {
		return withOptions().openSession();
	}

	public Session openTemporarySession() throws HibernateException {
		return withOptions()
				.autoClose( false )
				.flushBeforeCompletion( false )
				.connectionReleaseMode( ConnectionReleaseMode.AFTER_STATEMENT )
				.openSession();
	}

	public Session getCurrentSession() throws HibernateException {
		if ( currentSessionContext == null ) {
			throw new HibernateException( "No CurrentSessionContext configured!" );
		}
		return currentSessionContext.currentSession();
	}

	@Override
	public SessionBuilderImplementor withOptions() {
		return new SessionBuilderImpl( this );
	}

	@Override
	public StatelessSessionBuilder withStatelessOptions() {
		return new StatelessSessionBuilderImpl( this );
	}

	public StatelessSession openStatelessSession() {
		return withStatelessOptions().openStatelessSession();
	}

	public StatelessSession openStatelessSession(Connection connection) {
		return withStatelessOptions().connection( connection ).openStatelessSession();
	}

	@Override
	public void addObserver(SessionFactoryObserver observer) {
		this.observer.addObserver( observer );
	}

	public TransactionEnvironment getTransactionEnvironment() {
		return transactionEnvironment;
	}

	public Properties getProperties() {
		return properties;
	}

	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return null;
	}

	public TypeResolver getTypeResolver() {
		return typeResolver;
	}

	private void registerEntityNameResolvers(EntityPersister persister) {
		if ( persister.getEntityMetamodel() == null || persister.getEntityMetamodel().getTuplizer() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getEntityMetamodel().getTuplizer() );
	}

	private void registerEntityNameResolvers(EntityTuplizer tuplizer) {
		EntityNameResolver[] resolvers = tuplizer.getEntityNameResolvers();
		if ( resolvers == null ) {
			return;
		}

		for ( EntityNameResolver resolver : resolvers ) {
			registerEntityNameResolver( resolver );
		}
	}

	private static final Object ENTITY_NAME_RESOLVER_MAP_VALUE = new Object();

	public void registerEntityNameResolver(EntityNameResolver resolver) {
		entityNameResolvers.put( resolver, ENTITY_NAME_RESOLVER_MAP_VALUE );
	}

	public Iterable<EntityNameResolver> iterateEntityNameResolvers() {
		return entityNameResolvers.keySet();
	}

	public QueryPlanCache getQueryPlanCache() {
		return queryPlanCache;
	}

	@SuppressWarnings( {"ThrowableResultOfMethodCallIgnored"})
	private Map<String,HibernateException> checkNamedQueries() throws HibernateException {
		Map<String,HibernateException> errors = new HashMap<String,HibernateException>();

		// Check named HQL queries
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Checking %s named HQL queries", namedQueries.size() );
		}
		Iterator itr = namedQueries.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final String queryName = ( String ) entry.getKey();
			final NamedQueryDefinition qd = ( NamedQueryDefinition ) entry.getValue();
			// this will throw an error if there's something wrong.
			try {
				LOG.debugf( "Checking named query: %s", queryName );
				//TODO: BUG! this currently fails for named queries for non-POJO entities
				queryPlanCache.getHQLQueryPlan( qd.getQueryString(), false, Collections.EMPTY_MAP );
			}
			catch ( QueryException e ) {
				errors.put( queryName, e );
			}
			catch ( MappingException e ) {
				errors.put( queryName, e );
			}


		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Checking %s named SQL queries", namedSqlQueries.size() );
		}
		itr = namedSqlQueries.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final String queryName = ( String ) entry.getKey();
			final NamedSQLQueryDefinition qd = ( NamedSQLQueryDefinition ) entry.getValue();
			// this will throw an error if there's something wrong.
			try {
				LOG.debugf( "Checking named SQL query: %s", queryName );
				// TODO : would be really nice to cache the spec on the query-def so as to not have to re-calc the hash;
				// currently not doable though because of the resultset-ref stuff...
				NativeSQLQuerySpecification spec;
				if ( qd.getResultSetRef() != null ) {
					ResultSetMappingDefinition definition = sqlResultSetMappings.get( qd.getResultSetRef() );
					if ( definition == null ) {
						throw new MappingException( "Unable to find resultset-ref definition: " + qd.getResultSetRef() );
					}
					spec = new NativeSQLQuerySpecification(
							qd.getQueryString(),
					        definition.getQueryReturns(),
					        qd.getQuerySpaces()
					);
				}
				else {
					spec =  new NativeSQLQuerySpecification(
							qd.getQueryString(),
					        qd.getQueryReturns(),
					        qd.getQuerySpaces()
					);
				}
				queryPlanCache.getNativeSQLQueryPlan( spec );
			}
			catch ( QueryException e ) {
				errors.put( queryName, e );
			}
			catch ( MappingException e ) {
				errors.put( queryName, e );
			}

		}

		return errors;
	}

	public EntityPersister getEntityPersister(String entityName) throws MappingException {
		EntityPersister result = entityPersisters.get(entityName);
		if ( result == null ) {
			throw new MappingException( "Unknown entity: " + entityName );
		}
		return result;
	}

	@Override
	public Map<String, CollectionPersister> getCollectionPersisters() {
		return collectionPersisters;
	}

	@Override
	public Map<String, EntityPersister> getEntityPersisters() {
		return entityPersisters;
	}

	public CollectionPersister getCollectionPersister(String role) throws MappingException {
		CollectionPersister result = collectionPersisters.get(role);
		if ( result == null ) {
			throw new MappingException( "Unknown collection role: " + role );
		}
		return result;
	}

	public Settings getSettings() {
		return settings;
	}

	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return sessionFactoryOptions;
	}

	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	public Dialect getDialect() {
		if ( serviceRegistry == null ) {
			throw new IllegalStateException( "Cannot determine dialect because serviceRegistry is null." );
		}
		return dialect;
	}

	public Interceptor getInterceptor() {
		return sessionFactoryOptions.getInterceptor();
	}

	public SQLExceptionConverter getSQLExceptionConverter() {
		return getSQLExceptionHelper().getSqlExceptionConverter();
	}

	public SqlExceptionHelper getSQLExceptionHelper() {
		return getJdbcServices().getSqlExceptionHelper();
	}

	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}

	@Override
	public Reference getReference() {
		// from javax.naming.Referenceable
        LOG.debug( "Returning a Reference to the SessionFactory" );
		return new Reference(
				SessionFactoryImpl.class.getName(),
				new StringRefAddr("uuid", uuid),
				SessionFactoryRegistry.ObjectFactoryImpl.class.getName(),
				null
		);
	}

	public NamedQueryDefinition getNamedQuery(String queryName) {
		return namedQueries.get( queryName );
	}

	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
		return namedSqlQueries.get( queryName );
	}

	public ResultSetMappingDefinition getResultSetMapping(String resultSetName) {
		return sqlResultSetMappings.get( resultSetName );
	}

	public Type getIdentifierType(String className) throws MappingException {
		return getEntityPersister(className).getIdentifierType();
	}
	public String getIdentifierPropertyName(String className) throws MappingException {
		return getEntityPersister(className).getIdentifierPropertyName();
	}

	public Type[] getReturnTypes(String queryString) throws HibernateException {
		return queryPlanCache.getHQLQueryPlan( queryString, false, Collections.EMPTY_MAP )
				.getReturnMetadata()
				.getReturnTypes();
	}

	public String[] getReturnAliases(String queryString) throws HibernateException {
		return queryPlanCache.getHQLQueryPlan( queryString, false, Collections.EMPTY_MAP )
				.getReturnMetadata()
				.getReturnAliases();
	}

	public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
		return getClassMetadata( persistentClass.getName() );
	}

	public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
		return collectionMetadata.get(roleName);
	}

	public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
		return classMetadata.get( entityName );
	}

	/**
	 * Given the name of an entity class, determine all the class and interface names by which it can be
	 * referenced in an HQL query.
	 *
     * @param className The name of the entity class
	 *
	 * @return the names of all persistent (mapped) classes that extend or implement the
	 *     given class or interface, accounting for implicit/explicit polymorphism settings
	 *     and excluding mapped subclasses/joined-subclasses of other classes in the result.
	 * @throws MappingException
	 */
	public String[] getImplementors(String className) throws MappingException {

		final Class clazz;
		try {
			clazz = serviceRegistry.getService( ClassLoaderService.class ).classForName( className );
		}
		catch (ClassLoadingException cnfe) {
			return new String[] { className }; //for a dynamic-class
		}

		ArrayList<String> results = new ArrayList<String>();
		for ( EntityPersister checkPersister : entityPersisters.values() ) {
			if ( ! Queryable.class.isInstance( checkPersister ) ) {
				continue;
			}
			final Queryable checkQueryable = Queryable.class.cast( checkPersister );
			final String checkQueryableEntityName = checkQueryable.getEntityName();
			final boolean isMappedClass = className.equals( checkQueryableEntityName );
			if ( checkQueryable.isExplicitPolymorphism() ) {
				if ( isMappedClass ) {
					return new String[] { className }; //NOTE EARLY EXIT
				}
			}
			else {
				if ( isMappedClass ) {
					results.add( checkQueryableEntityName );
				}
				else {
					final Class mappedClass = checkQueryable.getMappedClass();
					if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
						final boolean assignableSuperclass;
						if ( checkQueryable.isInherited() ) {
							Class mappedSuperclass = getEntityPersister( checkQueryable.getMappedSuperclass() ).getMappedClass();
							assignableSuperclass = clazz.isAssignableFrom( mappedSuperclass );
						}
						else {
							assignableSuperclass = false;
						}
						if ( !assignableSuperclass ) {
							results.add( checkQueryableEntityName );
						}
					}
				}
			}
		}
		return results.toArray( new String[results.size()] );
	}

	public String getImportedClassName(String className) {
		String result = imports.get(className);
		if (result==null) {
			try {
				serviceRegistry.getService( ClassLoaderService.class ).classForName( className );
				return className;
			}
			catch (ClassLoadingException cnfe) {
				return null;
			}
		}
		else {
			return result;
		}
	}

	public Map<String,ClassMetadata> getAllClassMetadata() throws HibernateException {
		return classMetadata;
	}

	public Map getAllCollectionMetadata() throws HibernateException {
		return collectionMetadata;
	}

	public Type getReferencedPropertyType(String className, String propertyName)
		throws MappingException {
		return getEntityPersister( className ).getPropertyType( propertyName );
	}

	public ConnectionProvider getConnectionProvider() {
		return jdbcServices.getConnectionProvider();
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
	 * @throws HibernateException
	 */
	public void close() throws HibernateException {

		if ( isClosed ) {
			LOG.trace( "Already closed" );
			return;
		}

		LOG.closing();

		isClosed = true;

		settings.getMultiTableBulkIdStrategy().release( jdbcServices, buildLocalConnectionAccess() );

		Iterator iter = entityPersisters.values().iterator();
		while ( iter.hasNext() ) {
			EntityPersister p = (EntityPersister) iter.next();
			if ( p.hasCache() ) {
				p.getCacheAccessStrategy().getRegion().destroy();
			}
		}

		iter = collectionPersisters.values().iterator();
		while ( iter.hasNext() ) {
			CollectionPersister p = (CollectionPersister) iter.next();
			if ( p.hasCache() ) {
				p.getCacheAccessStrategy().getRegion().destroy();
			}
		}

		cacheAccess.close();

		queryPlanCache.cleanup();

		if ( settings.isAutoDropSchema() ) {
			schemaExport.drop( false, true );
		}

		SessionFactoryRegistry.INSTANCE.removeSessionFactory(
				uuid,
				name,
				settings.isSessionFactoryNameAlsoJndiName(),
				serviceRegistry.getService( JndiService.class )
		);

		observer.sessionFactoryClosed( this );
		serviceRegistry.destroy();
	}

	public Cache getCache() {
		return cacheAccess;
	}

	public void evictEntity(String entityName, Serializable id) throws HibernateException {
		getCache().evictEntity( entityName, id );
	}

	public void evictEntity(String entityName) throws HibernateException {
		getCache().evictEntityRegion( entityName );
	}

	public void evict(Class persistentClass, Serializable id) throws HibernateException {
		getCache().evictEntity( persistentClass, id );
	}

	public void evict(Class persistentClass) throws HibernateException {
		getCache().evictEntityRegion( persistentClass );
	}

	public void evictCollection(String roleName, Serializable id) throws HibernateException {
		getCache().evictCollection( roleName, id );
	}

	public void evictCollection(String roleName) throws HibernateException {
		getCache().evictCollectionRegion( roleName );
	}

	public void evictQueries() throws HibernateException {
		cacheAccess.evictQueries();
	}

	public void evictQueries(String regionName) throws HibernateException {
		getCache().evictQueryRegion( regionName );
	}

	public UpdateTimestampsCache getUpdateTimestampsCache() {
		return cacheAccess.getUpdateTimestampsCache();
	}

	public QueryCache getQueryCache() {
		return cacheAccess.getQueryCache();
	}

	public QueryCache getQueryCache(String regionName) throws HibernateException {
		return cacheAccess.getQueryCache( regionName );
	}

	public Region getSecondLevelCacheRegion(String regionName) {
		return cacheAccess.getSecondLevelCacheRegion( regionName );
	}

	public Region getNaturalIdCacheRegion(String regionName) {
		return cacheAccess.getNaturalIdCacheRegion( regionName );
	}

	@SuppressWarnings( {"unchecked"})
	public Map getAllSecondLevelCacheRegions() {
		return cacheAccess.getAllSecondLevelCacheRegions();
	}

	public boolean isClosed() {
		return isClosed;
	}

	public Statistics getStatistics() {
		return getStatisticsImplementor();
	}

	public StatisticsImplementor getStatisticsImplementor() {
		return serviceRegistry.getService( StatisticsImplementor.class );
	}

	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
		FilterDefinition def = filters.get( filterName );
		if ( def == null ) {
			throw new HibernateException( "No such filter configured [" + filterName + "]" );
		}
		return def;
	}

	public boolean containsFetchProfileDefinition(String name) {
		return fetchProfiles.containsKey( name );
	}

	public Set getDefinedFilterNames() {
		return filters.keySet();
	}

	public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
		return identifierGenerators.get(rootEntityName);
	}

	private org.hibernate.engine.transaction.spi.TransactionFactory transactionFactory() {
		return serviceRegistry.getService( org.hibernate.engine.transaction.spi.TransactionFactory.class );
	}

	private boolean canAccessTransactionManager() {
		try {
			return serviceRegistry.getService( JtaPlatform.class ).retrieveTransactionManager() != null;
		}
		catch (Exception e) {
			return false;
		}
	}

	private CurrentSessionContext buildCurrentSessionContext() {
		String impl = properties.getProperty( Environment.CURRENT_SESSION_CONTEXT_CLASS );
		// for backward-compatibility
		if ( impl == null ) {
			if ( canAccessTransactionManager() ) {
				impl = "jta";
			}
			else {
				return null;
			}
		}

		if ( "jta".equals( impl ) ) {
			if ( ! transactionFactory().compatibleWithJtaSynchronization() ) {
				LOG.autoFlushWillNotWork();
			}
			return new JTASessionContext( this );
		}
		else if ( "thread".equals( impl ) ) {
			return new ThreadLocalSessionContext( this );
		}
		else if ( "managed".equals( impl ) ) {
			return new ManagedSessionContext( this );
		}
		else {
			try {
				Class implClass = serviceRegistry.getService( ClassLoaderService.class ).classForName( impl );
				return ( CurrentSessionContext ) implClass
						.getConstructor( new Class[] { SessionFactoryImplementor.class } )
						.newInstance( this );
			}
			catch( Throwable t ) {
				LOG.unableToConstructCurrentSessionContext( impl, t );
				return null;
			}
		}
	}

	@Override
	public ServiceRegistryImplementor getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public EntityNotFoundDelegate getEntityNotFoundDelegate() {
		return sessionFactoryOptions.getEntityNotFoundDelegate();
	}

	public SQLFunctionRegistry getSqlFunctionRegistry() {
		return sqlFunctionRegistry;
	}

	public FetchProfile getFetchProfile(String name) {
		return fetchProfiles.get( name );
	}

	public TypeHelper getTypeHelper() {
		return typeHelper;
	}

	static class SessionBuilderImpl implements SessionBuilderImplementor {
		private final SessionFactoryImpl sessionFactory;
		private SessionOwner sessionOwner;
		private Interceptor interceptor;
		private Connection connection;
		private ConnectionReleaseMode connectionReleaseMode;
		private boolean autoClose;
		private boolean autoJoinTransactions = true;
		private boolean flushBeforeCompletion;
		private String tenantIdentifier;

		SessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;
			this.sessionOwner = null;
			final Settings settings = sessionFactory.settings;

			// set up default builder values...
			this.interceptor = sessionFactory.getInterceptor();
			this.connectionReleaseMode = settings.getConnectionReleaseMode();
			this.autoClose = settings.isAutoCloseSessionEnabled();
			this.flushBeforeCompletion = settings.isFlushBeforeCompletionEnabled();

			if ( sessionFactory.getCurrentTenantIdentifierResolver() != null ) {
				tenantIdentifier = sessionFactory.getCurrentTenantIdentifierResolver().resolveCurrentTenantIdentifier();
			}
		}

		protected TransactionCoordinatorImpl getTransactionCoordinator() {
			return null;
		}

		@Override
		public Session openSession() {
			return new SessionImpl(
					connection,
					sessionFactory,
					sessionOwner,
					getTransactionCoordinator(),
					autoJoinTransactions,
					sessionFactory.settings.getRegionFactory().nextTimestamp(),
					interceptor,
					flushBeforeCompletion,
					autoClose,
					connectionReleaseMode,
					tenantIdentifier
			);
		}

		@Override
		public SessionBuilder owner(SessionOwner sessionOwner) {
			this.sessionOwner = sessionOwner;
			return this;
		}

		@Override
		public SessionBuilder interceptor(Interceptor interceptor) {
			this.interceptor = interceptor;
			return this;
		}

		@Override
		public SessionBuilder noInterceptor() {
			this.interceptor = EmptyInterceptor.INSTANCE;
			return this;
		}

		@Override
		public SessionBuilder connection(Connection connection) {
			this.connection = connection;
			return this;
		}

		@Override
		public SessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
			this.connectionReleaseMode = connectionReleaseMode;
			return this;
		}

		@Override
		public SessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
			this.autoJoinTransactions = autoJoinTransactions;
			return this;
		}

		@Override
		public SessionBuilder autoClose(boolean autoClose) {
			this.autoClose = autoClose;
			return this;
		}

		@Override
		public SessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
			this.flushBeforeCompletion = flushBeforeCompletion;
			return this;
		}

		@Override
		public SessionBuilder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}
	}

	public static class StatelessSessionBuilderImpl implements StatelessSessionBuilder {
		private final SessionFactoryImpl sessionFactory;
		private Connection connection;
		private String tenantIdentifier;

		public StatelessSessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		@Override
		public StatelessSession openStatelessSession() {
			return new StatelessSessionImpl( connection, tenantIdentifier, sessionFactory );
		}

		@Override
		public StatelessSessionBuilder connection(Connection connection) {
			this.connection = connection;
			return this;
		}

		@Override
		public StatelessSessionBuilder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return this;
		}
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return customEntityDirtinessStrategy;
	}

	@Override
	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return currentTenantIdentifierResolver;
	}


	// Serialization handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Custom serialization hook defined by Java spec.  Used when the factory is directly serialized
	 *
	 * @param out The stream into which the object is being serialized.
	 *
	 * @throws IOException Can be thrown by the stream
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		LOG.debugf( "Serializing: %s", uuid );
		out.defaultWriteObject();
		LOG.trace( "Serialized" );
	}

	/**
	 * Custom serialization hook defined by Java spec.  Used when the factory is directly deserialized
	 *
	 * @param in The stream from which the object is being deserialized.
	 *
	 * @throws IOException Can be thrown by the stream
	 * @throws ClassNotFoundException Again, can be thrown by the stream
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		LOG.trace( "Deserializing" );
		in.defaultReadObject();
		LOG.debugf( "Deserialized: %s", uuid );
	}

	/**
	 * Custom serialization hook defined by Java spec.  Used when the factory is directly deserialized.
	 * Here we resolve the uuid/name read from the stream previously to resolve the SessionFactory
	 * instance to use based on the registrations with the {@link SessionFactoryRegistry}
	 *
	 * @return The resolved factory to use.
	 *
	 * @throws InvalidObjectException Thrown if we could not resolve the factory by uuid/name.
	 */
	private Object readResolve() throws InvalidObjectException {
		LOG.trace( "Resolving serialized SessionFactory" );
		return locateSessionFactoryOnDeserialization( uuid, name );
	}

	private static SessionFactory locateSessionFactoryOnDeserialization(String uuid, String name) throws InvalidObjectException{
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
	 * Custom serialization hook used during Session serialization.
	 *
	 * @param oos The stream to which to write the factory
	 * @throws IOException Indicates problems writing out the serial data stream
	 */
	void serialize(ObjectOutputStream oos) throws IOException {
		oos.writeUTF( uuid );
		oos.writeBoolean( name != null );
		if ( name != null ) {
			oos.writeUTF( name );
		}
	}

	/**
	 * Custom deserialization hook used during Session deserialization.
	 *
	 * @param ois The stream from which to "read" the factory
	 * @return The deserialized factory
	 * @throws IOException indicates problems reading back serial data stream
	 * @throws ClassNotFoundException indicates problems reading back serial data stream
	 */
	static SessionFactoryImpl deserialize(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		LOG.trace( "Deserializing SessionFactory from Session" );
		final String uuid = ois.readUTF();
		boolean isNamed = ois.readBoolean();
		final String name = isNamed ? ois.readUTF() : null;
		return (SessionFactoryImpl) locateSessionFactoryOnDeserialization( uuid, name );
	}
}
