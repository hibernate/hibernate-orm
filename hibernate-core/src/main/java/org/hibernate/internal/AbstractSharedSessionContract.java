/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FindOption;
import jakarta.persistence.StatementReference;
import jakarta.persistence.Timeout;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.sql.EntityMapping;
import jakarta.persistence.sql.ResultSetMapping;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.SessionException;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.Timeouts;
import org.hibernate.Transaction;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.enhance.spi.interceptor.SessionAssociationMarkers;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.creation.internal.ParentSessionObserver;
import org.hibernate.engine.creation.internal.SessionCreationOptions;
import org.hibernate.engine.creation.internal.SessionCreationOptionsAdaptor;
import org.hibernate.engine.creation.internal.SharedSessionBuilderImpl;
import org.hibernate.engine.creation.internal.SharedSessionCreationOptions;
import org.hibernate.engine.creation.internal.SharedStatelessSessionBuilderImpl;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.internal.find.FindByKeyOperation;
import org.hibernate.internal.find.Helper;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.jpa.internal.LegacySpecHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.UnknownNamedQueryException;
import org.hibernate.query.criteria.CriteriaDefinition;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.internal.MutationQueryImpl;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.specification.internal.SelectionSpecificationImpl;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.resource.jdbc.internal.EmptyStatementInspector;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.TransactionRequiredForJoinException;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.Boolean.TRUE;
import static org.hibernate.CacheMode.fromJpaModes;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.internal.SessionLogging.SESSION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.hibernate.query.sqm.internal.SqmUtil.verifyIsSelectStatement;

/**
 * Base class for implementations of {@link org.hibernate.SharedSessionContract} and
 * {@link SharedSessionContractImplementor}. Intended for concrete implementations of
 * {@link org.hibernate.Session} and {@link org.hibernate.StatelessSession}.
 * <p>
 * @implNote A {@code Session} or JPA {@code EntityManager} is a single-threaded object,
 *           which may not be called concurrently. Therefore, this implementation defines
 *           access to a number of instance state values in a manner that is not exactly
 *           thread-safe.
 *
 * @see SessionImpl
 * @see StatelessSessionImpl
 *
 * @author Steve Ebersole
 */
abstract class AbstractSharedSessionContract implements SharedSessionContractImplementor {
	private transient SessionFactoryImpl factory;
	private transient SessionFactoryOptions factoryOptions;
	private transient JdbcServices jdbcServices;

	// Defaults to null, meaning the properties
	// are the default properties of the factory.
	private Map<String, Object> properties;

	private UUID sessionIdentifier;
	private Object sessionToken;

	private transient JdbcConnectionAccess jdbcConnectionAccess;
	private transient JdbcSessionContext jdbcSessionContext;
	private transient JdbcCoordinator jdbcCoordinator;

	private transient Transaction currentHibernateTransaction;
	private transient TransactionCoordinator transactionCoordinator;
	private transient CacheTransactionSynchronization cacheTransactionSynchronization;

	private final boolean autoJoinTransactions;
	private final boolean isTransactionCoordinatorShared;
	private final PhysicalConnectionHandlingMode connectionHandlingMode;

	private final Interceptor interceptor;

	private final Object tenantIdentifier;
	private final boolean readOnly;
	private final TimeZone jdbcTimeZone;

	// mutable state
	private CacheMode cacheMode;
	private Integer jdbcBatchSize;

	private boolean criteriaCopyTreeEnabled;
	private boolean criteriaPlanCacheEnabled;

	private boolean nativeJdbcParametersIgnored;

	protected boolean closed;
	protected boolean waitingForAutoClose;

	// transient & non-final for serialization purposes
	private transient SessionEventListenerManager sessionEventsManager;
	private transient EntityNameResolver entityNameResolver;

	//Lazily initialized
	private transient ExceptionConverter exceptionConverter;
	private transient SessionAssociationMarkers sessionAssociationMarkers;

	AbstractSharedSessionContract(SessionFactoryImpl factory, SessionCreationOptions options) {
		this.factory = factory;

		factoryOptions = factory.getSessionFactoryOptions();
		jdbcServices = factory.getJdbcServices();
		cacheTransactionSynchronization =
				factory.getCache().getRegionFactory()
						.createTransactionContext( this );

		tenantIdentifier = getTenantId( factoryOptions, options );
		readOnly = options.isReadOnly();
		cacheMode = options.getInitialCacheMode();
		interceptor = interpret( options.getInterceptor() );
		jdbcTimeZone = options.getJdbcTimeZone();

		sessionEventsManager = createSessionEventsManager( factoryOptions, options );
		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );

		criteriaCopyTreeEnabled = factoryOptions.isCriteriaCopyTreeEnabled();
		criteriaPlanCacheEnabled = factoryOptions.isCriteriaPlanCacheEnabled();
		nativeJdbcParametersIgnored = factoryOptions.getNativeJdbcParametersIgnored();

		final var statementInspector = interpret( options.getStatementInspector() );

