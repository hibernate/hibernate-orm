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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EmptyInterceptor;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.Metamodel;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
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
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.profile.Association;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.internal.reader.AuditReaderImpl;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.internal.util.config.ConfigurationException;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.internal.AfterCompletionActionLegacyJpaImpl;
import org.hibernate.jpa.internal.ExceptionMapperLegacyJpaImpl;
import org.hibernate.jpa.internal.ManagedFlushCheckerLegacyJpaImpl;
import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.spi.HqlQueryImplementor;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
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
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tool.schema.spi.DelayedDropAction;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;


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
public final class SessionFactoryImpl implements SessionFactoryImplementor {
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

	private final transient SqmFunctionRegistry sqmFunctionRegistry;

	// todo : org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor too?

	private final transient TypeConfiguration typeConfiguration;
	private final transient MetamodelImplementor metamodel;
	private final transient PersistenceUnitUtil jpaPersistenceUnitUtil;

	private final transient CacheImplementor cacheEngine;

	private final transient QueryEngine queryEngine;

	private final transient CurrentSessionContext currentSessionContext;

	private volatile DelayedDropAction delayedDropAction;

	// todo (6.0) : move to MetamodelImpl
	private final transient Map<String,IdentifierGenerator> identifierGenerators;
	private final transient Map<String, FilterDefinition> filters;
	private final transient Map<String, FetchProfile> fetchProfiles;

	private transient StatisticsImplementor statistics;

