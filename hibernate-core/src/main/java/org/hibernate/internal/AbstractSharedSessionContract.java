/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import org.hibernate.CacheMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.StatementObserver;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.spi.ChangelogSupplier;
import org.hibernate.audit.spi.AuditWorkQueue;
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
import org.hibernate.dialect.rowsecurity.RowLevelSecurity;
import org.hibernate.engine.creation.internal.ParentSessionObserver;
import org.hibernate.engine.creation.internal.SessionCreationOptions;
import org.hibernate.engine.creation.internal.SharedSessionBuilderImpl;
import org.hibernate.engine.creation.internal.SharedSessionCreationOptions;
import org.hibernate.engine.creation.internal.SharedStatelessSessionBuilderImpl;
import org.hibernate.engine.creation.internal.options.SharedStatefulOptions;
import org.hibernate.engine.creation.internal.options.SharedStatelessOptions;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.extension.spi.Extension;
import org.hibernate.engine.extension.spi.ExtensionIntegrationContext;
import org.hibernate.engine.extension.spi.ExtensionIntegrationService;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.TemporalCollectionKey;
import org.hibernate.engine.spi.TemporalEntityKey;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.internal.MutationOrSelectionQueryImpl;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.internal.find.FindByKeyOperation;
import org.hibernate.internal.find.Helper;
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
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.named.spi.NamedObjectRepository;
import org.hibernate.query.named.spi.NamedQueryMemento;
import org.hibernate.query.named.spi.NamedResultSetMappingMemento;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.specification.internal.MutationSpecificationImpl;
import org.hibernate.query.specification.internal.SelectionSpecificationImpl;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutationQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sql.internal.NativeMutationOrSelectionQueryImpl;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sqm.internal.AugmentedTypedQueryReference;
import org.hibernate.query.sqm.tree.spi.SqmDmlStatement;
import org.hibernate.query.sqm.tree.spi.SqmStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
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
import java.util.function.Supplier;