		if ( options instanceof SharedSessionCreationOptions sharedOptions
				&& sharedOptions.isTransactionCoordinatorShared() ) {
			isTransactionCoordinatorShared = true;
			if ( options.getConnection() != null ) {
				throw new SessionException( "Cannot simultaneously share transaction context and specify connection" );
			}
			transactionCoordinator = sharedOptions.getTransactionCoordinator();
			jdbcCoordinator = sharedOptions.getJdbcCoordinator();
			// todo : "wrap" the transaction to no-op commit/rollback attempts?
			currentHibernateTransaction = sharedOptions.getTransaction();
			connectionHandlingMode = jdbcCoordinator.getLogicalConnection().getConnectionHandlingMode();
			autoJoinTransactions = false;
			jdbcSessionContext = createJdbcSessionContext( statementInspector );
			logInconsistentOptions( sharedOptions );
			addSharedSessionTransactionObserver( transactionCoordinator );
			sharedOptions.registerParentSessionObserver( new ParentSessionObserver() {
				@Override
				public void onParentFlush() {
					propagateFlush();
				}

				@Override
				public void onParentClose() {
					propagateClose();
				}
			} );
		}
		else {
			isTransactionCoordinatorShared = false;
			autoJoinTransactions = options.shouldAutoJoinTransactions();
			connectionHandlingMode = options.getPhysicalConnectionHandlingMode();
			jdbcSessionContext = createJdbcSessionContext( statementInspector );
			// This must happen *after* the JdbcSessionContext was initialized,
			// because some calls retrieve this context indirectly via Session getters.
			jdbcCoordinator = createJdbcCoordinator( options );
			transactionCoordinator = factory.transactionCoordinatorBuilder
					.buildTransactionCoordinator( jdbcCoordinator, this );
		}
	}

	final SessionFactoryOptions getSessionFactoryOptions() {
		return factoryOptions;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityHandler (JPA)

	@Override
	public EntityManagerFactory getEntityManagerFactory() {
		checkOpen();
		return getFactory();
	}

	@Override
	public Metamodel getMetamodel() {
		checkOpen();
		return factory.getJpaMetamodel();
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return getCacheMode().getJpaStoreMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getCacheMode().getJpaRetrieveMode();
	}

	@Override
	public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		setCacheMode( fromJpaModes( getCacheMode().getJpaRetrieveMode(), cacheStoreMode ) );
	}

	@Override
	public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		setCacheMode( fromJpaModes( cacheRetrieveMode, getCacheMode().getJpaStoreMode() ) );
	}

	protected Map<String, Object> properties() {
		return properties;
	}

	@Override
	public Map<String, Object> getProperties() {
		// EntityManager Javadoc implies that the
		// returned map should be a mutable copy,
		// not an unmodifiable map. There's no
		// good reason to cache the initial
		// properties, since we have to copy them
		// each time this method is called.
		return properties == null
				? getInitialProperties()
				: new HashMap<>( properties );
	}

	protected abstract Map<String, Object> getInitialProperties();

	@Override
	public void setProperty(String propertyName, Object value) {
		checkOpen();
		if ( propertyName == null ) {
			SESSION_LOGGER.nullPropertyKey();
		}
		else if ( !(value instanceof Serializable) ) {
			SESSION_LOGGER.nonSerializableProperty( propertyName );
		}
		else {
			// store property for future reference
			if ( properties == null ) {
				properties = getInitialProperties();
			}
			properties.put( propertyName, value );
			// now actually update the setting if
			// it's one that affects this Session
			interpretProperty( propertyName, value );
		}
	}

	protected abstract void interpretProperty(String propertyName, Object value);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// loading

	/**
	 * Create a FindByKeyOperation to be used for {@code find()} and {@code get()} handling.
	 *
	 * @param entityDescriptor The entity type being loaded.
	 * @param options Any options to apply.
	 */
	protected abstract <T> FindByKeyOperation<T> byKey(EntityPersister entityDescriptor, FindOption... options);

	/**
	 * Create a FindByKeyOperation to be used for {@code find()} and {@code get()} handling.
	 *
	 * @param entityDescriptor The entity type being loaded.
	 * @param graphSemantic Semantic of the supplied {@code rootGraph}.
	 * @param rootGraph The EntityGraph to apply to the load.
	 * @param options Any options to apply.
	 */
	protected abstract <T> FindByKeyOperation<T> byKey(
			EntityPersister entityDescriptor,
			GraphSemantic graphSemantic,
			RootGraphImplementor<?> rootGraph,
			FindOption... options);

	/**
	 * JPA requires {@code find()} and {@code get()} to throw {@linkplain IllegalArgumentException}
	 * if the specified {@code entityClass} is not actually a known entity class.
	 * However, Hibernate internals throw the much more meaningful {@linkplain UnknownEntityTypeException}.
	 * Here we catch {@linkplain UnknownEntityTypeException} and convert it to {@linkplain IllegalArgumentException}.
	 */
	protected EntityPersister requireEntityPersisterForLoad(Class<?> entityClass) {
		try {
			return requireEntityPersister( entityClass );
		}
		catch (UnknownEntityTypeException e) {
			var iae = new IllegalArgumentException( e.getMessage() );
			iae.addSuppressed( e );
			throw iae;
		}
	}

	/**
	 * Corollary to {@linkplain #requireEntityPersisterForLoad(Class)}, but for
	 * dynamic models.
	 *
	 * @implNote Even though JPA does not define support for dynamic models, we want to be consistent here.
	 */
	protected EntityPersister requireEntityPersisterForLoad(String entityName) {
		try {
			return requireEntityPersister( entityName );
		}
		catch (UnknownEntityTypeException e) {
			var iae = new IllegalArgumentException( e.getMessage() );
			iae.addSuppressed( e );
			throw iae;
		}
	}

	protected static EntityNotFoundException notFound(String entityName, Object key) {
		return new EntityNotFoundException(
				String.format(
						Locale.ROOT,
						"No entity of type `%s` existed for key `%s`",
						entityName,
						key
				)
		);
	}

	@Override
	public <T> T find(Class<T> entityClass, Object id) {
		checkOpen();

		final var persister = requireEntityPersisterForLoad( entityClass.getName() );
		//noinspection unchecked
		return (T) byKey( persister ).performFind( id );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object key, FindOption... findOptions) {
		checkOpen();

		final var persister = requireEntityPersisterForLoad( entityClass.getName() );
		//noinspection unchecked
		return (T) byKey( persister, findOptions ).performFind( key );
	}

	@Override
	public <T> T find(EntityGraph<T> entityGraph, Object key, FindOption... findOptions) {
		checkOpen();

		final var graph = (RootGraphImplementor<T>) entityGraph;
		final var type = graph.getGraphedType();

		final EntityPersister entityDescriptor = switch ( type.getRepresentationMode() ) {
			case POJO -> requireEntityPersisterForLoad( type.getJavaType() );
			case MAP -> requireEntityPersisterForLoad( type.getTypeName() );
		};

		//noinspection unchecked
		return (T) byKey( entityDescriptor, GraphSemantic.LOAD, graph, findOptions ).performFind( key );
	}

	@Override
	public Object find(String entityName, Object key, FindOption... findOptions) {
		checkOpen();
		var entityDescriptor = requireEntityPersisterForLoad( entityName );
		return byKey( entityDescriptor, findOptions ).performFind( key );
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id) {
		var result = find( entityClass, id );
		if ( result == null ) {
			throw notFound( entityClass.getName(), id );
		}
		return result;
	}

	@Override
	public <T> T get(Class<T> entityClass, Object key, FindOption... findOptions) {
		var result = find( entityClass, key, findOptions );
		if ( result == null ) {
			throw notFound( entityClass.getName(), key );
		}
		return result;
	}

	@Override
	public <T> T get(EntityGraph<T> entityGraph, Object key, FindOption... findOptions) {
		var result = find( entityGraph, key, findOptions );
		if ( result == null ) {
			final var graph = (RootGraphImplementor<T>) entityGraph;
			throw notFound( graph.getGraphedType().getTypeName(), key );
		}
		return result;
	}

	@Override
	public Object get(String entityName, Object key, FindOption... findOptions) {
		var result = find( entityName, key, findOptions );
		if ( result == null ) {
			throw notFound( entityName, key );
		}
		return result;
	}

	@Override
	public <T> List<T> getMultiple(Class<T> entityClass, List<?> keys, FindOption... findOptions) {
		var results = findMultiple( entityClass, keys, findOptions );
		Helper.verifyGetMultipleResults( results, entityClass.getName(), keys, findOptions );
		return results;
	}

	@Override
	public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> keys, FindOption... findOptions) {
		var results = findMultiple( entityGraph, keys, findOptions );
		Helper.verifyGetMultipleResults( results, ( (RootGraph<T>) entityGraph ).getGraphedType().getTypeName(), keys );
		return results;
	}

	@Override
	public <C> void runWithConnection(ConnectionConsumer<C> action) {
		doWork( connection -> {
			try {
				//noinspection unchecked
				action.accept( (C) connection );
			}
			catch (SQLException e) {
				throw getJdbcServices().getSqlExceptionHelper().convert( e, "Error in runWithConnection" );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );
	}

	@Override
	public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
		return doReturningWork( connection -> {
			try {
				//noinspection unchecked
				return function.apply( (C) connection );
			}
			catch (SQLException e) {
				throw getJdbcServices().getSqlExceptionHelper().convert( e, "Error in callWithConnection" );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );
	}

	@Override
	public <T> SelectionQueryImplementor<T> createQuery(CriteriaSelect<T> selectQuery) {
		return createQuery( (CriteriaQuery<T>) selectQuery );
	}

	@Override
	public <T> SelectionQueryImplementor<T> createQuery(String hql, EntityGraph<T> entityGraph) {
		// by definition this HQL must be a selection query
		return createSelectionQuery( hql, entityGraph );
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sql, ResultSetMapping<T> resultSetMapping) {
		final var query = new NativeQueryImpl<>( sql, resultSetMapping, this );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic native SQL query" );
		}
		if ( resultSetMapping instanceof EntityMapping<?> entityMapping ) {
			var lockMode = LockMode.fromJpaLockMode( entityMapping.lockMode() );
			if ( lockMode.greaterThan( LockMode.READ ) ) {
				query.setHibernateLockMode( lockMode );
			}
		}
		return query;
	}

	@Override
	public <T> EntityGraph<T> getEntityGraph(Class<T> entityClass, String name) {
		//noinspection unchecked
		return (EntityGraph<T>) factory.getNamedEntityGraphs( entityClass ).get( name );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shared builders

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return new SharedStatelessSessionBuilderImpl( this ) {
			@Override
			protected StatelessSessionImplementor createStatelessSession() {
				return new StatelessSessionImpl( factory,
						new SessionCreationOptionsAdaptor( factory, this,
								AbstractSharedSessionContract.this ) );
			}
		};
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new SharedSessionBuilderImpl( this ) {
			@Override
			protected SessionImplementor createSession() {
				return new SessionImpl( factory, this );
			}
		};
	}

	protected final void setUpMultitenancy(SessionFactoryImplementor factory, LoadQueryInfluencers loadQueryInfluencers) {
		if ( factory.getDefinedFilterNames().contains( TenantIdBinder.FILTER_NAME ) ) {
			final Object tenantIdentifier = getTenantIdentifierValue();
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "SessionFactory configured for multi-tenancy, but no tenant identifier specified" );
			}
			else {
				final var resolver = factory.getCurrentTenantIdentifierResolver();
				if ( resolver==null || !resolver.isRoot( tenantIdentifier ) ) {
					// turn on the filter, unless this is the "root" tenant with access to all partitions
					loadQueryInfluencers.enableFilter( TenantIdBinder.FILTER_NAME )
							.setParameter( TenantIdBinder.PARAMETER_NAME, tenantIdentifier );
				}
			}
		}
	}

	private void logInconsistentOptions(SharedSessionCreationOptions sharedOptions) {
		// TODO: these should probable be exceptions!
		if ( sharedOptions.shouldAutoJoinTransactions() ) {
			SESSION_LOGGER.invalidAutoJoinTransactionsWithSharedConnection();
		}
		if ( sharedOptions.getPhysicalConnectionHandlingMode() != connectionHandlingMode ) {
			SESSION_LOGGER.invalidPhysicalConnectionHandlingModeWithSharedConnection();
		}
	}

	private JdbcCoordinatorImpl createJdbcCoordinator(SessionCreationOptions options) {
		return new JdbcCoordinatorImpl( options.getConnection(), this, getJdbcServices() );
	}

	private JdbcSessionContextImpl createJdbcSessionContext(StatementInspector statementInspector) {
		return new JdbcSessionContextImpl(
				factory,
				statementInspector,
				connectionHandlingMode,
				getJdbcServices(),
				factory.batchBuilder,
				// TODO: this object is deprecated and should be removed
				new JdbcEventHandler(
						factory.getStatistics(),
						sessionEventsManager,
						// since jdbcCoordinator not yet initialized here
						() -> jdbcCoordinator
				)
		);
	}

	private static Object getTenantId( SessionFactoryOptions factoryOptions, SessionCreationOptions options ) {
		final Object tenantIdentifier = options.getTenantIdentifierValue();
		if ( factoryOptions.isMultiTenancyEnabled() && tenantIdentifier == null ) {
			throw new HibernateException( "SessionFactory configured for multi-tenancy, but no tenant identifier specified" );
		}
		return tenantIdentifier;
	}

	boolean isReadOnly() {
		return readOnly;
	}

	void checkNotReadOnly() {
		if ( isReadOnly() ) {
			throw new IllegalStateException( "Session is in read-only mode" );
		}
	}

	private static SessionEventListenerManager createSessionEventsManager(
			SessionFactoryOptions factoryOptions, SessionCreationOptions options) {
		final var customListeners = options.getCustomSessionEventListeners();
		return customListeners == null
				? new SessionEventListenerManagerImpl( factoryOptions.buildSessionEventListeners() )
				: new SessionEventListenerManagerImpl( customListeners );
	}

	/**
	 * Override the implementation provided on SharedSessionContractImplementor
	 * which is not very efficient: this method is hot in Hibernate Reactive, and could
	 * be hot in some ORM contexts as well.
	 */
	@Override
	public Integer getConfiguredJdbcBatchSize() {
		final Integer sessionJdbcBatchSize = jdbcBatchSize;
		return sessionJdbcBatchSize == null
				? factoryOptions.getJdbcBatchSize()
				: sessionJdbcBatchSize;
	}

	void afterTransactionBeginEvents() {
		getInterceptor().afterTransactionBegin( getTransactionIfAccessible() );
	}

	void beforeTransactionCompletionEvents() {
		SESSION_LOGGER.beforeTransactionCompletion();
		try {
			getInterceptor().beforeTransactionCompletion( getTransactionIfAccessible() );
		}
		catch (Throwable t) {
			SESSION_LOGGER.exceptionInBeforeTransactionCompletionInterceptor( t );
		}
	}

	void afterTransactionCompletionEvents(boolean successful) {
		SESSION_LOGGER.afterTransactionCompletion( successful, false );
		getEventListenerManager().transactionCompletion(successful);

		final var statistics = getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.endTransaction(successful);
		}

		try {
			getInterceptor().afterTransactionCompletion( getTransactionIfAccessible() );
		}
		catch (Throwable t) {
			SESSION_LOGGER.exceptionInAfterTransactionCompletionInterceptor( t );
		}
	}

	private Transaction getTransactionIfAccessible() {
		// We do not want an exception to be thrown if the transaction
		// is not accessible. If the transaction is not accessible,
		// then return null.
		return isTransactionAccessible() ? accessTransaction() : null;
	}

	protected void addSharedSessionTransactionObserver(TransactionCoordinator transactionCoordinator) {
	}

	protected void removeSharedSessionTransactionObserver(TransactionCoordinator transactionCoordinator) {
		transactionCoordinator.invalidate();
	}

	protected void prepareForAutoClose() {
		waitingForAutoClose = true;
		closed = true;
		// For non-shared transaction coordinators, we have to add the observer
		if ( !isTransactionCoordinatorShared ) {
			addSharedSessionTransactionObserver( transactionCoordinator );
		}
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return autoJoinTransactions;
	}

	private Interceptor interpret(Interceptor interceptor) {
		return interceptor == null ? EmptyInterceptor.INSTANCE : interceptor;
	}

	private StatementInspector interpret(StatementInspector statementInspector) {
		return statementInspector == null ? EmptyStatementInspector.INSTANCE : statementInspector;
	}

	@Override
	public final SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public final Interceptor getInterceptor() {
		return interceptor;
	}

	@Override
	public final JdbcCoordinator getJdbcCoordinator() {
		return jdbcCoordinator;
	}

	@Override
	public final TransactionCoordinator getTransactionCoordinator() {
		return transactionCoordinator;
	}

	@Override
	public final JdbcSessionContext getJdbcSessionContext() {
		return jdbcSessionContext;
	}

	public final EntityNameResolver getEntityNameResolver() {
		return entityNameResolver;
	}

	@Override
	public final SessionEventListenerManager getEventListenerManager() {
		return sessionEventsManager;
	}

	@Override
	public final UUID getSessionIdentifier() {
		if ( sessionIdentifier == null ) {
			//Lazily initialized: otherwise all the UUID generations will cause significant amount of contention.
			sessionIdentifier = StandardRandomStrategy.INSTANCE.generateUUID( null );
		}
		return sessionIdentifier;
	}

	@Override
	public final Object getSessionToken() {
		if ( sessionToken == null ) {
			sessionToken = new Object();
		}
		return sessionToken;
	}

	@Override
	public String getTenantIdentifier() {
		if ( tenantIdentifier == null ) {
			return null;
		}
		return factory.getTenantIdentifierJavaType().toString( tenantIdentifier );
	}

	@Override
	public final Object getTenantIdentifierValue() {
		return tenantIdentifier;
	}

	@Override
	public boolean isOpen() {
		return !isClosed();
	}

	@Override
	public boolean isClosed() {
		return closed || factory.isClosed();
	}

	@Override
	public void close() {
		if ( !closed || waitingForAutoClose ) {
			try {
				delayedAfterCompletion();
			}
			catch ( HibernateException e ) {
				if ( factoryOptions.isJpaBootstrap() ) {
					throw getExceptionConverter().convert( e );
				}
				else {
					throw e;
				}
			}

			if ( sessionEventsManager != null ) {
				sessionEventsManager.end();
			}

			if ( transactionCoordinator != null ) {
				removeSharedSessionTransactionObserver( transactionCoordinator );
			}

			if ( sessionAssociationMarkers != null ) {
				sessionAssociationMarkers.sessionClosed();
				sessionAssociationMarkers = null;
			}

			try {
				if ( !isTransactionCoordinatorShared ) {
					checkBeforeClosingJdbcCoordinator();
					jdbcCoordinator.close();
				}
			}
			finally {
				setClosed();
			}
		}
	}

	protected void setClosed() {
		closed = true;
		waitingForAutoClose = false;
		cleanupOnClose();
	}

	protected abstract void propagateFlush();

	protected abstract void propagateClose();

	protected void checkBeforeClosingJdbcCoordinator() {
	}

	protected void cleanupOnClose() {
		// nothing to do in base impl, here for SessionImpl hook
	}

	@Override
	public boolean isOpenOrWaitingForAutoClose() {
		return !closed && factory.isOpen()
			|| waitingForAutoClose;
	}

	@Override
	public void checkOpen(boolean markForRollbackIfClosed) {
		if ( isClosed() ) {
			if ( markForRollbackIfClosed && transactionCoordinator.isTransactionActive() ) {
				markForRollbackOnly();
			}
			throw new IllegalStateException( "Session/EntityManager is closed" );
		}
	}

	protected void checksBeforeQueryCreation() {
		checkOpen();
		checkTransactionSynchStatus();
	}

	@Override
	public void prepareForQueryExecution(boolean requiresTxn) {
		checksBeforeQueryCreation();
		if ( requiresTxn && !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "No active transaction" );
		}
	}

	@Override
	public Timeout getDefaultTimeout() {
		var timeoutInMilliseconds = getHintedQueryTimeout();
		if ( timeoutInMilliseconds == null ) {
			return null;
		}
		return Timeouts.interpretMilliSeconds( timeoutInMilliseconds );
	}

	protected Integer getHintedQueryTimeout() {
		return LegacySpecHelper.getInteger(
				HINT_SPEC_QUERY_TIMEOUT,
				HINT_JAVAEE_QUERY_TIMEOUT,
				this::getSessionProperty
		);
	}

	@Override
	public Timeout getDefaultLockTimeout() {
		var timeoutInMilliseconds = getHintedLockTimeout();
		if ( timeoutInMilliseconds == null ) {
			return null;
		}
		return Timeouts.interpretMilliSeconds( timeoutInMilliseconds );
	}

	protected Integer getHintedLockTimeout() {
		return LegacySpecHelper.getInteger(
				HINT_SPEC_LOCK_TIMEOUT,
				HINT_JAVAEE_LOCK_TIMEOUT,
				this::getSessionProperty,
				// treat WAIT_FOREVER the same as null
				value -> !Integer.valueOf( WAIT_FOREVER_MILLI ).equals( value )
		);
	}

	protected Object getSessionProperty(String propertyName) {
		return properties() == null
				? getDefaultProperties().get( propertyName )
				: properties().get( propertyName );
	}

	protected Map<String, Object> getDefaultProperties() {
		return getSessionFactoryOptions().getDefaultSessionProperties();
	}

	@Override
	public void markForRollbackOnly() {
		try {
			accessTransaction().markRollbackOnly();
		}
		catch (Exception ignore) {
		}
	}

	@Override
	public boolean isTransactionInProgress() {
		return isOpenOrWaitingForAutoClose()
			&& transactionCoordinator.isTransactionActive();
	}

	@Override
	public void checkTransactionNeededForUpdateOperation(String exceptionMessage) {
		if ( !factoryOptions.isAllowOutOfTransactionUpdateOperations()
				&& !isTransactionInProgress() ) {
			throw new TransactionRequiredException( exceptionMessage );
		}
	}

	private boolean isTransactionAccessible() {
		// JPA requires that access not be provided to the transaction when using JTA.
		// This is overridden when SessionFactoryOptions isJtaTransactionAccessEnabled() is true.
		return factoryOptions.isJtaTransactionAccessEnabled() // defaults to false in JPA bootstrap
			|| !factoryOptions.getJpaCompliance().isJpaTransactionComplianceEnabled()
			|| !factory.transactionCoordinatorBuilder.isJta();
	}

	@Override
	public Transaction getTransaction() throws HibernateException {
		if ( !isTransactionAccessible(  ) ) {
			throw new IllegalStateException(
					"Transaction is not accessible when using JTA with JPA-compliant transaction access enabled"
			);
		}
		return accessTransaction();
	}

	@Override
	public Transaction accessTransaction() {
		if ( currentHibernateTransaction == null ) {
			currentHibernateTransaction = new TransactionImpl( getTransactionCoordinator(), this );
		}
		if ( isOpenOrWaitingForAutoClose() ) {
			transactionCoordinator.pulse();
		}
		return currentHibernateTransaction;
	}

	@Override
	public void startTransactionBoundary() {
		cacheTransactionSynchronization.transactionJoined();
	}

	@Override
	public void beforeTransactionCompletion() {
		cacheTransactionSynchronization.transactionCompleting();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		cacheTransactionSynchronization.transactionCompleted( successful );
	}

	@Override
	public final CacheTransactionSynchronization getCacheTransactionSynchronization() {
		return cacheTransactionSynchronization;
	}

	@Override
	public Transaction beginTransaction() {
		checkOpen();
		final Transaction transaction = accessTransaction();
		// only need to begin a transaction if it was not
		// already active (this is the documented semantics)
		if ( !transaction.isActive() ) {
			transaction.begin();
		}
		return transaction;
	}

	protected void checkTransactionSynchStatus() {
		pulseTransactionCoordinator();
		delayedAfterCompletion();
	}

	protected void pulseTransactionCoordinator() {
		if ( !isClosed() ) {
			try {
				transactionCoordinator.pulse();
			}
			catch (HibernateException e) {
				throw e;
			}
			catch (RuntimeException e) {
				throw new HibernateException( "Exception pulsing TransactionCoordinator", e );
			}
		}
	}

	@Override
	public void joinTransaction() {
		checkOpen();
		try {
			// For a non-JTA TransactionCoordinator, this just logs a WARNing
			transactionCoordinator.explicitJoin();
		}
		catch ( TransactionRequiredForJoinException e ) {
			throw new TransactionRequiredException( e.getMessage() );
		}
		catch ( HibernateException he ) {
			throw getExceptionConverter().convert( he );
		}
	}

	@Override
	public boolean isJoinedToTransaction() {
		checkOpen();
		return transactionCoordinator.isJoined();
	}

	protected void delayedAfterCompletion() {
		if ( transactionCoordinator instanceof JtaTransactionCoordinatorImpl jtaTransactionCoordinator ) {
			jtaTransactionCoordinator.getSynchronizationCallbackCoordinator().processAnyDelayedAfterCompletion();
		}
	}

	@Override
	public Transaction getCurrentTransaction() {
		return currentHibernateTransaction;
	}

	@Override
	public boolean isConnected() {
		pulseTransactionCoordinator();
		return jdbcCoordinator.getLogicalConnection().isOpen();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		// See class-level Javadoc for a discussion of the concurrent-access safety of this method
		if ( jdbcConnectionAccess == null ) {
			if ( !factoryOptions.isMultiTenancyEnabled() ) {
				// we might still be using schema-based multitenancy
				jdbcConnectionAccess = new NonContextualJdbcConnectionAccess(
						readOnly,
						sessionEventsManager,
						factory.connectionProvider,
						this
				);
			}
			else {
				// we're using datasource-based multitenancy
				jdbcConnectionAccess = new ContextualJdbcConnectionAccess(
						tenantIdentifier,
						readOnly,
						sessionEventsManager,
						factory.multiTenantConnectionProvider,
						this
				);
			}
		}
		return jdbcConnectionAccess;
	}

	private boolean manageReadOnly() {
		return !factory.connectionProviderHandlesConnectionReadOnly();
	}

	private boolean manageSchema() {
		return !factory.connectionProviderHandlesConnectionSchema();
	}

	private boolean useSchemaBasedMultiTenancy() {
		return tenantIdentifier != null
			&& getSessionFactoryOptions().getTenantSchemaMapper() != null;
	}

	private String tenantSchema() {
		final var mapper = getSessionFactoryOptions().getTenantSchemaMapper();
		return mapper == null ? null : normalizeSchemaName( mapper.schemaName( tenantIdentifier ) );
	}

	private String normalizeSchemaName(String schemaName) {
		return jdbcServices.getJdbcEnvironment().getIdentifierHelper()
				.toMetaDataSchemaName( toIdentifier( schemaName ) );
	}

	private transient String initialSchema;

	@Override
	public void afterObtainConnection(Connection connection) throws SQLException {
		if ( useSchemaBasedMultiTenancy() && manageSchema() ) {
			initialSchema = connection.getSchema();
			connection.setSchema( tenantSchema() );
		}
		if ( readOnly && manageReadOnly() ) {
			connection.setReadOnly( true );
		}
	}

	@Override
	public void beforeReleaseConnection(Connection connection) throws SQLException {
		if ( useSchemaBasedMultiTenancy() && manageSchema() ) {
			connection.setSchema( initialSchema );
		}
		if ( readOnly && manageReadOnly() ) {
			connection.setReadOnly( false );
		}
	}

	@Override
	public EntityKey generateEntityKey(Object id, EntityPersister persister) {
		return new EntityKey( id, persister );
	}

	@Override
	public final SessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	public boolean useStreamForLobBinding() {
		return getDialect().useInputStreamToInsertBlob();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return factoryOptions.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public boolean useLanguageTagForLocale() {
		return factoryOptions.isPreferLocaleLanguageTagEnabled();
	}

	@Override
	public LobCreator getLobCreator() {
		return jdbcServices.getLobCreator( this );
	}

	@Override
	public Dialect getDialect() {
		return jdbcServices.getJdbcEnvironment().getDialect();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return factory.getTypeConfiguration();
	}

	@Override
	public <T> T execute(final Callback<T> callback) {
		return getJdbcCoordinator().coordinateWork(
				(workExecutor, connection) -> {
					try {
						return callback.executeOnConnection( connection );
					}
					catch (SQLException e) {
						throw getExceptionConverter().convert(
								e,
								"Error creating contextual LOB : " + e.getMessage()
						);
					}
				}
		);
	}

	@Override
	public TimeZone getJdbcTimeZone() {
		return jdbcTimeZone;
	}

	@Override
	public final JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public void setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	public void setCriteriaCopyTreeEnabled(boolean jpaCriteriaCopyComplianceEnabled) {
		criteriaCopyTreeEnabled = jpaCriteriaCopyComplianceEnabled;
	}

	@Override
	public boolean isCriteriaCopyTreeEnabled() {
		return criteriaCopyTreeEnabled;
	}

	@Override
	public boolean isCriteriaPlanCacheEnabled() {
		return criteriaPlanCacheEnabled;
	}

	@Override
	public void setCriteriaPlanCacheEnabled(boolean criteriaPlanCacheEnabled) {
		this.criteriaPlanCacheEnabled = criteriaPlanCacheEnabled;
	}

	@Override
	public boolean getNativeJdbcParametersIgnored() {
		return nativeJdbcParametersIgnored;
	}

	@Override
	public void setNativeJdbcParametersIgnored(boolean nativeJdbcParametersIgnored) {
		this.nativeJdbcParametersIgnored = nativeJdbcParametersIgnored;
	}

	@Override
	public void doWork(final Work work) throws HibernateException {
		doWork( (workExecutor, connection) -> {
			workExecutor.executeWork( work, connection );
			return null;
		} );
	}

	@Override
	public <T> T doReturningWork(final ReturningWork<T> work) throws HibernateException {
		return doWork( (workExecutor, connection) -> workExecutor.executeReturningWork( work, connection ) );
	}

	private <T> T doWork(WorkExecutorVisitable<T> work) throws HibernateException {
		return getJdbcCoordinator().coordinateWork( work );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HQL

	@Override
	public <R> SelectionQueryImplementor<R> createSelectionQuery(String hql, Class<R> expectedResultType) {
		checksBeforeQueryCreation();
		try {
			final var interpretation = interpretHql( hql, expectedResultType );
			return buildHqlSelectionQuery( hql, interpretation, expectedResultType, null );
		}
		catch ( RuntimeException e ) {
			markForRollbackOnly();
			throw e;
		}
	}

	@Override
	public <T> SelectionQueryImplementor<T> createQuery(String queryString, Class<T> expectedResultType) {
		// JPA form
		try {
			return createSelectionQuery( queryString, expectedResultType );
		}
		catch (RuntimeException e) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public <R> SelectionQueryImplementor<R> createSelectionQuery(String hql, EntityGraph<R> resultGraph) {
		checksBeforeQueryCreation();
		final var resultType = resultGraph.getGraphedType().getJavaType();
		try {
			final var interpretation = interpretHql( hql, resultType );
			return buildHqlSelectionQuery( hql, interpretation, resultType, (RootGraphImplementor<R>) resultGraph );
		}
		catch ( RuntimeException e ) {
			markForRollbackOnly();
			throw e;
		}
	}

	private <R> SelectionQueryImplementor<R> buildHqlSelectionQuery(
			String hql,
			HqlInterpretation<R> hqlInterpretation,
			Class<R> expectedResultType,
			RootGraphImplementor<R> entityGraph) {
		checkSelectionQuery( hql, hqlInterpretation );
		var selectionQuery = new SelectionQueryImpl<>( hql, hqlInterpretation, expectedResultType, entityGraph, this );
		if ( expectedResultType != null ) {
			checkResultType( expectedResultType, selectionQuery );
		}
		selectionQuery.setComment( hql );
		applyQuerySettingsAndHints( selectionQuery );

		return selectionQuery;
	}

	@Override
	public MutationQuery createMutationQuery(String hql) {
		checksBeforeQueryCreation();

		final var interpretation = interpretHql( hql, null );
		if ( interpretation.getSqmStatement() instanceof SqmDmlStatement mutationAst ) {
			return buildHqlMutationQuery( hql, interpretation, mutationAst.getTarget().getJavaType() );
		}

		throw new IllegalMutationQueryException( "Query string is not a mutation", hql );
	}

	private <T> MutationQueryImplementor<T> buildHqlMutationQuery(String hql, HqlInterpretation<T> interpretation, Class<T> targetType) {
		final var mutationQuery = new MutationQueryImpl<>( hql, interpretation, targetType, this );
		mutationQuery.setComment( hql );
		applyQuerySettingsAndHints( mutationQuery );
		return mutationQuery;
	}

	@Override
	public MutationQuery createStatement(String hqlString) {
		// JPA form
		try {
			return createMutationQuery( hqlString );
		}
		catch (IllegalMutationQueryException e) {
			var iae = new IllegalArgumentException( e.getMessage() );
			iae.addSuppressed( e );
			throw iae;
		}
	}


	@Override @Deprecated @SuppressWarnings("rawtypes")
	public QueryImplementor createQuery(String hql) {
		// JPA form
		checksBeforeQueryCreation();
		try {
			final var interpretation = interpretHql( hql, null );
			if ( interpretation.getSqmStatement() instanceof SqmSelectStatement<?> ) {
				DeprecationLogger.DEPRECATION_LOGGER.debugf( "Session#createQuery(String queryString) used to create SelectionQuery" );
				return buildHqlSelectionQuery( hql, interpretation, null, null  );
			}
			else {
				final SqmDmlStatement mutationAst = (SqmDmlStatement) interpretation.getSqmStatement();
				return buildHqlMutationQuery( hql, interpretation, mutationAst.getTarget().getJavaType() );
			}
		}
		catch (IllegalQueryOperationException illegalQueryType) {
			markForRollbackOnly();
			var iae = new IllegalArgumentException( illegalQueryType.getMessage() );
			iae.addSuppressed( illegalQueryType );
			throw iae;
		}
		catch ( RuntimeException e ) {
			markForRollbackOnly();
			throw getExceptionConverter().convert( e );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Criteria

	@Override
	public <R> SelectionQueryImplementor<R> createSelectionQuery(CriteriaQuery<R> criteria) {
		checksBeforeQueryCreation();
		try {
			final SelectionQueryImplementor<R> selectionQuery;
			if ( criteria instanceof CriteriaDefinition<R> criteriaDefinition ) {
				selectionQuery = (SelectionQueryImplementor<R>) criteriaDefinition.createSelectionQuery(this);
			}
			else {
				verifyIsSelectStatement( (SqmStatement<?>) criteria, null );
				selectionQuery = new SelectionQueryImpl<>( (SqmSelectStatement<R>) criteria, criteria.getResultType(), this );
			}

			applyQuerySettingsAndHints( selectionQuery );

			return selectionQuery;
		}
		catch ( RuntimeException e ) {
			markForRollbackOnly();
			throw e;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Reference

	@Override
	public <R> SelectionQueryImplementor<R> createQuery(TypedQueryReference<R> typedQueryReference) {
		checksBeforeQueryCreation();
		try {
			final SelectionQueryImplementor<R> selectionQuery;
			if ( typedQueryReference instanceof SelectionSpecificationImpl<R> specification ) {
				final var criteria = specification.buildCriteria( getCriteriaBuilder() );
				selectionQuery = new SelectionQueryImpl<>( (SqmSelectStatement<R>) criteria, specification.getResultType(), this );
			}
			else {
				//noinspection unchecked
				final var resultType = (Class<R>) typedQueryReference.getResultType();
				final QueryImplementor<R> query = buildNamedQuery( typedQueryReference.getName(),
								memento -> memento.toSelectionQuery( this, resultType ),
								memento -> memento.toSelectionQuery( this, resultType ) );
				typedQueryReference.getHints().forEach( query::setHint );
				selectionQuery = (SelectionQueryImplementor<R>) query;
			}

			applyQuerySettingsAndHints( selectionQuery );

			return selectionQuery;
		}
		catch ( RuntimeException e ) {
			markForRollbackOnly();
			throw e;
		}
	}

	@Override
	public MutationQuery createStatement(StatementReference statementReference) {
		checksBeforeQueryCreation();
		if ( statementReference instanceof MutationSpecification<?> specification ) {
			final var criteria = specification.buildCriteria( getCriteriaBuilder() );
			return new MutationQueryImpl<>( (SqmDmlStatement<?>) criteria, this );
		}
		else {
			// this cast is fine because of all our impls of TypedQueryReference return Class<R>
			final MutationQuery query =
					buildNamedQuery( statementReference.getName(),
							memento -> memento.toMutationQuery( this ),
							memento -> memento.toMutationQuery( this ) );
			statementReference.getHints().forEach( query::setHint );
			return query;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Native Query

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sqlString, @Nullable Class<T> resultClass) {
		checksBeforeQueryCreation();
		return buildNativeQuery( sqlString, null, resultClass );
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(
			String sqlString,
			@Nullable String resultSetMappingName,
			@Nullable Class<T> resultClass) {
		checksBeforeQueryCreation();
		return buildNativeQuery(
				sqlString,
				StringHelper.isNotEmpty( resultSetMappingName )
					? getResultSetMappingMemento( resultSetMappingName )
					: null,
				resultClass
		);
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sqlString, Class<T> resultClass, String tableAlias) {
		checksBeforeQueryCreation();
		final var query = buildNativeQuery( sqlString, null, resultClass );
		if ( getMappingMetamodel().isEntityClass( resultClass ) ) {
			query.addEntity( tableAlias, resultClass, LockMode.READ );
			return query;
		}
		else {
			throw new UnknownEntityTypeException( resultClass );
		}
	}

	private <T> NativeQueryImpl<T> buildNativeQuery(
			String sql,
			@Nullable NamedResultSetMappingMemento resultSetMapping,
			@Nullable Class<T> resultClass) {
		try {
			final var query = new NativeQueryImpl<>( sql, resultSetMapping, resultClass, this );
			if ( isEmpty( query.getComment() ) ) {
				query.setComment( "dynamic native SQL query" );
			}
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch ( RuntimeException he ) {
			throw getExceptionConverter().convert( he );
		}
	}

	@Override
	public MutationQuery createNativeMutationQuery(String sqlString) {
		final var query = createNativeQuery( sqlString );
		return query.asMutationQuery();
	}


	/**
	 * @deprecated This is a JPA defined method, but Hibernate considers it deprecated.
	 * Use {@linkplain #createNativeQuery(String, Class)}  or {@linkplain #createNativeStatement(String)} instead.
	 */
	@Override @Deprecated
	public NativeQueryImplementor<?> createNativeQuery(String sqlString) {
		checksBeforeQueryCreation();
		try {
			var query = new NativeQueryImpl<>( sqlString, this );
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch ( RuntimeException he ) {
			throw getExceptionConverter().convert( he );
		}
	}

	/**
	 * @deprecated This is a JPA defined method, but Hibernate considers it deprecated.
	 * Use {@linkplain #createNativeQuery(String, String, Class)} instead.
	 */
	@Override @Deprecated
	public NativeQueryImplementor<?> createNativeQuery(String sqlString, String resultSetMappingName) {
		checksBeforeQueryCreation();
		return buildNativeQuery(
				sqlString,
				StringHelper.isNotEmpty( resultSetMappingName )
						? getResultSetMappingMemento( resultSetMappingName )
						: null,
				null
		);
	}





	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query creation utilities

	protected <T> void applyQuerySettingsAndHints(Query<T> query) {
	}

	private NamedObjectRepository namedObjectRepository() {
		return getFactory().getQueryEngine().getNamedObjectRepository();
	}

	private NamedNativeQueryMemento<?> getNativeQueryMemento(String queryName) {
		return (NamedNativeQueryMemento<?>) namedObjectRepository().getQueryMementoByName( queryName, false );
	}

	private MappingMetamodel getMappingMetamodel() {
		return getFactory().getMappingMetamodel();
	}

	// Hibernate Reactive may need to use this
	protected final EntityPersister requireEntityPersister(Class<?> entityClass) {
		return getMappingMetamodel().getEntityDescriptor( entityClass );
	}

	// Hibernate Reactive may need to use this
	protected final EntityPersister requireEntityPersister(String entityName) {
		return getMappingMetamodel().getEntityDescriptor( entityName );
	}

	// Hibernate Reactive may need to use this
	protected final CollectionPersister requireCollectionPersister(String roleName) {
		return getMappingMetamodel().getCollectionDescriptor( roleName );
	}

	protected <R> HqlInterpretation<R> interpretHql(String hql, Class<R> resultType) {
		return getFactory().getQueryEngine().interpretHql( hql, resultType );
	}

	protected static void checkSelectionQuery(String hql, HqlInterpretation<?> hqlInterpretation) {
		if ( !( hqlInterpretation.getSqmStatement() instanceof SqmSelectStatement ) ) {
			throw new IllegalSelectQueryException( "Expecting a selection query, but found '" + hql + "'", hql);
		}
	}

	protected static <R> void checkResultType(Class<R> expectedResultType, SelectionQuery<R> query) {
		final var resultType = query.getResultType();
		if ( !expectedResultType.isAssignableFrom( resultType ) ) {
			throw new QueryTypeMismatchException(
					String.format(
							Locale.ROOT,
							"Incorrect query result type: query produces '%s' but type '%s' was given",
							expectedResultType.getName(),
							resultType.getName()
					)
			);
		}
	}

	protected NamedResultSetMappingMemento getResultSetMappingMemento(String resultSetMappingName) {
		final var resultSetMappingMemento =
				namedObjectRepository().getResultSetMappingMemento( resultSetMappingName );
		if ( resultSetMappingMemento == null ) {
			throw new HibernateException( "No result set mapping with given name '" + resultSetMappingName + "'" );
		}
		return resultSetMappingMemento;
	}











	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named Query

	@Override
	public <R> SelectionQueryImplementor<R> createNamedQuery(String name, Class<R> resultClass) {
		checksBeforeQueryCreation();
		if ( resultClass == null ) {
			throw new IllegalArgumentException( "Result class is null" );
		}
		try {
			final QueryImplementor<R> query = buildNamedQuery( name,
					memento -> createSqmQueryImplementor( resultClass, memento ),
					memento -> createNativeQueryImplementor( resultClass, memento ) );
			if ( query instanceof SelectionQueryImplementor<R> selectionQuery ) {
				return selectionQuery;
			}
			else {
				// JPA implies (though is not very explicit) that this should lead
				// to an IllegalArgumentException.  Yuck, but...
				var msg = "Named query is not a selection query : " + name;
				var iae = new IllegalArgumentException( msg );
				iae.addSuppressed( new IllegalSelectQueryException( msg, query.getQueryString() ) );
				throw iae;
			}
		}
		catch (RuntimeException e) {
			throw convertNamedQueryException( e );
		}
	}





	/**
	 * @deprecated This is a JPA-defined method, but Hibernate considers it
	 * bad thus we deprecate it through our APIs.  Use {@linkplain #createNamedQuery(String, Class)}
	 * instead.
	 */
	@Override @Deprecated
	public QueryImplementor<?> createNamedQuery(String name) {
		checksBeforeQueryCreation();
		try {
			return buildNamedQuery( name,
					this::createSqmQueryImplementor,
					this::createNativeQueryImplementor );
		}
		catch (RuntimeException e) {
			throw convertNamedQueryException( e );
		}
	}

	@Override
	public <R> NativeQueryImplementor<R> createNamedQuery(String name, String resultSetMappingName) {
		final NamedNativeQueryMemento<?> nativeQueryMemento = getNativeQueryMemento( name );
		return nativeQueryMemento.toQuery( this, resultSetMappingName );
	}

	@Override
	public <R> NativeQueryImplementor<R> createNamedQuery(String name, String resultSetMappingName, Class<R> resultClass) {
		final NamedNativeQueryMemento<?> queryMemento = getNativeQueryMemento( name );
		return queryMemento.toQuery( this, resultSetMappingName );
	}

	@Override
	public <R> SelectionQuery<R> createNamedSelectionQuery(String queryName, Class<R> expectedResultType) {
		checksBeforeQueryCreation();
		return buildNamedQuery( queryName,
				memento -> memento.toSelectionQuery( this, expectedResultType ),
				memento -> memento.toSelectionQuery( this, expectedResultType ) );
	}

	@Override
	public MutationQuery createNamedStatement(String queryName) {
		checksBeforeQueryCreation();
		return buildNamedQuery( queryName,
				memento -> memento.toMutationQuery( this ),
				memento -> memento.toMutationQuery( this ) );
	}

	/**
	 * Resolves a query name to its registered {@linkplain NamedQueryMemento memento}.
	 * Based on which kind of
	 * G
	 * @param queryName
	 * @param sqmCreator
	 * @param nativeCreator
	 * @return
	 * @param <Q>
	 */
	protected <Q extends Query> Q buildNamedQuery(
			String queryName,
			Function<NamedSqmQueryMemento<?>, Q> sqmCreator,
			Function<NamedNativeQueryMemento<?>, Q> nativeCreator) {

		final var namedMemento = namedObjectRepository().getQueryMementoByName( queryName, false );

		if ( namedMemento instanceof NamedSqmQueryMemento<?> sqmMemento ) {
			return sqmCreator.apply( sqmMemento );
		}
		else if ( namedMemento instanceof NamedNativeQueryMemento<?> nativeMemento ) {
			return nativeCreator.apply( nativeMemento );
		}
		else {
			throw new IllegalStateException( "Welp, we're here" );
		}
	}

	private RuntimeException convertNamedQueryException(RuntimeException e) {
		if ( e instanceof UnknownNamedQueryException ) {
			// JPA expects this to mark the transaction for rollback only
			transactionCoordinator.getTransactionDriverControl().markRollbackOnly();
			// it also expects an IllegalArgumentException, so wrap UnknownNamedQueryException
			return new IllegalArgumentException( e.getMessage(), e );
		}
		else if ( e instanceof IllegalArgumentException ) {
			return e;
		}
		else {
			return getExceptionConverter().convert( e );
		}
	}

	protected <T> NativeQueryImplementor<T> createNativeQueryImplementor(NamedNativeQueryMemento<T> memento) {
		final var query = memento.toQuery(this );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic native SQL query" );
		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	protected <T> QueryImplementor<T> createSqmQueryImplementor(NamedSqmQueryMemento<T> memento) {
		final var query = memento.toQuery( this );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic query" );

		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	protected <T> NativeQueryImplementor<T> createNativeQueryImplementor(Class<T> resultType, NamedNativeQueryMemento<?> memento) {
		final var query = memento.toQuery(this, resultType );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic native SQL query" );
		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	protected <T> QueryImplementor<T> createSqmQueryImplementor(Class<T> resultType, NamedQueryMemento<?> memento) {
		final var query = memento.toQuery( this, resultType );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "named query" );
		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	@Override
	public MutationQuery createNamedMutationQuery(String queryName) {
		checksBeforeQueryCreation();
		var query = buildNamedQuery( queryName,
				memento -> memento.toMutationQuery( this ),
				memento -> memento.toMutationQuery( this ) );
		return query.asMutationQuery();
	}

	protected <T> NativeQueryImplementor<T> createNativeQueryImplementor(String queryName, NamedNativeQueryMemento<T> memento) {
		final var query = memento.toQuery( this );
		final Boolean isUnequivocallySelect = query.isSelectQuery();
		if ( isUnequivocallySelect == TRUE ) {
			throw new IllegalMutationQueryException(
					"Expecting named native query (" + queryName + ") to be a mutation query, but found `"
							+ memento.getSqlString() + "`"
			);
		}
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic native SQL query" );
		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	@Override
	public MutationQueryImplementor<?> createMutationQuery(CriteriaStatement<?> criteriaUpdate) {
		checkOpen();
		try {
			final var query = new MutationQueryImpl<>( (SqmDmlStatement<?>) criteriaUpdate, this );
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch (RuntimeException e) {
			markForRollbackIfJpaComplianceEnabled();
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public MutationQueryImplementor<?> createStatement(CriteriaStatement<?> criteriaStatement) {
		return createMutationQuery( criteriaStatement );
	}

	@Override
	public MutationQueryImplementor<?> createMutationQuery(@SuppressWarnings("rawtypes") JpaCriteriaInsert insert) {
		checkOpen();
		try {
			final MutationQueryImpl<?> mutationQuery = new MutationQueryImpl<>( (SqmDmlStatement<?>) insert, this );
			applyQuerySettingsAndHints( mutationQuery );
			return mutationQuery;
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public MutationQuery createNativeStatement(String sql) {
		checksBeforeQueryCreation();
		try {
			final var query = new NativeQueryImpl( sql, true, this );
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		checkOpen();

		final var memento =
				factory.getQueryEngine().getNamedObjectRepository()
						.getCallableQueryMemento( name );
		if ( memento == null ) {
			throw new IllegalArgumentException( "No named stored procedure call with given name '" + name + "'" );
		}
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = memento.makeProcedureCall( this );
//		procedureCall.setComment( "Named stored procedure call [" + name + "]" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createNamedStoredProcedureQuery(String name) {
		return getNamedProcedureCall( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// dynamic ProcedureCall support

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public <T> SelectionQueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		checkOpen();
		if ( criteriaQuery instanceof CriteriaDefinition<T> criteriaDefinition ) {
			final SelectionQueryImplementor<T> selectionQuery
					= (SelectionQueryImplementor<T>) criteriaDefinition.createSelectionQuery( this );
			applyQuerySettingsAndHints( selectionQuery );
			return selectionQuery;
		}
		else {
			try {
				final var selectStatement = (SqmSelectStatement<T>) criteriaQuery;
				if ( ! ( selectStatement.getQueryPart() instanceof SqmQueryGroup ) ) {
					final var querySpec = selectStatement.getQuerySpec();
					if ( querySpec.getSelectClause().getSelections().isEmpty() ) {
						if ( querySpec.getFromClause().getRoots().size() == 1 ) {
							querySpec.getSelectClause().setSelection( querySpec.getFromClause().getRoots().get(0) );
						}
					}
				}

				final var query = new SelectionQueryImpl<>( selectStatement, selectStatement.getResultType(), this );
				applyQuerySettingsAndHints( query );
				return query;
			}
			catch (RuntimeException e) {
				markForRollbackIfJpaComplianceEnabled();
				throw getExceptionConverter().convert( e );
			}
		}
	}

	@Override
	public <R> SelectionQueryImplementor<R> createSelectionQuery(CriteriaSelect<R> criteria) {
		return createSelectionQuery( (CriteriaQuery<R>) criteria );
	}
























	@Override
	public <T> RootGraphImplementor<T> createEntityGraph(Class<T> rootType) {
		checkOpen();
		return new RootGraphImpl<>( null, getFactory().getJpaMetamodel().entity( rootType ) );
	}

	@Override
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
		final RootGraph<?> entityGraph = createEntityGraph( graphName );
		if ( entityGraph == null ) {
			return null;
		}
		final var type = getFactory().getJpaMetamodel().managedType( rootType );
		final var graphedType = entityGraph.getGraphedType();
		if ( !Objects.equals( graphedType.getTypeName(), type.getTypeName() ) ) {
			throw new IllegalArgumentException( "Named entity graph '" + graphName
												+ "' is for type '" + graphedType.getTypeName() + "'");
		}
		@SuppressWarnings("unchecked") // this cast is sound, because we just checked
		final var graph = (RootGraph<T>) entityGraph;
		return graph;
	}


	@Override
	public RootGraphImplementor<?> createEntityGraph(String graphName) {
		checkOpen();
		final var named = getFactory().findEntityGraphByName( graphName );
		return named == null ? null : named.makeCopy( true );
	}

	@Override
	public RootGraphImplementor<?> getEntityGraph(String graphName) {
		checkOpen();
		final var named = getFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			throw new IllegalArgumentException( "No EntityGraph with given name '" + graphName + "'" );
		}
		return named;
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		checkOpen();
		return getFactory().findEntityGraphsByType( entityClass );
	}

	@Override
	public ExceptionConverter getExceptionConverter() {
		if ( exceptionConverter == null ) {
			exceptionConverter = new ExceptionConverterImpl( this );
		}
		return exceptionConverter;
	}

	@Override
	public Integer getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		this.jdbcBatchSize = jdbcBatchSize;
	}

	@Override
	public EventMonitor getEventMonitor() {
		return factory.eventMonitor;
	}

	@Override
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		checkOpen();
		return getFactory().getCriteriaBuilder();
	}

	private void markForRollbackIfJpaComplianceEnabled() {
		if ( getSessionFactoryOptions().getJpaCompliance().isJpaTransactionComplianceEnabled() ) {
			markForRollbackOnly();
		}
	}

	// filter support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Filter getEnabledFilter(String filterName) {
		pulseTransactionCoordinator();
		return getLoadQueryInfluencers().getEnabledFilter( filterName );
	}

	@Override
	public Filter enableFilter(String filterName) {
		checkOpen();
		pulseTransactionCoordinator();
		return getLoadQueryInfluencers().enableFilter( filterName );
	}

	@Override
	public void disableFilter(String filterName) {
		checkOpen();
		pulseTransactionCoordinator();
		getLoadQueryInfluencers().disableFilter( filterName );
	}

	@Override
	public FormatMapper getXmlFormatMapper() {
		return factoryOptions.getXmlFormatMapper();
	}

	@Override
	public FormatMapper getJsonFormatMapper() {
		return factoryOptions.getJsonFormatMapper();
	}

	@Override
	public SessionAssociationMarkers getSessionAssociationMarkers() {
		if ( sessionAssociationMarkers == null ) {
			sessionAssociationMarkers = new SessionAssociationMarkers( this );
		}
		return sessionAssociationMarkers;
	}

	@Serial
	private void writeObject(ObjectOutputStream oos) throws IOException {
		SESSION_LOGGER.serializingSession( getSessionIdentifier() );


		if ( !jdbcCoordinator.isReadyForSerialization() ) {
			// throw a more specific (helpful) exception message when this happens from Session,
			//		as opposed to more generic exception from jdbcCoordinator#serialize call later
			throw new IllegalStateException( "Cannot serialize " + getClass().getSimpleName() + " [" + getSessionIdentifier() + "] while connected" );
		}

		if ( isTransactionCoordinatorShared ) {
			throw new IllegalStateException( "Cannot serialize " + getClass().getSimpleName() + " [" + getSessionIdentifier() + "] as it has a shared TransactionCoordinator" );
		}

		// todo : (5.2) come back and review serialization plan...
		//		this was done quickly during initial HEM consolidation into CORE and is likely not perfect :)
		//
		//		be sure to review state fields in terms of transient modifiers

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 1 :: write non-transient state...
		oos.defaultWriteObject();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 2 :: write transient state...
		// 		-- see concurrent access discussion

		factory.serialize( oos );
		oos.writeObject( jdbcSessionContext.getStatementInspector() );
		jdbcCoordinator.serialize( oos );
	}

	@Serial
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException, SQLException {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 1 :: read back non-transient state...
		ois.defaultReadObject();
		SESSION_LOGGER.deserializingSession( getSessionIdentifier() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 2 :: read back transient state...
		//		-- see above

		factory = SessionFactoryImpl.deserialize( ois );
		factoryOptions = factory.getSessionFactoryOptions();
		jdbcServices = factory.getJdbcServices();

		//TODO: this isn't quite right, see createSessionEventsManager()
		final var baseline = factoryOptions.buildSessionEventListeners();
		sessionEventsManager = new SessionEventListenerManagerImpl( baseline );

		jdbcSessionContext = createJdbcSessionContext( (StatementInspector) ois.readObject() );
		jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, this );

		cacheTransactionSynchronization =
				factory.getCache().getRegionFactory()
						.createTransactionContext( this );
		transactionCoordinator =
				factory.transactionCoordinatorBuilder.buildTransactionCoordinator( jdbcCoordinator, this );

		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );
	}

	protected static CacheRetrieveMode determineCacheRetrieveMode(Map<String, Object> settings) {
		final CacheRetrieveMode cacheRetrieveMode =
				(CacheRetrieveMode) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
		return cacheRetrieveMode == null
				? (CacheRetrieveMode) settings.get( JAKARTA_SHARED_CACHE_RETRIEVE_MODE )
				: cacheRetrieveMode;
	}

	protected static CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		final CacheStoreMode cacheStoreMode =
				(CacheStoreMode) settings.get( JPA_SHARED_CACHE_STORE_MODE );
		return cacheStoreMode == null
				? (CacheStoreMode) settings.get( JAKARTA_SHARED_CACHE_STORE_MODE )
				: cacheStoreMode;
	}
}