	public SessionFactoryImpl(
			final BootstrapContext bootstrapContext,
			final MetadataImplementor metadata,
			SessionFactoryOptions options) {
		LOG.debug( "Building session factory" );

		this.sessionFactoryOptions = options;
		this.settings = new Settings( options, metadata );

		this.serviceRegistry = options
				.getServiceRegistry()
				.getService( SessionFactoryServiceRegistryFactory.class )
				.buildServiceRegistry( this, bootstrapContext, options );

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

		maskOutSensitiveInformation( this.properties );
		logIfEmptyCompositesEnabled( this.properties );

		this.cacheEngine = this.serviceRegistry.getService( CacheImplementor.class );
		this.jpaPersistenceUnitUtil = new PersistenceUnitUtilImpl( this );

		for ( SessionFactoryObserver sessionFactoryObserver : options.getSessionFactoryObservers() ) {
			this.observer.addObserver( sessionFactoryObserver );
		}

		this.filters = new HashMap<>();
		this.filters.putAll( metadata.getFilterDefinitions() );

		LOG.debugf( "Session factory constructed with filter configurations : %s", filters );
		LOG.debugf( "Instantiating session factory with properties: %s", properties );

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

			final IdentifierGeneratorFactory identifierGeneratorFactory = serviceRegistry.getService(
					MutableIdentifierGeneratorFactory.class
			);

			//Generators:
			this.identifierGenerators = new HashMap<>();

			metadata.getEntityMappings().stream()
					.filter( entityMapping -> entityMapping.getEntityMappingHierarchy().getRootType().equals( entityMapping ) )
					.map( RootClass.class::cast )
					.forEach( rootClass -> {
						IdentifierGenerator generator =  rootClass.getIdentifier()
								.createIdentifierGenerator(
										identifierGeneratorFactory,
										jdbcServices.getJdbcEnvironment().getDialect(),
										settings.getDefaultCatalogName(),
										settings.getDefaultSchemaName(),
										rootClass
								);
						identifierGenerators.put( rootClass.getEntityName(), generator );
					} );

			LOG.debug( "Instantiated session factory" );

			this.typeConfiguration = metadata.getTypeConfiguration();

			this.metamodel = typeConfiguration.scope( this, bootstrapContext );

			prepareEventListeners( metamodel );

			this.sqmFunctionRegistry = new SqmFunctionRegistry();
			jdbcServices.getDialect().initializeFunctionRegistry( sqmFunctionRegistry );
			sessionFactoryOptions.getSqmFunctionRegistry().overlay( sqmFunctionRegistry );

			this.queryEngine = new QueryEngine(
					this,
					metadata.buildNamedQueryRepository( this ),
					sqmFunctionRegistry
			);

			settings.getMultiTableBulkIdStrategy().prepare(
					metamodel,
					sessionFactoryOptions,
					buildLocalConnectionAccess()
			);

			currentSessionContext = buildCurrentSessionContext();

			// this needs to happen after all persisters are all ready to go...
			this.fetchProfiles = new HashMap<>();
			for ( org.hibernate.mapping.FetchProfile mappingProfile : metadata.getFetchProfiles() ) {
				final FetchProfile fetchProfile = new FetchProfile( mappingProfile.getName() );
				for ( org.hibernate.mapping.FetchProfile.Fetch mappingFetch : mappingProfile.getFetches() ) {
					// resolve the persister owning the fetch
					final EntityTypeDescriptor owner = metamodel.findEntityDescriptor( mappingFetch.getEntity() );
					if ( owner == null ) {
						throw new HibernateException(
								"Unable to resolve entity reference [" + mappingFetch.getEntity()
										+ "] in fetch profile [" + fetchProfile.getName() + "]"
						);
					}

					final Attribute attribute = owner.getAttribute( mappingFetch.getAssociation() );
					if ( !Fetchable.class.isInstance( attribute ) ) {
						throw new HibernateException( "Fetch profile [" + fetchProfile.getName() + "] specified an invalid association" );
					}

					// resolve the style
					final Fetch.Style fetchStyle = Fetch.Style.parse( mappingFetch.getStyle() );

					// then construct the fetch instance...
					fetchProfile.addFetch( new Association( owner, mappingFetch.getAssociation() ), fetchStyle );
					owner.registerAffectingFetchProfile( fetchProfile.getName() );
				}
				fetchProfiles.put( fetchProfile.getName(), fetchProfile );
			}

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

	private void prepareEventListeners(Metamodel metamodel) {
		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		eventListenerRegistry.prepare( metamodel );

		for ( Map.Entry entry : ( (Map<?, ?>) cfgService.getSettings() ).entrySet() ) {
			if ( !String.class.isInstance( entry.getKey() ) ) {
				continue;
			}
			final String propertyName = (String) entry.getKey();
			if ( !propertyName.startsWith( org.hibernate.jpa.AvailableSettings.EVENT_LISTENER_PREFIX ) ) {
				continue;
			}
			final String eventTypeName = propertyName.substring( org.hibernate.jpa.AvailableSettings.EVENT_LISTENER_PREFIX.length() + 1 );
			final EventType eventType = EventType.resolveEventTypeByName( eventTypeName );
			final EventListenerGroup eventListenerGroup = eventListenerRegistry.getEventListenerGroup( eventType );
			for ( String listenerImpl : ( (String) entry.getValue() ).split( " ," ) ) {
				eventListenerGroup.appendListener( instantiate( listenerImpl, classLoaderService ) );
			}
		}
	}

	private Object instantiate(String listenerImpl, ClassLoaderService classLoaderService) {
		try {
			return classLoaderService.classForName( listenerImpl ).newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Could not instantiate requested listener [" + listenerImpl + "]", e );
		}
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
		return new JdbcConnectionAccess() {
			@Override
			public Connection obtainConnection() throws SQLException {
				return !settings.getMultiTenancyStrategy().requiresMultiTenantConnectionProvider()
						? serviceRegistry.getService( ConnectionProvider.class ).getConnection()
						: serviceRegistry.getService( MultiTenantConnectionProvider.class ).getAnyConnection();
			}

			@Override
			public void releaseConnection(Connection connection) throws SQLException {
				if ( !settings.getMultiTenancyStrategy().requiresMultiTenantConnectionProvider() ) {
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

	@Override
	public QueryEngine getQueryEngine() {
		return queryEngine;
	}

	public Session openSession() throws HibernateException {
		return withOptions().openSession();
	}

	public Session openTemporarySession() throws HibernateException {
		return withOptions()
				.autoClose( false )
				.flushMode( FlushMode.MANUAL )
				.connectionHandlingMode( PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT )
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

	@Override
	public Map<String, Object> getProperties() {
		validateNotClosed();
		return properties;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
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
		return buildEntityManager( SynchronizationType.SYNCHRONIZED, Collections.emptyMap() );
	}

	private Session buildEntityManager(SynchronizationType synchronizationType, Map map) {
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
			map.keySet().forEach( key -> {
				if ( key instanceof String ) {
					session.setProperty( (String) key, map.get( key ) );
				}
			} );
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
		return buildEntityManager( synchronizationType, Collections.emptyMap() );
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
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		validateNotClosed();
		return queryEngine.getCriteriaBuilder();
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

	public Type getIdentifierType(String className) throws MappingException {
		return getMetamodel().findEntityDescriptor( className ).getIdentifierType();
	}

	public String getIdentifierPropertyName(String className) throws MappingException {
		return getMetamodel().findEntityDescriptor( className ).getIdentifierPropertyName();
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
		//This is an idempotent operation so we can do it even before the checks (it won't hurt):
		Environment.getBytecodeProvider().resetCaches();
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

		isClosed = true;

		settings.getMultiTableBulkIdStrategy().release( metamodel, buildLocalConnectionAccess() );

		// NOTE : the null checks below handle cases where close is called from
		//		a failed attempt to create the SessionFactory

		if ( queryEngine != null ) {
			queryEngine.close();
		}

		if ( cacheEngine != null ) {
			cacheEngine.close();
		}

		if ( metamodel != null ) {
			metamodel.close();
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
		return cacheEngine;
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
			final ProcedureCallImplementor unwrapped = query.unwrap( ProcedureCallImplementor.class );
			if ( unwrapped != null ) {
				getQueryEngine().getNamedQueryRepository().registerCallableQueryMemento(
						name,
						unwrapped.toMemento( name, this )
				);
				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a StoredProcedureQueryImpl
		}

		// next handle HqlQueryImplementor
		try {
			final HqlQueryImplementor hqlQuery = query.unwrap( HqlQueryImplementor.class );
			if ( hqlQuery != null ) {
				getQueryEngine().getNamedQueryRepository().registerHqlQueryMemento(
						name,
						hqlQuery.toMemento( name, this )
				);

				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a HQL/JPQL query
		}

		// lastly, try as a native query
		try {
			final NativeQueryImplementor nativeQuery = query.unwrap( NativeQueryImplementor.class );
			if ( nativeQuery != null ) {
				getQueryEngine().getNamedQueryRepository().registerNativeQueryMemento(
						name,
						nativeQuery.toMemento( name, this )
				);

				return;
			}
		}
		catch ( PersistenceException ignore ) {
			// this means 'query' is not a native query
		}

		// if we get here, we are unsure how to properly unwrap the incoming query to extract the needed information
		throw new PersistenceException(
				String.format(
						"Unsure how to how to properly unwrap given Query [%s] as basis for named query",
						query
				)
		);
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
		return identifierGenerators.get( rootEntityName );
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

	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return sqmFunctionRegistry;
	}

	public FetchProfile getFetchProfile(String name) {
		return fetchProfiles.get( name );
	}

	@Override
	public AllowableParameterType resolveParameterBindType(Object bindValue) {
		if ( bindValue == null ) {
			// we can't guess
			return null;
		}

		return resolveParameterBindType( HibernateProxyHelper.getClassWithoutInitializingProxy( bindValue ) );
	}

	@Override
	public AllowableParameterType resolveParameterBindType(Class clazz) {
		return getMetamodel().resolveAllowableParamterType( clazz );
	}

	@Override
	public AuditReader openAuditReader() {
		return new AuditReaderImpl( openSession(), true );
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
		if ( options.getInterceptor() != null && options.getInterceptor() != EmptyInterceptor.INSTANCE ) {
			return options.getInterceptor();
		}

		// then check the Session-scoped interceptor prototype
		if ( options.getStatelessInterceptorImplementor() != null && options.getStatelessInterceptorImplementorSupplier() != null ) {
			throw new HibernateException(
					"A session scoped interceptor class or supplier are allowed, but not both!" );
		}
		else if ( options.getStatelessInterceptorImplementor() != null ) {
			try {
				/**
				 * We could remove the getStatelessInterceptorImplementor method and use just the getStatelessInterceptorImplementorSupplier
				 * since it can cover both cases when the user has given a Supplier<? extends Interceptor> or just the
				 * Class<? extends Interceptor>, in which case, we simply instantiate the Interceptor when calling the Supplier.
				 */
				return options.getStatelessInterceptorImplementor().newInstance();
			}
			catch (InstantiationException | IllegalAccessException e) {
				throw new HibernateException( "Could not supply session-scoped SessionFactory Interceptor", e );
			}
		}
		else if ( options.getStatelessInterceptorImplementorSupplier() != null ) {
			return options.getStatelessInterceptorImplementorSupplier().get();
		}

		return null;
	}

	static class SessionBuilderImpl<T extends SessionBuilder> implements SessionBuilderImplementor<T>, SessionCreationOptions {
		private static final Logger log = CoreLogging.logger( SessionBuilderImpl.class );

		private final SessionFactoryImpl sessionFactory;
		private SessionOwner sessionOwner;
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

		private List<SessionEventListener> listeners;

		//todo : expose setting
		private SessionOwnerBehavior sessionOwnerBehavior = SessionOwnerBehavior.LEGACY_NATIVE;
		private PersistenceUnitTransactionType persistenceUnitTransactionType;

		SessionBuilderImpl(SessionFactoryImpl sessionFactory) {
			this.sessionFactory = sessionFactory;
			this.sessionOwner = null;

			// set up default builder values...
			this.statementInspector = sessionFactory.getSessionFactoryOptions().getStatementInspector();
			this.connectionHandlingMode = sessionFactory.getSessionFactoryOptions().getPhysicalConnectionHandlingMode();
			this.autoClose = sessionFactory.getSessionFactoryOptions().isAutoCloseSessionEnabled();
			this.flushMode = sessionFactory.getSessionFactoryOptions().isFlushBeforeCompletionEnabled()
					? FlushMode.AUTO
					: FlushMode.MANUAL;

			if ( sessionFactory.getCurrentTenantIdentifierResolver() != null ) {
				tenantIdentifier = sessionFactory.getCurrentTenantIdentifierResolver().resolveCurrentTenantIdentifier();
			}
			this.jdbcTimeZone = sessionFactory.getSessionFactoryOptions().getJdbcTimeZone();

			listeners = sessionFactory.getSessionFactoryOptions().getBaselineSessionEventsListenerBuilder().buildBaselineList();
			queryParametersValidationEnabled = sessionFactory.getSessionFactoryOptions().isQueryParametersValidationEnabled();
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SessionCreationOptions

		@Override
		public SessionOwner getSessionOwner() {
			return sessionOwner;
		}

		@Override
		public ExceptionMapper getExceptionMapper() {
			if ( sessionOwner != null ) {
				return sessionOwner.getExceptionMapper();
			}
			else {
				return sessionOwnerBehavior == SessionOwnerBehavior.LEGACY_JPA
						? ExceptionMapperLegacyJpaImpl.INSTANCE
						: null;
			}
		}

		@Override
		public AfterCompletionAction getAfterCompletionAction() {
			if ( sessionOwner != null ) {
				return sessionOwner.getAfterCompletionAction();
			}
			return sessionOwnerBehavior == SessionOwnerBehavior.LEGACY_JPA
					? AfterCompletionActionLegacyJpaImpl.INSTANCE
					: null;
		}

		@Override
		public ManagedFlushChecker getManagedFlushChecker() {
			if ( sessionOwner != null ) {
				return sessionOwner.getManagedFlushChecker();
			}
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

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SessionBuilder

		@Override
		public Session openSession() {
			log.tracef( "Opening Hibernate Session.  tenant=%s, owner=%s", tenantIdentifier, sessionOwner );
			final SessionImpl session = new SessionImpl( sessionFactory, this );

			for ( SessionEventListener listener : listeners ) {
				session.getEventListenerManager().addListener( listener );
			}

			return session;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T owner(SessionOwner sessionOwner) {
			this.sessionOwner = sessionOwner;
			return (T) this;
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
			Collections.addAll( this.listeners, listeners );
			return (T) this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T clearEventListeners() {
			listeners.clear();
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

			if ( sessionFactory.getCurrentTenantIdentifierResolver() != null ) {
				tenantIdentifier = sessionFactory.getCurrentTenantIdentifierResolver().resolveCurrentTenantIdentifier();
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
}