import static org.hibernate.CacheMode.fromJpaModes;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.CacheSettings.JAKARTA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.CacheSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.dialect.rowsecurity.RowLevelSecurity.TenantIdentifierSource.DATABASE_USER;
import static org.hibernate.internal.SessionLogging.SESSION_LOGGER;
import static org.hibernate.internal.util.ArgumentsHelper.bindReferenceArguments;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;
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
abstract class AbstractSharedSessionContract
		implements SharedSessionContractImplementor, ExtensionIntegrationContext {

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

	private transient ChangesetIdentifierSupplier<?> changesetIdSupplier;

	// mutable state
	private CacheMode cacheMode;
	private Integer jdbcBatchSize;

	private transient Object currentChangesetId;
	private transient Object currentChangesetContext;

	private boolean criteriaCopyTreeEnabled;
	private boolean criteriaPlanCacheEnabled;

	private boolean nativeJdbcParametersIgnored;

	private final boolean rowLevelSecurityEnabled;

	protected boolean closed;
	protected boolean waitingForAutoClose;
	private transient int sessionUseProhibitedDepth;

	// transient & non-final for serialization purposes
	private transient SessionEventListenerManager sessionEventsManager;
	private transient EntityNameResolver entityNameResolver;

	//Lazily initialized
	private transient ExceptionConverter exceptionConverter;
	private transient SessionAssociationMarkers sessionAssociationMarkers;

	private transient final Map<Class<?>, Object> extensions;

	AbstractSharedSessionContract(SessionFactoryImpl factory, SessionCreationOptions options) {
		this.factory = factory;

		factoryOptions = factory.getSessionFactoryOptions();
		jdbcServices = factory.getJdbcServices();
		cacheTransactionSynchronization =
				factory.getCache().getRegionFactory()
						.createTransactionContext( this );

		tenantIdentifier = getTenantId( factoryOptions, options );
		readOnly = options.isReadOnly();
		jdbcBatchSize = options.getJdbcBatchSize();
		cacheMode = options.getInitialCacheMode();
		interceptor = interpret( options.resolveInterceptor( factory ) );
		jdbcTimeZone = options.getJdbcTimeZone();

		sessionEventsManager = createSessionEventsManager( factoryOptions, options );
		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor, this );

		criteriaCopyTreeEnabled = factoryOptions.isCriteriaCopyTreeEnabled();
		criteriaPlanCacheEnabled = factoryOptions.isCriteriaPlanCacheEnabled();
		nativeJdbcParametersIgnored = factoryOptions.getNativeJdbcParametersIgnored();

		final var statementInspector = interpret( options.getStatementInspector() );

		changesetIdSupplier = initializeChangesetIdSupplier( factory );

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
			jdbcSessionContext = createJdbcSessionContext( options.getStatementObserver(), statementInspector );
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
			jdbcSessionContext = createJdbcSessionContext( options.getStatementObserver(), statementInspector );
			// This must happen *after* the JdbcSessionContext was initialized,
			// because some calls retrieve this context indirectly via Session getters.
			jdbcCoordinator = createJdbcCoordinator( options );
			transactionCoordinator = factory.transactionCoordinatorBuilder
					.buildTransactionCoordinator( jdbcCoordinator, this );
		}

		extensions = new HashMap<>();
		for ( var integration : factory.getServiceRegistry()
				.requireService( ExtensionIntegrationService.class )
				.extensionIntegrations() ) {
			extensions.put( integration.getExtensionType(),
					integration.createExtension( this ) );
		}

		rowLevelSecurityEnabled = isRowLevelSecurityEnabled();
	}

	private static ChangesetIdentifierSupplier<?> initializeChangesetIdSupplier(SessionFactoryImplementor factory) {
		final var changesetCoordinator = factory.getChangesetCoordinator();
		return changesetCoordinator.useServerTimestamp( factory.getJdbcServices().getDialect() )
				? null
				: changesetCoordinator.getIdentifierSupplier();
	}

	final SessionFactoryOptions getSessionFactoryOptions() {
		return factoryOptions;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityHandler (JPA)

	@Override
	@Nonnull
	public EntityManagerFactory getEntityManagerFactory() {
		checkOpen();
		return getFactory();
	}

	@Override
	@Nonnull
	public Metamodel getMetamodel() {
		checkOpen();
		return factory.getJpaMetamodel();
	}

	@Override
	@Nonnull
	public CacheStoreMode getCacheStoreMode() {
		return getCacheMode().getJpaStoreMode();
	}

	@Override
	@Nonnull
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getCacheMode().getJpaRetrieveMode();
	}

	@Override
	public void setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		setCacheMode( fromJpaModes( getCacheMode().getJpaRetrieveMode(), cacheStoreMode ) );
	}

	@Override
	public void setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		setCacheMode( fromJpaModes( cacheRetrieveMode, getCacheMode().getJpaStoreMode() ) );
	}

	protected final Map<String, Object> properties() {
		return properties;
	}

	@Override
	@Nonnull
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
	public void setProperty(@Nonnull String propertyName, @Nullable Object value) {
		checkOpen();
		//noinspection ConstantValue
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
	 * @param entityClass
	 * @param entityDescriptor The entity type being loaded.
	 * @param options Any options to apply.
	 */
	protected abstract <T> FindByKeyOperation<T> byKey(
			@Nullable Class<T> entityClass,
			EntityPersister entityDescriptor,
			FindOption... options);

	/**
	 * Create a FindByKeyOperation to be used for {@code find()} and {@code get()} handling.
	 *
	 * @param entityClass
	 * @param entityDescriptor The entity type being loaded.
	 * @param graphSemantic Semantic of the supplied {@code rootGraph}.
	 * @param rootGraph The EntityGraph to apply to the load.
	 * @param options Any options to apply.
	 */
	protected abstract <T> FindByKeyOperation<T> byKey(
			Class<T> entityClass,
			EntityPersister entityDescriptor,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
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
	@Nullable
	public <T> T find(@Nonnull Class<T> entityClass, @Nonnull Object id) {
		checkOpen();

		final var persister = requireEntityPersisterForLoad( entityClass.getName() );
		return byKey( entityClass, persister ).performFind( id );
	}

	@Override
	@Nullable
	public <T> T find(@Nonnull Class<T> entityClass, @Nonnull Object key, @Nullable FindOption... findOptions) {
		checkOpen();

		final var persister = requireEntityPersisterForLoad( entityClass.getName() );
		return byKey( entityClass, persister, findOptions ).performFind( key );
	}

	@Override
	public <T> T find(@Nonnull EntityGraph<T> entityGraph, @Nonnull Object key, @Nullable FindOption... findOptions) {
		checkOpen();

		final var graph = (RootGraphImplementor<T>) entityGraph;
		final var type = graph.getGraphedType();
		final var entityDescriptor = switch ( type.getRepresentationMode() ) {
			case POJO -> requireEntityPersisterForLoad( type.getJavaType() );
			case MAP -> requireEntityPersisterForLoad( type.getTypeName() );
		};
		return byKey( type.getJavaType(), entityDescriptor, GraphSemantic.LOAD, graph, findOptions )
				.performFind( key );
	}

	@Override
	public Object find(@Nonnull String entityName, @Nonnull Object key, @Nullable FindOption... findOptions) {
		checkOpen();
		final var entityDescriptor = requireEntityPersisterForLoad( entityName );
		return byKey( null, entityDescriptor, findOptions ).performFind( key );
	}

	@Override
	@Nonnull
	public <T> T get(@Nonnull Class<T> entityClass, @Nonnull Object id) {
		final var result = find( entityClass, id );
		if ( result == null ) {
			throw notFound( entityClass.getName(), id );
		}
		return result;
	}

	@Override
	@Nonnull
	public <T> T get(@Nonnull Class<T> entityClass, @Nonnull Object key, @Nullable FindOption... findOptions) {
		final var result = find( entityClass, key, findOptions );
		if ( result == null ) {
			throw notFound( entityClass.getName(), key );
		}
		return result;
	}

	@Override
	@Nonnull
	public <T> T get(@Nonnull EntityGraph<T> entityGraph, @Nonnull Object key, @Nullable FindOption... findOptions) {
		final var result = find( entityGraph, key, findOptions );
		if ( result == null ) {
			final var graph = (RootGraphImplementor<T>) entityGraph;
			throw notFound( graph.getGraphedType().getTypeName(), key );
		}
		return result;
	}

	@Override
	@Nonnull
	public Object get(@Nonnull String entityName, @Nonnull Object key, @Nullable FindOption... findOptions) {
		final var result = find( entityName, key, findOptions );
		if ( result == null ) {
			throw notFound( entityName, key );
		}
		return result;
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(
			@Nonnull Class<T> entityClass,
			@Nonnull List<?> keys,
			@Nullable FindOption... findOptions) {
		final var results = findMultiple( entityClass, keys, findOptions );
		Helper.verifyGetMultipleResults( results, entityClass.getName(), keys, findOptions );
		return results;
	}

	@Override
	@Nonnull
	public <T> List<T> getMultiple(
			@Nonnull EntityGraph<T> entityGraph,
			@Nonnull List<?> keys,
			@Nullable FindOption... findOptions) {
		final var results = findMultiple( entityGraph, keys, findOptions );
		Helper.verifyGetMultipleResults( results, ( (RootGraph<T>) entityGraph ).getGraphedType().getTypeName(), keys );
		return results;
	}

	@Override
	@Nonnull
	public <E> List<E> findMultiple(
			@Nonnull Class<E> entityType,
			@Nonnull List<?> keys,
			@Nullable FindOption... options) {
		final var loadQueryInfluencers = getLoadQueryInfluencers();
		final var entityPersister = requireEntityPersisterForLoad( entityType );
		final var effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
		final var graph = effectiveEntityGraph.getGraph();
		//TODO: should we be checking the graph type upfront?
		@SuppressWarnings("unchecked")
		final var castGraph = (RootGraphImplementor<E>) graph;
		return findMultiple(
				entityPersister,
				effectiveEntityGraph.getSemantic(),
				castGraph,
				keys,
				options
		);
	}

	@Override
	@Nonnull
	public <E> List<E> findMultiple(
			@Nonnull EntityGraph<E> entityGraph,
			@Nonnull List<?> keys,
			@Nullable FindOption... options) {
		final var rootGraph = (RootGraphImplementor<E>) entityGraph;
		final var type = rootGraph.getGraphedType();
		final var entityDescriptor = switch ( type.getRepresentationMode() ) {
			case POJO -> requireEntityPersisterForLoad( type.getJavaType() );
			case MAP -> requireEntityPersisterForLoad( type.getTypeName() );
		};
		return findMultiple( entityDescriptor, GraphSemantic.LOAD, rootGraph, keys, options );
	}

	abstract <E> List<E> findMultiple(
			@Nonnull EntityPersister entityDescriptor,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<E> rootGraph,
			@Nonnull List<?> keys,
			@Nullable FindOption... options);

	@Override
	public <C> void runWithConnection(@Nonnull ConnectionConsumer<C> action) {
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
	public <C, T> T callWithConnection(@Nonnull ConnectionFunction<C, T> function) {
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
	@Nonnull
	public <T> SelectionQueryImplementor<T> createQuery(@Nonnull String hql, @Nonnull EntityGraph<T> entityGraph) {
		// by definition this HQL must be a selection query
		return createSelectionQuery( hql, entityGraph );
	}

	@Override
	@Nonnull
	public <T> NativeQueryImplementor<T> createNativeQuery(@Nonnull String sql, @Nonnull ResultSetMapping<T> mapping) {
		checksBeforeQueryCreation();
		final var query = new NativeQueryImpl<>( sql, mapping, this );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic native SQL query" );
		}
		if ( mapping instanceof EntityMapping<?> entityMapping ) {
			final var lockMode = LockMode.fromJpaLockMode( entityMapping.lockMode() );
			if ( lockMode.greaterThan( LockMode.READ ) ) {
				query.setHibernateLockMode( lockMode );
			}
		}
		return query;
	}

	@Override
	@Nonnull
	public <T> RootGraph<T> getEntityGraph(@Nonnull Class<T> entityClass, @Nonnull String name) {
		return castEntityGraph( entityClass, name, getEntityGraph( name ) );
	}

	private <T> RootGraph<T> castEntityGraph(Class<T> entityClass, String name, RootGraph<?> entityGraph) {
		final var type = getFactory().getJpaMetamodel().managedType( entityClass );
		final var graphedType = entityGraph.getGraphedType();
		if ( !Objects.equals( graphedType.getTypeName(), type.getTypeName() ) ) {
			throw new IllegalArgumentException( "Named entity graph '" + name
					+ "' is for type '" + graphedType.getTypeName() + "'" );
		}
		@SuppressWarnings("unchecked") // this cast is sound, because we just checked
		final var graph = (RootGraph<T>) entityGraph;
		return graph;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Shared builders

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		checkSessionReentrancy();
		return new SharedStatelessSessionBuilderImpl( this ) {
			@Override
			protected StatelessSessionImplementor createStatelessSession(SharedStatelessOptions options) {
				return new StatelessSessionImpl( factory, options );
			}
		};
	}

	@Override
	@Nonnull
	public SharedSessionBuilder sessionWithOptions() {
		checkSessionReentrancy();
		return new SharedSessionBuilderImpl( this ) {
			@Override
			protected SessionImplementor createSession(SharedStatefulOptions options) {
				return new SessionImpl( factory, options );
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

	private JdbcSessionContextImpl createJdbcSessionContext(
			StatementObserver statementObserver,
			StatementInspector statementInspector) {
		return new JdbcSessionContextImpl(
				factory,
				statementObserver,
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
		final var transaction = getTransactionIfAccessible();
		runInterceptorCallback( () -> interceptor.afterTransactionBegin( transaction ) );
	}

	void beforeTransactionCompletionEvents() {
		SESSION_LOGGER.beforeTransactionCompletion();
		try {
			final var transaction = getTransactionIfAccessible();
			runInterceptorCallback( () -> interceptor.beforeTransactionCompletion( transaction ) );
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
			final var transaction = getTransactionIfAccessible();
			runInterceptorCallback( () -> interceptor.afterTransactionCompletion( transaction ) );
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
	@Nonnull
	public final SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public final Interceptor getInterceptor() {
		return interceptor;
	}

	@Override
	@Nonnull
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
	@Nonnull
	public final SessionEventListenerManager getEventListenerManager() {
		return sessionEventsManager;
	}

	@Override
	@Nonnull
	public final UUID getSessionIdentifier() {
		if ( sessionIdentifier == null ) {
			//Lazily initialized: otherwise all the UUID generations will cause significant amount of contention.
			sessionIdentifier = StandardRandomStrategy.INSTANCE.generateUUID( null );
		}
		return sessionIdentifier;
	}

	@Override
	@Nonnull
	public SharedSessionContractImplementor getSession() {
		return this;
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
		return tenantIdentifier == null
				? null
				: factory.getTenantIdentifierJavaType().toString( tenantIdentifier );
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
		checkSessionReentrancy();
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
					//noinspection resource
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
		checkSessionReentrancy();
		if ( isClosed() ) {
			if ( markForRollbackIfClosed && transactionCoordinator.isTransactionActive() ) {
				markForRollbackOnly();
			}
			throw new IllegalStateException( "Session/EntityManager is closed" );
		}
	}

	private void startSessionUseProhibited() {
		sessionUseProhibitedDepth++;
	}

	private void finishSessionUseProhibited() {
		sessionUseProhibitedDepth--;
	}

	protected void checkSessionReentrancy() {
		if ( sessionUseProhibitedDepth > 0 ) {
			throw new IllegalStateException( "Session method called from entity lifecycle callback or Interceptor method" );
		}
	}

	protected void checksBeforeQueryCreation() {
		checkOpen();
		checkTransactionSyncStatus();
	}

	@Override
	public void prepareForQueryExecution(boolean requiresTxn) {
		checksBeforeQueryCreation();
		if ( requiresTxn && !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "No active transaction" );
		}
	}

	@Override
	@Nullable
	public Timeout getDefaultTimeout() {
		final var timeoutInMilliseconds = getHintedQueryTimeout();
		return timeoutInMilliseconds != null
				? Timeouts.interpretMilliSeconds( timeoutInMilliseconds )
				: null;
	}

	protected Integer getHintedQueryTimeout() {
		return LegacySpecHelper.getInteger(
				HINT_SPEC_QUERY_TIMEOUT,
				HINT_JAVAEE_QUERY_TIMEOUT,
				this::getSessionProperty
		);
	}

	@Override
	@Nullable
	public Timeout getDefaultLockTimeout() {
		final var timeoutInMilliseconds = getHintedLockTimeout();
		return timeoutInMilliseconds != null
				? Timeouts.interpretMilliSeconds( timeoutInMilliseconds )
				: null;
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
		return properties == null
				? getDefaultProperties().get( propertyName )
				: properties.get( propertyName );
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
	public Object getCurrentChangesetIdentifier() {
		if ( currentChangesetId != null ) {
			return currentChangesetId;
		}
		else if ( isTransactionInProgress() ) {
			initializeCurrentChangesetIdentifier();
			return currentChangesetId;
		}
		else {
			return generateCurrentChangesetIdentifier();
		}
	}

	private Object generateCurrentChangesetIdentifier() {
		if ( changesetIdSupplier == null ) {
			return null;
		}
		if ( changesetIdSupplier instanceof ChangelogSupplier<?> changelogSupplier ) {
			final var changesetContext = changelogSupplier.generateContext( this );
			currentChangesetContext = changesetContext;
			changelogSupplier.registerLegacyContext( this, changesetContext );
			return changesetContext.changesetId();
		}
		return changesetIdSupplier.generateIdentifier( this );
	}

	@Override
	public @Nullable Object getCurrentChangesetContext() {
		if ( currentChangesetContext != null ) {
			return currentChangesetContext;
		}
		getCurrentChangesetIdentifier();
		return currentChangesetContext;
	}

	@Override
	public void afterTransactionBegin() {
		setUpRowLevelSecurity();
	}

	private RowLevelSecurity getRowLevelSecurity() {
		return getJdbcServices().getDialect().getRowLevelSecurity();
	}

	private void setUpRowLevelSecurity() {
		if ( rowLevelSecurityEnabled ) {
			setUpRowLevelSecurity( getJdbcCoordinator().getLogicalConnection().getPhysicalConnection() );
		}
	}

	private void setUpRowLevelSecurity(Connection connection) {
		if ( rowLevelSecurityEnabled ) {
			final Object tenantIdentifier = getTenantIdentifierValue();
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "Row-level security enabled, but no tenant identifier specified" );
			}

			if ( !useDatabaseUserForRowLevelSecurity() ) {
				final var resolver = factory.getCurrentTenantIdentifierResolver();
				final boolean root = resolver != null && resolver.isRoot( tenantIdentifier );
				try {
					getRowLevelSecurity()
							.setTenantIdentifier( connection, getTenantIdentifier(), root );
				}
				catch (SQLException e) {
					throw getJdbcServices().getSqlExceptionHelper()
							.convert( e, "Unable to set row-level security tenant identifier" );
				}
			}
		}
	}

	private boolean useDatabaseUserForRowLevelSecurity() {
		return getSessionFactoryOptions().getTenantCredentialsMapper() != null
			&& getRowLevelSecurity().supportsTenantIdentifierSource( DATABASE_USER );
	}

	//TODO: move this to SessionFactoryOptions
	private boolean isRowLevelSecurityEnabled() {
		return factory.getDefinedFilterNames().contains( TenantIdBinder.FILTER_NAME )
			&& getBoolean( MULTI_TENANT_RLS_ENABLED, getSettings(), true )
			&& getRowLevelSecurity().supportsRowLevelSecurity();
	}

	@Nonnull
	private Map<String, Object> getSettings() {
		return factory.getServiceRegistry()
				.requireService( ConfigurationService.class )
				.getSettings();
	}

	protected void initializeCurrentChangesetIdentifier() {
		currentChangesetId = generateCurrentChangesetIdentifier();
	}

	protected void clearTransactionStartInstant() {
		currentChangesetId = null;
		currentChangesetContext = null;
	}

	@Override
	public void checkTransactionNeededForUpdateOperation(@Nonnull String exceptionMessage) {
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
	@Nonnull
	public Transaction getTransaction() throws HibernateException {
		if ( !isTransactionAccessible() ) {
			throw new IllegalStateException(
					"Transaction is not accessible when using JTA with JPA-compliant transaction access enabled"
			);
		}
		return accessTransaction();
	}

	@Override
	@Nonnull
	public Transaction accessTransaction() {
		checkSessionReentrancy();
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
		clearTransactionStartInstant();
		cacheTransactionSynchronization.transactionCompleted( successful );
	}

	@Override
	public final CacheTransactionSynchronization getCacheTransactionSynchronization() {
		return cacheTransactionSynchronization;
	}

	@Override
	@Nonnull
	public Transaction beginTransaction() {
		checkOpen();
		final var transaction = accessTransaction();
		// only need to begin a transaction if it was not
		// already active (this is the documented semantics)
		if ( !transaction.isActive() ) {
			transaction.begin();
		}
		return transaction;
	}

	protected void checkTransactionSyncStatus() {
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
	@Nullable
	public Transaction getCurrentTransaction() {
		return currentHibernateTransaction;
	}

	@Override
	public boolean isConnected() {
		checkSessionReentrancy();
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
		setUpRowLevelSecurity( connection );
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

	private AuditWorkQueue auditWorkQueue;

	@Override
	public AuditWorkQueue getAuditWorkQueue() {
		if ( auditWorkQueue == null ) {
			auditWorkQueue = new AuditWorkQueue();
		}
		return auditWorkQueue;
	}

	@Override
	@Nonnull
	public EntityKey generateEntityKey(@Nonnull Object id, @Nonnull EntityPersister persister) {
		final Object temporalId = getLoadQueryInfluencers().getTemporalIdentifier();
		return temporalId != null && temporalId != AuditLog.ALL_CHANGESETS
				? new TemporalEntityKey( id, persister, temporalId )
				: new EntityKey( id, persister );
	}

	@Override
	@Nonnull
	public CollectionKey generateCollectionKey(@Nonnull CollectionPersister persister, @Nonnull Object key) {
		final Object temporalId = getLoadQueryInfluencers().getTemporalIdentifier();
		return temporalId != null && temporalId != AuditLog.ALL_CHANGESETS
				? new TemporalCollectionKey( persister, key, temporalId )
				: new CollectionKey( persister, key );
	}

	@Override
	@Nonnull
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
	@Nonnull
	public Dialect getDialect() {
		return jdbcServices.getJdbcEnvironment().getDialect();
	}

	@Override
	@Nonnull
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
	@Nonnull
	public final JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	@Nonnull
	public CacheMode getCacheMode() {
		return cacheMode;
	}

	@Override
	public void setCacheMode(@Nonnull CacheMode cacheMode) {
		//noinspection ConstantValue
		if ( cacheMode == null ) {
			throw new IllegalArgumentException( "CacheMode cannot be null" );
		}
		checkSessionReentrancy();
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
	public void doWork(final @Nonnull Work work) throws HibernateException {
		doWork( (workExecutor, connection) -> {
			workExecutor.executeWork( work, connection );
			return null;
		} );
	}

	@Override
	public <T> T doReturningWork(final @Nonnull ReturningWork<T> work) throws HibernateException {
		return doWork( (workExecutor, connection) -> workExecutor.executeReturningWork( work, connection ) );
	}

	private <T> T doWork(WorkExecutorVisitable<T> work) throws HibernateException {
		checkSessionReentrancy();
		return getJdbcCoordinator().coordinateWork( work );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HQL

	@Override
	@Nonnull
	public <R> SelectionQueryImplementor<R> createSelectionQuery(@Nonnull String hql, @Nonnull Class<R> expectedResultType) {
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
	@Nonnull
	public <T> SelectionQueryImplementor<T> createQuery(@Nonnull String queryString, @Nonnull Class<T> expectedResultType) {
		// JPA form
		try {
			return createSelectionQuery( queryString, expectedResultType );
		}
		catch (IllegalSelectQueryException e) {
			// JPA requires IllegalArgumentException
			var iae = new IllegalArgumentException( e.getMessage() );
			iae.addSuppressed( e );
			throw iae;
		}
		catch (RuntimeException e) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@Nonnull
	public <R> SelectionQueryImplementor<R> createSelectionQuery(@Nonnull String hql, @Nonnull EntityGraph<R> resultGraph) {
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
		final var selectionQuery =
				new SelectionQueryImpl<>( hql, hqlInterpretation, expectedResultType, entityGraph, this );
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
		return buildHqlMutationQuery( hql, interpretHql( hql ) );
	}

	private <T> MutationQueryImplementor<T> buildHqlMutationQuery(String hql, HqlInterpretation<T> interpretation) {
		if ( interpretation.getSqmStatement() instanceof SqmDmlStatement<T> mutationAst ) {
			return buildHqlMutationQuery( hql, interpretation, mutationAst.getTarget().getJavaType() );
		}
		else {
			throw new IllegalMutationQueryException( "Query string is not a mutation", hql );
		}
	}

	private <T> MutationQueryImplementor<T> buildHqlMutationQuery(String hql, HqlInterpretation<T> interpretation, Class<T> targetType) {
		final var mutationQuery = new MutationQueryImpl<>( hql, interpretation, targetType, this );
		mutationQuery.setComment( hql );
		applyQuerySettingsAndHints( mutationQuery );
		return mutationQuery;
	}

	@Override
	@Nonnull
	public MutationQuery createStatement(@Nonnull String hqlString) {
		// JPA form
		try {
			return createMutationQuery( hqlString );
		}
		catch (IllegalMutationQueryException e) {
			markForRollbackOnly();
			// JPA requires IllegalArgumentException
			var iae = new IllegalArgumentException( e.getMessage() );
			iae.addSuppressed( e );
			throw iae;
		}
		catch (RuntimeException e) {
			markForRollbackOnly();
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@Nonnull
	public MutationOrSelectionQuery createQuery(@Nonnull String hql) {
		// JPA form
		checksBeforeQueryCreation();
		try {
			final var interpretation = interpretHql( hql );
			final var delegate =
					interpretation.getSqmStatement() instanceof SqmSelectStatement<?>
							? buildHqlSelectionQuery( hql, interpretation, null, null )
							: buildHqlMutationQuery( hql, interpretation );
			return MutationOrSelectionQueryImpl.from( delegate );
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
	@Nonnull
	public <R> SelectionQueryImplementor<R> createSelectionQuery(@Nonnull CriteriaQuery<R> criteria) {
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
	@Nonnull
	public <R> SelectionQueryImplementor<R> createQuery(@Nonnull TypedQueryReference<R> typedQueryReference) {
		checksBeforeQueryCreation();
		try {
			final SelectionQueryImplementor<R> selectionQuery;
			if ( typedQueryReference instanceof SelectionSpecificationImpl<R> specification ) {
				selectionQuery = specification.createQuery( this );
			}
			else if ( typedQueryReference instanceof AugmentedTypedQueryReference<R> augmentedReference ) {
				final var criteriaQuery = augmentedReference.getCriteriaQuery();
				final var sqmMemento = augmentedReference.getSqmMemento();
				final var query = sqmMemento == null
						? new SelectionQueryImpl<>( criteriaQuery, criteriaQuery.getResultType(), this )
						: new SelectionQueryImpl<>( sqmMemento, criteriaQuery, criteriaQuery.getResultType(), this );
				bindReferenceArguments( query, augmentedReference );
				augmentedReference.getHints().forEach( query::setHint );

				final String entityGraphName = augmentedReference.getEntityGraphName();
				if ( !isEmpty( entityGraphName ) ) {
					query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraphName );
				}
				augmentedReference.getOptions().forEach( query::addOption );
				selectionQuery = query;
			}
			else {
				final var resultType = (Class<R>) typedQueryReference.getResultType();
				final var query = buildNamedQuery( typedQueryReference.getName(),
								memento -> memento.toSelectionQuery( this, resultType ),
								memento -> memento.toSelectionQuery( this, resultType ) );
				bindReferenceArguments( query, typedQueryReference );
				typedQueryReference.getHints().forEach( query::setHint );
				selectionQuery = query;

				final String entityGraphName = typedQueryReference.getEntityGraphName();
				if ( !isEmpty( entityGraphName ) ) {
					selectionQuery.setHint( HINT_SPEC_LOAD_GRAPH, entityGraphName );
				}
				typedQueryReference.getOptions().forEach( selectionQuery::addOption );
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
	@Nonnull
	public MutationQuery createStatement(@Nonnull StatementReference statementReference) {
		checksBeforeQueryCreation();
		if ( statementReference instanceof MutationSpecificationImpl<?> specification ) {
			return specification.createQuery( this );
		}
		else if ( statementReference instanceof MutationSpecification<?> specification ) {
			final var criteria = specification.buildCriteria( getCriteriaBuilder() );
			return new MutationQueryImpl<>( (SqmDmlStatement<?>) criteria, this );
		}
		else {
			final var query =
					buildNamedQuery( statementReference.getName(),
							memento -> memento.toMutationQuery( this ),
							memento -> memento.toMutationQuery( this ) );
			bindReferenceArguments( query, statementReference );
			statementReference.getHints().forEach( query::setHint );
			statementReference.getOptions().forEach( query::addOption );
			return query;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Native Query
	@Override
	@Nonnull
	public <T> NativeQueryImplementor<T> createNativeQuery(@Nonnull String sqlString, @Nonnull Class<T> resultClass) {
		checksBeforeQueryCreation();
		return buildNativeQuery( sqlString, null, resultClass );
	}

	@Override
	@Nonnull
	public <T> NativeQueryImplementor<T> createNativeQuery(
			@Nonnull String sqlString,
			@Nullable String resultSetMappingName,
			@Nullable Class<T> resultClass) {
		checksBeforeQueryCreation();
		return buildNativeQuery(
				sqlString,
				isNotEmpty( resultSetMappingName )
					? getResultSetMappingMemento( resultSetMappingName )
					: null,
				resultClass
		);
	}

	@Override
	@Nonnull
	public <T> NativeQueryImplementor<T> createNativeQuery(
			@Nonnull String sqlString,
			@Nonnull Class<T> resultClass,
			@Nonnull String tableAlias) {
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
	 */@Nonnull

	@Override @Deprecated
	public NativeQueryImplementor<?> createNativeQuery(@Nonnull String sqlString) {
		checksBeforeQueryCreation();
		try {
			final var query = new NativeQueryImpl<>( sqlString, this );
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
	 */@Nonnull

	@Override @Deprecated
	public NativeQueryImplementor<?> createNativeQuery(@Nonnull String sqlString, @Nonnull String resultSetMappingName) {
		checksBeforeQueryCreation();
		return buildNativeQuery(
				sqlString,
				isNotEmpty( resultSetMappingName )
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

	protected HqlInterpretation<?> interpretHql(String hql) {
		return interpretHql( hql, null );
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
	@Nonnull
	public <R> SelectionQueryImplementor<R> createNamedQuery(@Nonnull String name, @Nonnull Class<R> resultClass) {
		checksBeforeQueryCreation();
		//noinspection ConstantValue
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

	@Override
	@Nonnull
	public MutationOrSelectionQuery createNamedQuery(@Nonnull String name) {
		checksBeforeQueryCreation();
		try {
			return buildNamedQuery( name,
					memento -> MutationOrSelectionQueryImpl.from( createSqmQueryImplementor( memento ) ),
					this::createNativeMutationOrSelectionQueryImplementor );
		}
		catch (RuntimeException e) {
			throw convertNamedQueryException( e );
		}
	}

	@Override
	@Nonnull
	public <R> NativeQueryImplementor<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName) {
		final NamedNativeQueryMemento<?> nativeQueryMemento = getNativeQueryMemento( name );
		return nativeQueryMemento.toQuery( this, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <R> NativeQueryImplementor<R> createNamedQuery(@Nonnull String name, @Nonnull String resultSetMappingName, @Nonnull Class<R> resultClass) {
		final NamedNativeQueryMemento<?> queryMemento = getNativeQueryMemento( name );
		return queryMemento.toQuery( this, resultSetMappingName );
	}

	@Override
	@Nonnull
	public <R> SelectionQuery<R> createNamedSelectionQuery(@Nonnull String queryName, @Nonnull Class<R> expectedResultType) {
		checksBeforeQueryCreation();
		return buildNamedQuery( queryName,
				memento -> memento.toSelectionQuery( this, expectedResultType ),
				memento -> memento.toSelectionQuery( this, expectedResultType ) );
	}

	@Override
	@Nonnull
	public MutationQuery createNamedStatement(@Nonnull String queryName) {
		checksBeforeQueryCreation();
		return buildNamedQuery( queryName,
				memento -> memento.toMutationQuery( this ),
				memento -> memento.toMutationQuery( this ) );
	}

	/**
	 * Resolves a query name to its registered {@linkplain NamedQueryMemento memento}.
	 */
	protected <Q extends Query<?>> Q buildNamedQuery(
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

	protected MutationOrSelectionQuery createNativeMutationOrSelectionQueryImplementor(NamedNativeQueryMemento<?> memento) {
		final var query = NativeMutationOrSelectionQueryImpl.from( memento, this );
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
	@Nonnull
	public MutationQuery createNamedMutationQuery(@Nonnull String queryName) {
		checksBeforeQueryCreation();
		var query = buildNamedQuery( queryName,
				memento -> memento.toMutationQuery( this ),
				memento -> memento.toMutationQuery( this ) );
		return query.asMutationQuery();
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<?> createMutationQuery(@Nonnull CriteriaStatement<?> criteriaUpdate) {
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
	@Nonnull
	public MutationQueryImplementor<?> createStatement(@Nonnull CriteriaStatement<?> criteriaStatement) {
		return createMutationQuery( criteriaStatement );
	}

	@Override
	@Nonnull
	public MutationQueryImplementor<?> createMutationQuery(@SuppressWarnings("rawtypes") @Nonnull JpaCriteriaInsert insert) {
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
	@Nonnull
	public MutationQuery createNativeStatement(@Nonnull String sql) {
		checksBeforeQueryCreation();
		try {
			final var query = new NativeQueryImpl<>( sql, true, this );
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@Nonnull
	public ProcedureCall getNamedProcedureCall(@Nonnull String name) {
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
	@Nonnull
	public ProcedureCall createNamedStoredProcedureQuery(@Nonnull String name) {
		return getNamedProcedureCall( name );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// dynamic ProcedureCall support

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureCall(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@Nonnull
	public ProcedureCall createStoredProcedureQuery(@Nonnull String procedureName, @Nonnull String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@Nonnull
	public <T> SelectionQueryImplementor<T> createQuery(@Nonnull CriteriaSelect<T> selectQuery) {
		return createSelectionQuery( selectQuery );
	}

	@Override
	@Nonnull
	public <T> SelectionQueryImplementor<T> createSelectionQuery(@Nonnull CriteriaSelect<T> criteriaQuery) {
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
	@Nonnull
	public <T> RootGraphImplementor<T> createEntityGraph(@Nonnull Class<T> rootType) {
		checkOpen();
		return new RootGraphImpl<>( null, getFactory().getJpaMetamodel().entity( rootType ) );
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public <T> RootGraph<T> createEntityGraph(@Nonnull Class<T> rootType, @Nonnull String graphName) {
		final var entityGraph = createEntityGraph( graphName );
		return entityGraph == null ? null : castEntityGraph( rootType, graphName, entityGraph );
	}

	@Override @Deprecated
	@Nullable
	public RootGraphImplementor<?> createEntityGraph(@Nonnull String graphName) {
		checkOpen();
		final var named = getFactory().findEntityGraphByName( graphName );
		return named == null ? null : named.makeCopy( true );
	}

	@Override
	@Nonnull
	public RootGraphImplementor<?> getEntityGraph(@Nonnull String graphName) {
		checkOpen();
		final var named = getFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			throw new IllegalArgumentException( "No EntityGraph with given name '" + graphName + "'" );
		}
		//TODO: we're using the deprecated operation
		//      here to preserve the graph name
		return named.makeCopy(true, graphName );
	}

	@Override
	@Nonnull
	public <T> List<EntityGraph<? super T>> getEntityGraphs(@Nonnull Class<T> entityClass) {
		checkOpen();
		return getFactory().findEntityGraphsByType( entityClass );
	}

	@Override
	@Nonnull
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
		checkSessionReentrancy();
		this.jdbcBatchSize = jdbcBatchSize;
	}

	@Override
	public EventMonitor getEventMonitor() {
		return factory.eventMonitor;
	}

	@Override
	@Nonnull
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
	@Nullable
	public Filter getEnabledFilter(@Nonnull String filterName) {
		checkSessionReentrancy();
		pulseTransactionCoordinator();
		return getLoadQueryInfluencers().getEnabledFilter( filterName );
	}

	@Override
	@Nonnull
	public Filter enableFilter(@Nonnull String filterName) {
		checkOpen();
		pulseTransactionCoordinator();
		return getLoadQueryInfluencers().enableFilter( filterName );
	}

	@Override
	public void disableFilter(@Nonnull String filterName) {
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
	@Nonnull
	public SessionAssociationMarkers getSessionAssociationMarkers() {
		if ( sessionAssociationMarkers == null ) {
			sessionAssociationMarkers = new SessionAssociationMarkers( this );
		}
		return sessionAssociationMarkers;
	}

	@Override
	public <E extends Extension> E getExtension(Class<E> extension) {
		return extension.cast( extensions.get( extension ) );
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
		oos.writeObject( jdbcSessionContext.getStatementObserver() );
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

		jdbcSessionContext = createJdbcSessionContext( (StatementObserver) ois.readObject(), (StatementInspector) ois.readObject() );
		jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, this );

		cacheTransactionSynchronization =
				factory.getCache().getRegionFactory()
						.createTransactionContext( this );
		transactionCoordinator =
				factory.transactionCoordinatorBuilder.buildTransactionCoordinator( jdbcCoordinator, this );

		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor, this );

		changesetIdSupplier = initializeChangesetIdSupplier( factory );
	}

	protected static CacheRetrieveMode determineCacheRetrieveMode(Map<String, Object> settings) {
		@SuppressWarnings("deprecation")
		final CacheRetrieveMode cacheRetrieveMode =
				(CacheRetrieveMode) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
		return cacheRetrieveMode == null
				? (CacheRetrieveMode) settings.get( JAKARTA_SHARED_CACHE_RETRIEVE_MODE )
				: cacheRetrieveMode;
	}

	protected static CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		@SuppressWarnings("deprecation")
		final CacheStoreMode cacheStoreMode =
				(CacheStoreMode) settings.get( JPA_SHARED_CACHE_STORE_MODE );
		return cacheStoreMode == null
				? (CacheStoreMode) settings.get( JAKARTA_SHARED_CACHE_STORE_MODE )
				: cacheStoreMode;
	}

	/**
	 * Run a Jakarta Persistence entity lifecycle callback.
	 */
	public void runEntityLifecycleCallback(@Nonnull Runnable callback) {
		startSessionUseProhibited();
		try {
			callback.run();
		}
		finally {
			finishSessionUseProhibited();
		}
	}

	/**
	 * Call a Jakarta Persistence entity lifecycle callback.
	 */
	public <T> T callEntityLifecycleCallback(@Nonnull Supplier<T> callback) {
		startSessionUseProhibited();
		try {
			return callback.get();
		}
		finally {
			finishSessionUseProhibited();
		}
	}

	/**
	 * Run a Hibernate {@link Interceptor} callback.
	 */
	public void runInterceptorCallback(@Nonnull Runnable callback) {
		startSessionUseProhibited();
		try {
			callback.run();
		}
		finally {
			finishSessionUseProhibited();
		}
	}

	/**
	 * Call a Hibernate {@link Interceptor} callback.
	 */
	public <T> T callInterceptorCallback(@Nonnull Supplier<T> callback) {
		startSessionUseProhibited();
		try {
			return callback.get();
		}
		finally {
			finishSessionUseProhibited();
		}
	}
}
