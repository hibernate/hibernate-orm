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
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EmptyInterceptor;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.context.internal.JTASessionContext;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.context.internal.ThreadLocalSessionContext;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.profile.Association;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.query.spi.ReturnMetadata;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.internal.AfterCompletionActionLegacyJpaImpl;
import org.hibernate.jpa.internal.ExceptionMapperLegacyJpaImpl;
import org.hibernate.jpa.internal.ManagedFlushCheckerLegacyJpaImpl;
import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;
import org.hibernate.mapping.RootClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.metamodel.internal.MetamodelImpl;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.AfterCompletionAction;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ManagedFlushChecker;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.secure.spi.GrantedPermission;
import org.hibernate.secure.spi.JaccPermissionDeclarations;
import org.hibernate.secure.spi.JaccService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;

import org.jboss.logging.Logger;

import static org.hibernate.metamodel.internal.JpaMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

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
 * @author Gavin King
 * @author Steve Ebersole
 * @author Chris Cranford
 */
public class SessionFactoryImpl implements SessionFactoryImplementor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SessionFactoryImpl.class );

	private final String name;
	private final String uuid;

	private transient volatile boolean isClosed;

	private final transient SessionFactoryObserverChain observer = new SessionFactoryObserverChain();

	private final transient SessionFactoryOptions sessionFactoryOptions;
	private final transient Settings settings;
	private final transient Map<String,Object> properties;

	private final transient SessionFactoryServiceRegistry serviceRegistry;
	private final transient JdbcServices jdbcServices;

	private final transient SQLFunctionRegistry sqlFunctionRegistry;

	// todo : org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor too?

	private final transient MetamodelImplementor metamodel;
	private final transient CriteriaBuilderImpl criteriaBuilder;
	private final PersistenceUnitUtil jpaPersistenceUnitUtil;
	private final transient CacheImplementor cacheAccess;
	private final transient NamedQueryRepository namedQueryRepository;
	private final transient QueryPlanCache queryPlanCache;

	private final transient CurrentSessionContext currentSessionContext;

	private volatile DelayedDropAction delayedDropAction;

	// todo : move to MetamodelImpl
	private final transient Map<String,IdentifierGenerator> identifierGenerators;
	private final transient Map<String, FilterDefinition> filters;
	private final transient Map<String, FetchProfile> fetchProfiles;

	private final transient TypeHelper typeHelper;

	private final transient FastSessionServices fastSessionServices;
	private final transient SessionBuilder defaultSessionOpenOptions;
	private final transient SessionBuilder temporarySessionOpenOptions;

	public SessionFactoryImpl(
			final MetadataImplementor metadata,
			SessionFactoryOptions options,
			QueryPlanCache.QueryPlanCreator queryPlanCacheFunction) {
		LOG.debug( "Building session factory" );

		this.sessionFactoryOptions = options;
		this.settings = new Settings( options, metadata );

		this.serviceRegistry = options
				.getServiceRegistry()
				.getService( SessionFactoryServiceRegistryFactory.class )
				.buildServiceRegistry( this, options );

		metadata.initSessionFactory( this );

		final CfgXmlAccessService cfgXmlAccessService = serviceRegistry.getService( CfgXmlAccessService.class );

		String sfName = settings.getSessionFactoryName();
		if ( cfgXmlAccessService.getAggregatedConfig() != null ) {
			if ( sfName == null ) {
				sfName = cfgXmlAccessService.getAggregatedConfig().getSessionFactoryName();
			}
			applyCfgXmlValues( cfgXmlAccessService.getAggregatedConfig(), serviceRegistry );
		}

		this.name = sfName;
		this.uuid = options.getUuid();

		jdbcServices = serviceRegistry.getService( JdbcServices.class );

		this.properties = new HashMap<>();
		this.properties.putAll( serviceRegistry.getService( ConfigurationService.class ).getSettings() );
		if ( !properties.containsKey( AvailableSettings.JPA_VALIDATION_FACTORY ) ) {
			if ( getSessionFactoryOptions().getValidatorFactoryReference() != null ) {
				properties.put(
						AvailableSettings.JPA_VALIDATION_FACTORY,
						getSessionFactoryOptions().getValidatorFactoryReference()
				);
			}
		}

		maskOutSensitiveInformation(this.properties);
		logIfEmptyCompositesEnabled( this.properties );

		this.sqlFunctionRegistry = new SQLFunctionRegistry( jdbcServices.getJdbcEnvironment().getDialect(), options.getCustomSqlFunctionMap() );
		this.cacheAccess = this.serviceRegistry.getService( CacheImplementor.class );
		this.criteriaBuilder = new CriteriaBuilderImpl( this );
		this.jpaPersistenceUnitUtil = new PersistenceUnitUtilImpl( this );

		for ( SessionFactoryObserver sessionFactoryObserver : options.getSessionFactoryObservers() ) {
			this.observer.addObserver( sessionFactoryObserver );
		}

		this.typeHelper = new TypeLocatorImpl( metadata.getTypeConfiguration().getTypeResolver() );

		this.filters = new HashMap<>();
		this.filters.putAll( metadata.getFilterDefinitions() );

		LOG.debugf( "Session factory constructed with filter configurations : %s", filters );
		LOG.debugf( "Instantiating session factory with properties: %s", properties );

		this.queryPlanCache = new QueryPlanCache( this, queryPlanCacheFunction );

		class IntegratorObserver implements SessionFactoryObserver {
			private ArrayList<Integrator> integrators = new ArrayList<>();

			@Override
			public void sessionFactoryCreated(SessionFactory factory) {
			}

			@Override
			public void sessionFactoryClosed(SessionFactory factory) {
				for ( Integrator integrator : integrators ) {
					integrator.disintegrate( SessionFactoryImpl.this, SessionFactoryImpl.this.serviceRegistry );
				}
				integrators.clear();
			}
		}
		final IntegratorObserver integratorObserver = new IntegratorObserver();
		this.observer.addObserver( integratorObserver );
		try {
			for ( Integrator integrator : serviceRegistry.getService( IntegratorService.class ).getIntegrators() ) {
				integrator.integrate( metadata, this, this.serviceRegistry );
				integratorObserver.integrators.add( integrator );
			}
			//Generators:
			this.identifierGenerators = new HashMap<>();
			metadata.getEntityBindings().stream().filter( model -> !model.isInherited() ).forEach( model -> {
				IdentifierGenerator generator = model.getIdentifier().createIdentifierGenerator(
						metadata.getIdentifierGeneratorFactory(),
						jdbcServices.getJdbcEnvironment().getDialect(),
						settings.getDefaultCatalogName(),
						settings.getDefaultSchemaName(),
						(RootClass) model
				);
				identifierGenerators.put( model.getEntityName(), generator );
			} );

			LOG.debug( "Instantiated session factory" );

			this.metamodel = metadata.getTypeConfiguration().scope( this );
			( (MetamodelImpl) this.metamodel ).initialize(
					metadata,
					determineJpaMetaModelPopulationSetting( properties )
			);

			//Named Queries:
			this.namedQueryRepository = metadata.buildNamedQueryRepository( this );

			settings.getMultiTableBulkIdStrategy().prepare(
					jdbcServices,
					buildLocalConnectionAccess(),
					metadata,
					sessionFactoryOptions
			);

			SchemaManagementToolCoordinator.process(
					metadata,
					serviceRegistry,
					properties,
					action -> SessionFactoryImpl.this.delayedDropAction = action
			);

			currentSessionContext = buildCurrentSessionContext();

			//checking for named queries
			if ( settings.isNamedQueryStartupCheckingEnabled() ) {
				final Map<String, HibernateException> errors = checkNamedQueries();
				if ( !errors.isEmpty() ) {
					StringBuilder failingQueries = new StringBuilder( "Errors in named queries: " );
					String separator = System.lineSeparator();

					for ( Map.Entry<String, HibernateException> entry : errors.entrySet() ) {
						LOG.namedQueryError( entry.getKey(), entry.getValue() );

						failingQueries
							.append( separator)
							.append( entry.getKey() )
							.append( " failed because of: " )
							.append( entry.getValue() );
					}
					throw new HibernateException( failingQueries.toString() );
				}
			}

			// this needs to happen after persisters are all ready to go...
			this.fetchProfiles = new HashMap<>();
			for ( org.hibernate.mapping.FetchProfile mappingProfile : metadata.getFetchProfiles() ) {
				final FetchProfile fetchProfile = new FetchProfile( mappingProfile.getName() );
				for ( org.hibernate.mapping.FetchProfile.Fetch mappingFetch : mappingProfile.getFetches() ) {
					// resolve the persister owning the fetch
					final String entityName = metamodel.getImportedClassName( mappingFetch.getEntity() );
					final EntityPersister owner = entityName == null
							? null
							: metamodel.entityPersister( entityName );
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

			this.defaultSessionOpenOptions = withOptions();
			this.temporarySessionOpenOptions = buildTemporarySessionOpenOptions();
			this.fastSessionServices = new FastSessionServices( this );

			this.observer.sessionFactoryCreated( this );

			SessionFactoryRegistry.INSTANCE.addSessionFactory(
					getUuid(),
					name,
					settings.isSessionFactoryNameAlsoJndiName(),
					this,
					serviceRegistry.getService( JndiService.class )
			);
		}
		catch (Exception e) {
			for ( Integrator integrator : serviceRegistry.getService( IntegratorService.class ).getIntegrators() ) {
				integrator.disintegrate( this, serviceRegistry );
				integratorObserver.integrators.remove( integrator );
			}
			close();
			throw e;
		}
	}

	private SessionBuilder buildTemporarySessionOpenOptions() {
		return withOptions()
				.autoClose( false )
				.flushMode( FlushMode.MANUAL )
				.connectionHandlingMode( PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT );
	}

	private void applyCfgXmlValues(LoadedConfig aggregatedConfig, SessionFactoryServiceRegistry serviceRegistry) {
		final JaccService jaccService = serviceRegistry.getService( JaccService.class );
		if ( jaccService.getContextId() != null ) {
			final JaccPermissionDeclarations permissions = aggregatedConfig.getJaccPermissions( jaccService.getContextId() );
			if ( permissions != null ) {
				for ( GrantedPermission grantedPermission : permissions.getPermissionDeclarations() ) {
					jaccService.addPermission( grantedPermission );
				}
			}
		}

		if ( aggregatedConfig.getEventListenerMap() != null ) {
			final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
			final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
			for ( Map.Entry<EventType, Set<String>> entry : aggregatedConfig.getEventListenerMap().entrySet() ) {
				final EventListenerGroup group = eventListenerRegistry.getEventListenerGroup( entry.getKey() );
				for ( String listenerClassName : entry.getValue() ) {
					try {
						group.appendListener( cls.classForName( listenerClassName ).newInstance() );
					}
					catch (Exception e) {
						throw new ConfigurationException( "Unable to instantiate event listener class : " + listenerClassName, e );
					}
				}
			}
		}
	}

	private JdbcConnectionAccess buildLocalConnectionAccess() {
		if ( settings.getMultiTenancyStrategy().requiresMultiTenantConnectionProvider() ) {
			final MultiTenantConnectionProvider mTenantConnectionProvider = serviceRegistry.getService( MultiTenantConnectionProvider.class );
			return new JdbcEnvironmentInitiator.MultiTenantConnectionProviderJdbcConnectionAccess( mTenantConnectionProvider );
		}
		else {
			final ConnectionProvider connectionProvider = serviceRegistry.getService( ConnectionProvider.class );
			return new JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess( connectionProvider );
		}
	}

	public Session openSession() throws HibernateException {
		final CurrentTenantIdentifierResolver currentTenantIdentifierResolver = getCurrentTenantIdentifierResolver();
		//We can only use reuse the defaultSessionOpenOptions as a constant when there is no TenantIdentifierResolver
		if ( currentTenantIdentifierResolver != null ) {
			return this.withOptions().openSession();
		}
		else {
			return this.defaultSessionOpenOptions.openSession();
		}
	}

	public Session openTemporarySession() throws HibernateException {
		final CurrentTenantIdentifierResolver currentTenantIdentifierResolver = getCurrentTenantIdentifierResolver();
		//We can only use reuse the defaultSessionOpenOptions as a constant when there is no TenantIdentifierResolver
		if ( currentTenantIdentifierResolver != null ) {
			return buildTemporarySessionOpenOptions()
					.openSession();
		}
		else {
			return this.temporarySessionOpenOptions.openSession();
		}
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

	@Override
	public Map<String, Object> getProperties() {
		validateNotClosed();
		return properties;
	}

	protected void validateNotClosed() {
		if ( isClosed ) {
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
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return null;
	}

	/**
	 * Retrieve the {@link Type} resolver associated with this factory.
	 *
	 * @return The type resolver
	 *
	 * @deprecated (since 5.3) No replacement, access to and handling of Types will be much different in 6.0
	 */
	@Deprecated
	public TypeResolver getTypeResolver() {
		return metamodel.getTypeConfiguration().getTypeResolver();
	}

	public QueryPlanCache getQueryPlanCache() {
		return queryPlanCache;
	}

	private Map<String,HibernateException> checkNamedQueries() throws HibernateException {
		return namedQueryRepository.checkNamedQueries( queryPlanCache );
	}

	@Override
	public DeserializationResolver getDeserializationResolver() {
		return new DeserializationResolver() {
			@Override
			public SessionFactoryImplementor resolve() {
				return (SessionFactoryImplementor) SessionFactoryRegistry.INSTANCE.findSessionFactory(
						uuid,
						name
				);
			}
		};
	}

	@SuppressWarnings("deprecation")
	public Settings getSettings() {
		return settings;
	}

	@Override
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		return getMetamodel().findEntityGraphsByJavaType( entityClass );
	}



	// todo : (5.2) review synchronizationType, persistenceContextType, transactionType usage

	// SynchronizationType -> should we auto enlist in transactions
	private transient SynchronizationType synchronizationType;

	// PersistenceContextType -> influences FlushMode and 'autoClose'
	private transient PersistenceContextType persistenceContextType;


	@Override
	public Session createEntityManager() {
		validateNotClosed();
		return buildEntityManager( SynchronizationType.SYNCHRONIZED, null );
	}

	private <K,V> Session buildEntityManager(final SynchronizationType synchronizationType, final Map<K,V> map) {
		assert !isClosed;

		SessionBuilderImplementor builder = withOptions();
		if ( synchronizationType == SynchronizationType.SYNCHRONIZED ) {
			builder.autoJoinTransactions( true );
		}
		else {
			builder.autoJoinTransactions( false );
		}

		final Session session = builder.openSession();
		if ( map != null ) {
			for ( Map.Entry<K, V> o : map.entrySet() ) {
				final K key = o.getKey();
				if ( key instanceof String ) {
					final String sKey = (String) key;
					session.setProperty( sKey, o.getValue() );
				}
			}
		}
		return session;
	}

	@Override
	public Session createEntityManager(Map map) {
		validateNotClosed();
		return buildEntityManager( SynchronizationType.SYNCHRONIZED, map );
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
		if ( !getServiceRegistry().getService( TransactionCoordinatorBuilder.class ).isJta() ) {
			throw new IllegalStateException(
					"Illegal attempt to specify a SynchronizationType when building an EntityManager from a " +
							"EntityManagerFactory defined as RESOURCE_LOCAL (as opposed to JTA)"
			);
		}
	}

	@Override
	public Session createEntityManager(SynchronizationType synchronizationType, Map map) {
		validateNotClosed();
		errorIfResourceLocalDueToExplicitSynchronizationType();
		return buildEntityManager( synchronizationType, map );
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		validateNotClosed();
		return criteriaBuilder;
	}

	@Override
	public MetamodelImplementor getMetamodel() {
		validateNotClosed();
		return metamodel;
	}

	@Override
	public boolean isOpen() {
		return !isClosed;
	}

	@Override
	public RootGraphImplementor findEntityGraphByName(String name) {
		return getMetamodel().findEntityGraphByName( name );
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
	public NamedQueryRepository getNamedQueryRepository() {
		return namedQueryRepository;
	}


	public Type getIdentifierType(String className) throws MappingException {
		return getMetamodel().entityPersister( className ).getIdentifierType();
	}
	public String getIdentifierPropertyName(String className) throws MappingException {
		return getMetamodel().entityPersister( className ).getIdentifierPropertyName();
	}

	public Type[] getReturnTypes(String queryString) throws HibernateException {
		final ReturnMetadata metadata = queryPlanCache.getHQLQueryPlan( queryString, false, Collections.EMPTY_MAP )
				.getReturnMetadata();
		return metadata == null ? null : metadata.getReturnTypes();
	}

	public String[] getReturnAliases(String queryString) throws HibernateException {
		final ReturnMetadata metadata = queryPlanCache.getHQLQueryPlan( queryString, false, Collections.EMPTY_MAP )
				.getReturnMetadata();
		return metadata == null ? null : metadata.getReturnAliases();
	}

	public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
		return getClassMetadata( persistentClass.getName() );
	}

	public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
		return (CollectionMetadata) getMetamodel().collectionPersister( roleName );
	}

	public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
		return (ClassMetadata) getMetamodel().entityPersister( entityName );
	}

	@Override
	public Map<String,ClassMetadata> getAllClassMetadata() throws HibernateException {
		throw new UnsupportedOperationException( "org.hibernate.SessionFactory.getAllClassMetadata is no longer supported" );
	}

	public Map getAllCollectionMetadata() throws HibernateException {
		throw new UnsupportedOperationException( "org.hibernate.SessionFactory.getAllCollectionMetadata is no longer supported" );
	}

	public Type getReferencedPropertyType(String className, String propertyName)
		throws MappingException {
		return getMetamodel().entityPersister( className ).getPropertyType( propertyName );
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
		synchronized (this) {
			if ( isClosed ) {
				if ( getSessionFactoryOptions().getJpaCompliance().isJpaClosedComplianceEnabled() ) {
					throw new IllegalStateException( "EntityManagerFactory is already closed" );
				}

				LOG.trace( "Already closed" );
				return;
			}

			isClosed = true;
		}

		LOG.closing();
		observer.sessionFactoryClosing( this );

		settings.getMultiTableBulkIdStrategy().release( serviceRegistry.getService( JdbcServices.class ), buildLocalConnectionAccess() );

		// NOTE : the null checks below handle cases where close is called from
		//		a failed attempt to create the SessionFactory

		if ( cacheAccess != null ) {
			cacheAccess.close();
		}

		if ( metamodel != null ) {
			metamodel.close();
		}

		if ( queryPlanCache != null ) {
			queryPlanCache.cleanup();
		}

		if ( delayedDropAction != null ) {
			delayedDropAction.perform( serviceRegistry );
		}

		SessionFactoryRegistry.INSTANCE.removeSessionFactory(
				getUuid(),
				name,
				settings.isSessionFactoryNameAlsoJndiName(),
				serviceRegistry.getService( JndiService.class )
		);

		observer.sessionFactoryClosed( this );
		serviceRegistry.destroy();
	}

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
	public void addNamedQuery(String name, Query query) {
		validateNotClosed();

		// NOTE : we use Query#unwrap here (rather than direct type checking) to account for possibly wrapped
		// query implementations

		// first, handle StoredProcedureQuery
		try {
			final ProcedureCall unwrapped = query.unwrap( ProcedureCall.class );
			if ( unwrapped != null ) {
				addNamedStoredProcedureQuery( name, unwrapped );
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a StoredProcedureQueryImpl
		}

		// then try as a native-SQL or JPQL query
		try {
			org.hibernate.query.Query hibernateQuery = query.unwrap( org.hibernate.query.Query.class );
			if ( hibernateQuery != null ) {
				// create and register the proper NamedQueryDefinition...
				if ( NativeQuery.class.isInstance( hibernateQuery ) ) {
					getNamedQueryRepository().registerNamedSQLQueryDefinition(
							name,
							extractSqlQueryDefinition( (NativeQuery) hibernateQuery, name )
					);
				}
				else {
					getNamedQueryRepository().registerNamedQueryDefinition(
							name,
							extractHqlQueryDefinition( hibernateQuery, name )
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
						"Unsure how to how to properly unwrap given Query [%s] as basis for named query",
						query
				)
		);
	}

	private void addNamedStoredProcedureQuery(String name, ProcedureCall procedureCall) {
		getNamedQueryRepository().registerNamedProcedureCallMemento(
				name,
				procedureCall.extractMemento( procedureCall.getHints() )
		);
	}

	private NamedSQLQueryDefinition extractSqlQueryDefinition(NativeQuery nativeSqlQuery, String name) {
		final NamedSQLQueryDefinitionBuilder builder = new NamedSQLQueryDefinitionBuilder( name );
		fillInNamedQueryBuilder( builder, nativeSqlQuery );
		builder.setCallable( nativeSqlQuery.isCallable() )
				.setQuerySpaces( nativeSqlQuery.getSynchronizedQuerySpaces() )
				.setQueryReturns( nativeSqlQuery.getQueryReturns() );
		return builder.createNamedQueryDefinition();
	}

	private NamedQueryDefinition extractHqlQueryDefinition(org.hibernate.query.Query hqlQuery, String name) {
		final NamedQueryDefinitionBuilder builder = new NamedQueryDefinitionBuilder( name );
		fillInNamedQueryBuilder( builder, hqlQuery );
		// LockOptions only valid for HQL/JPQL queries...
		builder.setLockOptions( hqlQuery.getLockOptions().makeCopy() );
		return builder.createNamedQueryDefinition();
	}

	private void fillInNamedQueryBuilder(NamedQueryDefinitionBuilder builder, org.hibernate.query.Query query) {
		builder.setQuery( query.getQueryString() )
				.setComment( query.getComment() )
				.setCacheable( query.isCacheable() )
				.setCacheRegion( query.getCacheRegion() )
				.setCacheMode( query.getCacheMode() )
				.setReadOnly( query.isReadOnly() )
				.setFlushMode( query.getHibernateFlushMode() );

		if ( query.getQueryOptions().getFirstRow() != null ) {
			builder.setFirstResult( query.getQueryOptions().getFirstRow() );
		}

		if ( query.getQueryOptions().getMaxRows() != null ) {
			builder.setMaxResults( query.getQueryOptions().getMaxRows() );
		}

		if ( query.getQueryOptions().getTimeout() != null ) {
			builder.setTimeout( query.getQueryOptions().getTimeout() );
		}

		if ( query.getQueryOptions().getFetchSize() != null ) {
			builder.setFetchSize( query.getQueryOptions().getFetchSize() );
		}
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		if ( type.isAssignableFrom( SessionFactory.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( SessionFactoryImplementor.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( SessionFactoryImpl.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( EntityManagerFactory.class ) ) {
			return type.cast( this );
		}

		throw new PersistenceException( "Hibernate cannot unwrap EntityManagerFactory as '" + type.getName() + "'" );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		getMetamodel().addNamedEntityGraph( graphName, (RootGraphImplementor<T>) entityGraph );
	}

	public boolean isClosed() {
		return isClosed;
	}

	private transient StatisticsImplementor statistics;

	public StatisticsImplementor getStatistics() {
		if ( statistics == null ) {
			statistics = serviceRegistry.getService( StatisticsImplementor.class );
		}
		return statistics;
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

	private boolean canAccessTransactionManager() {
		try {
			return serviceRegistry.getService( JtaPlatform.class ).retrieveTransactionManager() != null;
		}
		catch (Exception e) {
			return false;
		}
	}

	private CurrentSessionContext buildCurrentSessionContext() {
		String impl = (String) properties.get( Environment.CURRENT_SESSION_CONTEXT_CLASS );
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
//			if ( ! transactionFactory().compatibleWithJtaSynchronization() ) {
//				LOG.autoFlushWillNotWork();
//			}
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
				return (CurrentSessionContext)
						implClass.getConstructor( new Class[] { SessionFactoryImplementor.class } )
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

	@Override
	public Type resolveParameterBindType(Object bindValue) {
		if ( bindValue == null ) {
			// we can't guess
			return null;
		}

		return resolveParameterBindType( HibernateProxyHelper.getClassWithoutInitializingProxy( bindValue ) );
	}

	@Override
	public Type resolveParameterBindType(Class clazz){
		String typename = clazz.getName();
		Type type = getTypeResolver().heuristicType( typename );
		boolean serializable = type != null && type instanceof SerializableType;
		if ( type == null || serializable ) {
			try {
				getMetamodel().entityPersister( clazz.getName() );
			}
			catch (MappingException me) {
				if ( serializable ) {
					return type;
				}
				else {
					throw new HibernateException( "Could not determine a type for class: " + typename );
				}
			}
			return getTypeHelper().entity( clazz );
		}
		else {
			return type;
		}
	}

	public static Interceptor configuredInterceptor(Interceptor interceptor, SessionFactoryOptions options) {
		// NOTE : DO NOT return EmptyInterceptor.INSTANCE from here as a "default for the Session"
		// 		we "filter" that one out here.  The return from here should represent the
		//		explicitly configured Interceptor (if one).  Return null from here instead; Session
		//		will handle it

		if ( interceptor != null && interceptor != EmptyInterceptor.INSTANCE ) {
			return interceptor;
		}

		// prefer the SF-scoped interceptor, prefer that to any Session-scoped interceptor prototype
		final Interceptor optionsInterceptor = options.getInterceptor();
		if ( optionsInterceptor != null && optionsInterceptor != EmptyInterceptor.INSTANCE ) {
			return optionsInterceptor;
		}

		// then check the Session-scoped interceptor prototype
		final Class<? extends Interceptor> statelessInterceptorImplementor = options.getStatelessInterceptorImplementor();
		final Supplier<? extends Interceptor> statelessInterceptorImplementorSupplier = options.getStatelessInterceptorImplementorSupplier();
		if ( statelessInterceptorImplementor != null && statelessInterceptorImplementorSupplier != null ) {
			throw new HibernateException(
					"A session scoped interceptor class or supplier are allowed, but not both!" );
		}
		else if ( statelessInterceptorImplementor != null ) {
			try {
				/**
				 * We could remove the getStatelessInterceptorImplementor method and use just the getStatelessInterceptorImplementorSupplier
				 * since it can cover both cases when the user has given a Supplier<? extends Interceptor> or just the
				 * Class<? extends Interceptor>, in which case, we simply instantiate the Interceptor when calling the Supplier.
				 */
				return statelessInterceptorImplementor.newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new HibernateException( "Could not supply session-scoped SessionFactory Interceptor", e );
			}
		}
		else if ( statelessInterceptorImplementorSupplier != null ) {
			return statelessInterceptorImplementorSupplier.get();
		}

		return null;
	}

	public static class SessionBuilderImpl<T extends SessionBuilder> implements SessionBuilderImplementor<T>, SessionCreationOptions {
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
		private String tenantIdentifier;
		private TimeZone jdbcTimeZone;
		private boolean queryParametersValidationEnabled;

		// Lazy: defaults can be built by invoking the builder in fastSessionServices.defaultSessionEventListeners
		// (Need a fresh build for each Session as the listener instances can't be reused across sessions)
		// Only initialize of the builder is overriding the default.
		private List<SessionEventListener> listeners;

		//todo : expose setting
		private SessionOwnerBehavior sessionOwnerBehavior = SessionOwnerBehavior.LEGACY_NATIVE;

		public SessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;

			// set up default builder values...
			final SessionFactoryOptions sessionFactoryOptions = sessionFactory.getSessionFactoryOptions();
			this.statementInspector = sessionFactoryOptions.getStatementInspector();
			this.connectionHandlingMode = sessionFactoryOptions.getPhysicalConnectionHandlingMode();
			this.autoClose = sessionFactoryOptions.isAutoCloseSessionEnabled();
			this.flushMode = sessionFactoryOptions.isFlushBeforeCompletionEnabled()
					? FlushMode.AUTO
					: FlushMode.MANUAL;

			final CurrentTenantIdentifierResolver currentTenantIdentifierResolver = sessionFactory.getCurrentTenantIdentifierResolver();
			if ( currentTenantIdentifierResolver != null ) {
				tenantIdentifier = currentTenantIdentifierResolver.resolveCurrentTenantIdentifier();
			}
			this.jdbcTimeZone = sessionFactoryOptions.getJdbcTimeZone();
			this.queryParametersValidationEnabled = sessionFactoryOptions.isQueryParametersValidationEnabled();
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SessionCreationOptions

		@Override
		public SessionOwner getSessionOwner() {
			return null;
		}

		@Override
		public ExceptionMapper getExceptionMapper() {
			return sessionOwnerBehavior == SessionOwnerBehavior.LEGACY_JPA
					? ExceptionMapperLegacyJpaImpl.INSTANCE
					: null;
		}

		@Override
		public AfterCompletionAction getAfterCompletionAction() {
			return sessionOwnerBehavior == SessionOwnerBehavior.LEGACY_JPA
					? AfterCompletionActionLegacyJpaImpl.INSTANCE
					: null;
		}

		@Override
		public ManagedFlushChecker getManagedFlushChecker() {
			return sessionOwnerBehavior == SessionOwnerBehavior.LEGACY_JPA
					? ManagedFlushCheckerLegacyJpaImpl.INSTANCE
					: null;
		}

		@Override
		public boolean isQueryParametersValidationEnabled() {
			return this.queryParametersValidationEnabled;
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
			return configuredInterceptor( interceptor, sessionFactory.getSessionFactoryOptions() );
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
		public Session openSession() {
			log.tracef( "Opening Hibernate Session.  tenant=%s", tenantIdentifier );
			return new SessionImpl( sessionFactory, this );
		}

		@Override
		@SuppressWarnings("unchecked")
		public T owner(SessionOwner sessionOwner) {
			throw new UnsupportedOperationException( "SessionOwner was long deprecated and this method should no longer be invoked" );
		}

		@Override
		@SuppressWarnings("unchecked")
		public T interceptor(Interceptor interceptor) {
			this.interceptor = interceptor;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T noInterceptor() {
			this.interceptor = EmptyInterceptor.INSTANCE;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T statementInspector(StatementInspector statementInspector) {
			this.statementInspector = statementInspector;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T connection(Connection connection) {
			this.connection = connection;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
			// NOTE : Legacy behavior (when only ConnectionReleaseMode was exposed) was to always acquire a
			// Connection using ConnectionAcquisitionMode.AS_NEEDED..

			final PhysicalConnectionHandlingMode handlingMode = PhysicalConnectionHandlingMode.interpret(
					ConnectionAcquisitionMode.AS_NEEDED,
					connectionReleaseMode
			);
			connectionHandlingMode( handlingMode );
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
			this.connectionHandlingMode = connectionHandlingMode;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T autoJoinTransactions(boolean autoJoinTransactions) {
			this.autoJoinTransactions = autoJoinTransactions;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T autoClose(boolean autoClose) {
			this.autoClose = autoClose;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T autoClear(boolean autoClear) {
			this.autoClear = autoClear;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T flushMode(FlushMode flushMode) {
			this.flushMode = flushMode;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T eventListeners(SessionEventListener... listeners) {
			if ( this.listeners == null ) {
				this.listeners = sessionFactory.getSessionFactoryOptions()
						.getBaselineSessionEventsListenerBuilder()
						.buildBaselineList();
			}
			Collections.addAll( this.listeners, listeners );
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T clearEventListeners() {
			if ( listeners == null ) {
				//Needs to initialize explicitly to an empty list as otherwise "null" immplies the default listeners will be applied
				this.listeners = new ArrayList<SessionEventListener>( 3 );
			}
			else {
				listeners.clear();
			}
			return (T) this;
		}

		@Override
		public T jdbcTimeZone(TimeZone timeZone) {
			jdbcTimeZone = timeZone;
			return (T) this;
		}

		@Override
		public T setQueryParameterValidation(boolean enabled) {
			queryParametersValidationEnabled = enabled;
			return (T) this;
		}
	}

	public static class StatelessSessionBuilderImpl implements StatelessSessionBuilder, SessionCreationOptions {
		private final SessionFactoryImpl sessionFactory;
		private Connection connection;
		private String tenantIdentifier;
		private boolean queryParametersValidationEnabled;

		public StatelessSessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;

			CurrentTenantIdentifierResolver tenantIdentifierResolver = sessionFactory.getCurrentTenantIdentifierResolver();
			if ( tenantIdentifierResolver != null ) {
				tenantIdentifier = tenantIdentifierResolver.resolveCurrentTenantIdentifier();
			}
			queryParametersValidationEnabled = sessionFactory.getSessionFactoryOptions().isQueryParametersValidationEnabled();
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

		@Override
		public StatelessSessionBuilder tenantIdentifier(String tenantIdentifier) {
			this.tenantIdentifier = tenantIdentifier;
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
			return configuredInterceptor( EmptyInterceptor.INSTANCE, sessionFactory.getSessionFactoryOptions() );

		}

		@Override
		public StatementInspector getStatementInspector() {
			return null;
		}

		@Override
		public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
			return null;
		}

		@Override
		public String getTenantIdentifier() {
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
		public SessionOwner getSessionOwner() {
			return null;
		}

		@Override
		public ExceptionMapper getExceptionMapper() {
			return null;
		}

		@Override
		public AfterCompletionAction getAfterCompletionAction() {
			return null;
		}

		@Override
		public ManagedFlushChecker getManagedFlushChecker() {
			return null;
		}

		@Override
		public boolean isQueryParametersValidationEnabled() {
			return queryParametersValidationEnabled;
		}

		@Override
		public StatelessSessionBuilder setQueryParameterValidation(boolean enabled) {
			queryParametersValidationEnabled = enabled;
			return this;
		}
	}

	@Override
	public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
		return getSessionFactoryOptions().getCustomEntityDirtinessStrategy();
	}

	@Override
	public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
		return getSessionFactoryOptions().getCurrentTenantIdentifierResolver();
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
		LOG.debugf( "Serializing: %s", getUuid() );
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
		LOG.debugf( "Deserialized: %s", getUuid() );
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
		return locateSessionFactoryOnDeserialization( getUuid(), name );
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
		oos.writeUTF( getUuid() );
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

	private void maskOutSensitiveInformation(Map<String, Object> props) {
		maskOutIfSet( props, AvailableSettings.JPA_JDBC_USER );
		maskOutIfSet( props, AvailableSettings.JPA_JDBC_PASSWORD );
		maskOutIfSet( props, AvailableSettings.USER );
		maskOutIfSet( props, AvailableSettings.PASS );
	}

	private void maskOutIfSet(Map<String, Object> props, String setting) {
		if ( props.containsKey( setting ) ) {
			props.put( setting, "****" );
		}
	}

	private void logIfEmptyCompositesEnabled(Map<String, Object> props ) {
		final boolean isEmptyCompositesEnabled = ConfigurationHelper.getBoolean(
				AvailableSettings.CREATE_EMPTY_COMPOSITES_ENABLED,
				props,
				false
		);
		if ( isEmptyCompositesEnabled ) {
			// It would be nice to do this logging in ComponentMetamodel, where
			// AvailableSettings.CREATE_EMPTY_COMPOSITES_ENABLED is actually used.
			// Unfortunately that would end up logging a message several times for
			// each embeddable/composite. Doing it here will log the message only
			// once.
			LOG.emptyCompositesEnabled();
		}
	}

	/**
	 * @return the FastSessionServices for this SessionFactory.
	 */
	@Override
	public FastSessionServices getFastSessionServices() {
		return this.fastSessionServices;
	}

}
