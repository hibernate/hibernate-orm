/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.Transaction;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.enhance.spi.interceptor.SessionAssociationMarkers;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.dialect.Dialect;
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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalNamedQueryOptionsException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.UnknownNamedQueryException;
import org.hibernate.query.criteria.CriteriaDefinition;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.specification.internal.MutationSpecificationImpl;
import org.hibernate.query.specification.internal.SelectionSpecificationImpl;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.internal.SqmQueryImpl;
import org.hibernate.query.sqm.internal.SqmSelectionQueryImpl;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.Boolean.TRUE;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.internal.util.StringHelper.isEmpty;
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
public abstract class AbstractSharedSessionContract implements SharedSessionContractImplementor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SessionImpl.class );

	private transient SessionFactoryImpl factory;
	private transient SessionFactoryOptions factoryOptions;
	private transient JdbcServices jdbcServices;

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

	public AbstractSharedSessionContract(SessionFactoryImpl factory, SessionCreationOptions options) {
		this.factory = factory;

		factoryOptions = factory.getSessionFactoryOptions();
		jdbcServices = factory.getJdbcServices();
		cacheTransactionSynchronization = factory.getCache().getRegionFactory().createTransactionContext( this );

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

		isTransactionCoordinatorShared = isTransactionCoordinatorShared( options );
		if ( isTransactionCoordinatorShared ) {
			final var sharedOptions = (SharedSessionCreationOptions) options;
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
		}
		else {
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

	@Override
	public SharedStatelessSessionBuilder statelessWithOptions() {
		return new SharedStatelessSessionBuilderImpl( this );
	}

	private static boolean isTransactionCoordinatorShared(SessionCreationOptions options) {
		return options instanceof SharedSessionCreationOptions sharedSessionCreationOptions
			&& sharedSessionCreationOptions.isTransactionCoordinatorShared();
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
		if ( sharedOptions.shouldAutoJoinTransactions() ) {
			LOG.debug( "Session creation specified 'autoJoinTransactions', which is invalid in conjunction " +
							"with sharing JDBC connection between sessions; ignoring" );
		}
		if ( sharedOptions.getPhysicalConnectionHandlingMode() != connectionHandlingMode ) {
			LOG.debug( "Session creation specified 'PhysicalConnectionHandlingMode' which is invalid in conjunction " +
							"with sharing JDBC connection between sessions; ignoring" );
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
		final var customListeners = options.getCustomSessionEventListener();
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
		try {
			getInterceptor().beforeTransactionCompletion( getTransactionIfAccessible() );
		}
		catch (Throwable t) {
			LOG.exceptionInBeforeTransactionCompletionInterceptor( t );
		}
	}

	void afterTransactionCompletionEvents(boolean successful) {
		getEventListenerManager().transactionCompletion(successful);

		final var statistics = getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.endTransaction(successful);
		}

		try {
			getInterceptor().afterTransactionCompletion( getTransactionIfAccessible() );
		}
		catch (Throwable t) {
			LOG.exceptionInAfterTransactionCompletionInterceptor( t );
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
				if ( shouldCloseJdbcCoordinatorOnClose( isTransactionCoordinatorShared ) ) {
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

	protected boolean shouldCloseJdbcCoordinatorOnClose(boolean isTransactionCoordinatorShared) {
		return true;
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

	private void checksBeforeQueryCreation() {
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

	protected Transaction getCurrentTransaction() {
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// dynamic HQL handling

	@Override @SuppressWarnings("rawtypes")
	public QueryImplementor createQuery(String queryString) {
		return createQuery( queryString, null );
	}

	@Override
	public SelectionQuery<?> createSelectionQuery(String hqlString) {
		return interpretAndCreateSelectionQuery( hqlString, null );
	}

	private <R> SelectionQuery<R> interpretAndCreateSelectionQuery(String hql, Class<R> resultType) {
		checksBeforeQueryCreation();
		try {
			final var interpretation = interpretHql( hql, resultType );
			checkSelectionQuery( hql, interpretation );
			return createSelectionQuery( hql, resultType, interpretation );
		}
		catch ( RuntimeException e ) {
			markForRollbackOnly();
			throw e;
		}
	}

	private <R> SelectionQuery<R> createSelectionQuery(String hql, Class<R> resultType, HqlInterpretation<R> interpretation) {
		final var query = new SqmSelectionQueryImpl<>( hql, interpretation, resultType, this );
		if ( resultType != null ) {
			checkResultType( resultType, query );
		}
		query.setComment( hql );
		applyQuerySettingsAndHints( query );
		return query;
	}

	protected <R> HqlInterpretation<R> interpretHql(String hql, Class<R> resultType) {
		return getFactory().getQueryEngine().interpretHql( hql, resultType );
	}

	protected static void checkSelectionQuery(String hql, HqlInterpretation<?> hqlInterpretation) {
		if ( !( hqlInterpretation.getSqmStatement() instanceof SqmSelectStatement ) ) {
			throw new IllegalSelectQueryException( "Expecting a selection query, but found '" + hql + "'", hql);
		}
	}

	protected static <R> void checkResultType(Class<R> expectedResultType, SqmSelectionQueryImpl<R> query) {
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

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> expectedResultType) {
		return interpretAndCreateSelectionQuery( hqlString, expectedResultType );
	}

	@Override
	public <R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph) {
		final var rootGraph = (RootGraph<R>) resultGraph;
		return interpretAndCreateSelectionQuery( hqlString, rootGraph.getGraphedType().getJavaType() )
				.setEntityGraph( resultGraph, GraphSemantic.LOAD );
	}


	@Override
	public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
		if ( criteria instanceof CriteriaDefinition<R> criteriaDefinition ) {
			return criteriaDefinition.createSelectionQuery(this);
		}
		else {
			verifyIsSelectStatement( (SqmStatement<?>) criteria, null );
			return new SqmSelectionQueryImpl<>( (SqmSelectStatement<R>) criteria, criteria.getResultType(), this );
		}
	}

	@Override
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> expectedResultType) {
		checksBeforeQueryCreation();
		try {
			final var interpretation = interpretHql( queryString, expectedResultType );
			final var query = new SqmQueryImpl<>( queryString, interpretation, expectedResultType, this );
			applyQuerySettingsAndHints( query );
			query.setComment( queryString );
			return query;
		}
		catch (RuntimeException e) {
			markForRollbackOnly();
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public <R> QueryImplementor<R> createQuery(TypedQueryReference<R> typedQueryReference) {
		checksBeforeQueryCreation();
		if ( typedQueryReference instanceof SelectionSpecificationImpl<R> specification ) {
			final var query = specification.buildCriteria( getCriteriaBuilder() );
			return new SqmQueryImpl<>( (SqmStatement<R>) query, specification.getResultType(), this );
		}
		else if ( typedQueryReference instanceof MutationSpecificationImpl<?> specification ) {
			final var query = specification.buildCriteria( getCriteriaBuilder() );
			return new SqmQueryImpl<>( (SqmStatement<R>) query, (Class<R>) specification.getResultType(), this );
		}
		else {
			@SuppressWarnings("unchecked")
			// this cast is fine because of all our impls of TypedQueryReference return Class<R>
			final var resultType = (Class<R>) typedQueryReference.getResultType();
			final var query =
					buildNamedQuery( typedQueryReference.getName(),
							memento -> createSqmQueryImplementor( resultType, memento ),
							memento -> createNativeQueryImplementor( resultType, memento ) );
			typedQueryReference.getHints().forEach( query::setHint );
			return query;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// dynamic native (SQL) query handling

	@Override
	public NativeQueryImplementor<?> createNativeQuery(String sqlString) {
		return createNativeQuery( sqlString, (Class<?>) null );
	}

	@Override
	public NativeQueryImplementor<?> createNativeQuery(String sqlString, String resultSetMappingName) {
		checksBeforeQueryCreation();
		return buildNativeQuery( sqlString, resultSetMappingName, null );
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	//note: we're doing something a bit funny here to work around
	//      the clashing signatures declared by the supertypes
	public NativeQueryImplementor createNativeQuery(String sqlString, @Nullable Class resultClass) {
		checksBeforeQueryCreation();
		return buildNativeQuery( sqlString, resultClass );
	}

	private <T> NativeQueryImpl<T> buildNativeQuery(String sql, String resultSetMappingName, Class<T> resultClass) {
		if ( isEmpty( resultSetMappingName ) ) {
			throw new IllegalArgumentException( "Result set mapping name was not specified" );
		}

		try {
			final var memento = getResultSetMappingMemento( resultSetMappingName );
			final var query = new NativeQueryImpl<>( sql, memento, resultClass, this );
			if ( isEmpty( query.getComment() ) ) {
				query.setComment( "dynamic native SQL query" );
			}
			//TODO: why no applyQuerySettingsAndHints( query ); ???
			return query;
		}
		catch ( RuntimeException he ) {
			throw getExceptionConverter().convert( he );
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

	private <T> NativeQueryImplementor<T> buildNativeQuery(String sql, @Nullable Class<T> resultClass) {
		try {
			final var query = new NativeQueryImpl<>( sql, resultClass, this );
			if ( isEmpty( query.getComment() ) ) {
				query.setComment( "dynamic native SQL query" );
			}
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch (RuntimeException he) {
			throw getExceptionConverter().convert( he );
		}
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

	private MappingMetamodel getMappingMetamodel() {
		return getFactory().getMappingMetamodel();
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sqlString, Class<T> resultClass, String tableAlias) {
		checksBeforeQueryCreation();
		final var query = buildNativeQuery( sqlString, resultClass );
		if ( getMappingMetamodel().isEntityClass( resultClass ) ) {
			query.addEntity( tableAlias, resultClass, LockMode.READ );
			return query;
		}
		else {
			throw new UnknownEntityTypeException( resultClass );
		}
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sqlString, String resultSetMappingName, Class<T> resultClass) {
		checksBeforeQueryCreation();
		return buildNativeQuery( sqlString, resultSetMappingName, resultClass );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// named query handling

	@Override @Deprecated
	public QueryImplementor<?> getNamedQuery(String queryName) {
		return createNamedQuery( queryName );
	}

	@Override
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
	public <R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass) {
		checksBeforeQueryCreation();
		if ( resultClass == null ) {
			throw new IllegalArgumentException( "Result class is null" );
		}
		try {
			return buildNamedQuery( name,
					memento -> createSqmQueryImplementor( resultClass, memento ),
					memento -> createNativeQueryImplementor( resultClass, memento ) );
		}
		catch (RuntimeException e) {
			throw convertNamedQueryException( e );
		}
	}

	@Override
	public SelectionQuery<?> createNamedSelectionQuery(String queryName) {
		return createNamedSelectionQuery( queryName, null );
	}

	@Override
	public <R> SelectionQuery<R> createNamedSelectionQuery(String queryName, Class<R> expectedResultType) {
		checksBeforeQueryCreation();
		return buildNamedQuery( queryName,
				memento -> createNamedSqmSelectionQuery( memento, expectedResultType ),
				memento -> createNamedNativeSelectionQuery( memento, expectedResultType ) );
	}

	private NamedObjectRepository namedObjectRepository() {
		return getFactory().getQueryEngine().getNamedObjectRepository();
	}

	private NamedSqmQueryMemento<?> getSqmQueryMemento(String queryName) {
		return namedObjectRepository().getSqmQueryMemento( queryName );
	}

	private NamedNativeQueryMemento<?> getNativeQueryMemento(String queryName) {
		return namedObjectRepository().getNativeQueryMemento( queryName );
	}

	private <R> SelectionQuery<R> createNamedNativeSelectionQuery(
			NamedNativeQueryMemento<?> memento,
			Class<R> expectedResultType) {
		return memento.toQuery( this, expectedResultType );
	}

	private <R> SqmSelectionQuery<R> createNamedSqmSelectionQuery(
			NamedSqmQueryMemento<?> memento,
			Class<R> expectedResultType) {
		final var selectionQuery = memento.toSelectionQuery( expectedResultType, this );
		final String comment = memento.getComment();
		selectionQuery.setComment( isEmpty( comment ) ? "Named query: " + memento.getRegistrationName() : comment );
		applyQuerySettingsAndHints( selectionQuery );
		final var lockOptions = memento.getLockOptions();
		if ( lockOptions != null ) {
			selectionQuery.getLockOptions().overlay( lockOptions );
		}
		return selectionQuery;
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

	protected void applyQuerySettingsAndHints(SelectionQuery<?> query) {
	}

	protected void applyQuerySettingsAndHints(Query<?> query) {
	}

	protected <T,Q extends CommonQueryContract> Q buildNamedQuery(
			String queryName,
			Function<NamedSqmQueryMemento<T>, Q> sqmCreator,
			Function<NamedNativeQueryMemento<T>, Q> nativeCreator) {

		// this method can be called for either a named HQL query or a named native query

		// first see if it is a named HQL query
		final var namedSqmQueryMemento = getSqmQueryMemento( queryName );
		if ( namedSqmQueryMemento != null ) {
			return sqmCreator.apply( (NamedSqmQueryMemento<T>) namedSqmQueryMemento );
		}

		// otherwise, see if it is a named native query
		final var namedNativeDescriptor = getNativeQueryMemento( queryName );
		if ( namedNativeDescriptor != null ) {
			return nativeCreator.apply( (NamedNativeQueryMemento<T>) namedNativeDescriptor );
		}

		throw new UnknownNamedQueryException( queryName );
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

	protected <T> SqmQueryImplementor<T> createSqmQueryImplementor(NamedSqmQueryMemento<T> memento) {
		final var query = memento.toQuery( this );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic query" );
		}
		applyQuerySettingsAndHints( query );
		if ( memento.getLockOptions() != null ) {
			query.setLockOptions( memento.getLockOptions() );
		}
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

	protected <T> SqmQueryImplementor<T> createSqmQueryImplementor(Class<T> resultType, NamedSqmQueryMemento<?> memento) {
		final var query = memento.toQuery( this, resultType );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic query" );
		}
		applyQuerySettingsAndHints( query );
		if ( memento.getLockOptions() != null ) {
			query.setLockOptions( memento.getLockOptions() );
		}
		return query;
	}

	@Override
	public NativeQueryImplementor<?> getNamedNativeQuery(String queryName) {
		final var namedNativeDescriptor = getNativeQueryMemento( queryName );
		if ( namedNativeDescriptor != null ) {
			return namedNativeDescriptor.toQuery( this );
		}
		else {
			throw noQueryForNameException( queryName );
		}
	}

	@Override
	public NativeQueryImplementor<?> getNamedNativeQuery(String queryName, String resultSetMapping) {
		final var namedNativeDescriptor = getNativeQueryMemento( queryName );
		if ( namedNativeDescriptor != null ) {
			return namedNativeDescriptor.toQuery( this, resultSetMapping );
		}
		else {
			throw noQueryForNameException( queryName );
		}
	}

	private RuntimeException noQueryForNameException(String queryName) {
		return getExceptionConverter()
				.convert( new IllegalArgumentException( "No query with given name '" + queryName + "'" ) );
	}

	@Override
	public MutationQuery createMutationQuery(String hqlString) {
		final var query = createQuery( hqlString );
		final var sqmStatement = ( (SqmQueryImplementor<?>) query ).getSqmStatement();
		checkMutationQuery( hqlString, sqmStatement );
		return query;
	}

	protected static void checkMutationQuery(String hqlString, SqmStatement<?> sqmStatement) {
		if ( !( sqmStatement instanceof SqmDmlStatement ) ) {
			throw new IllegalMutationQueryException( "Expecting a mutation query, but found '" + hqlString + "'" );
		}
	}

	@Override
	public MutationQuery createNativeMutationQuery(String sqlString) {
		final var query = createNativeQuery( sqlString );
		if ( query.isSelectQuery() == TRUE ) {
			throw new IllegalMutationQueryException( "Expecting a native mutation query, but found `" + sqlString + "`" );
		}
		return query;
	}

	@Override
	public MutationQuery createNamedMutationQuery(String queryName) {
		checksBeforeQueryCreation();
		return buildNamedQuery( queryName,
				memento -> createSqmQueryImplementor( queryName, memento ),
				memento -> createNativeQueryImplementor( queryName, memento ) );
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

	protected <T> SqmQueryImplementor<T> createSqmQueryImplementor(String queryName, NamedSqmQueryMemento<T> memento) {
		final var query = memento.toQuery( this );
		final var sqmStatement = query.getSqmStatement();
		if ( !( sqmStatement instanceof SqmDmlStatement ) ) {
			throw new IllegalMutationQueryException(
					"Expecting a named mutation query '" + queryName + "', but found a select statement"
			);
		}
		if ( memento.getLockOptions() != null && ! memento.getLockOptions().isEmpty() ) {
			throw new IllegalNamedQueryOptionsException(
					"Named mutation query '" + queryName + "; specified lock-options"
			);
		}
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic HQL query" );
		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmUpdateStatement<?>) updateQuery, null );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmDeleteStatement<?>) deleteQuery, null );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") JpaCriteriaInsert insert) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmStatement<?>) insert, null );
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
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final var procedureCall = new ProcedureCallImpl<>( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
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

	@Override
	public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		checkOpen();
		if ( criteriaQuery instanceof CriteriaDefinition<T> criteriaDefinition ) {
			return (QueryImplementor<T>) criteriaDefinition.createSelectionQuery(this);
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

				return createCriteriaQuery( selectStatement, criteriaQuery.getResultType() );
			}
			catch (RuntimeException e) {
				markForRollbackIfJpaComplianceEnabled();
				throw getExceptionConverter().convert( e );
			}
		}
	}

	@Override
	public QueryImplementor<?> createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate criteriaUpdate) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmUpdateStatement<?>) criteriaUpdate, null );
		}
		catch (RuntimeException e) {
			markForRollbackIfJpaComplianceEnabled();
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public QueryImplementor<?> createQuery(@SuppressWarnings("rawtypes") CriteriaDelete criteriaDelete) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmDeleteStatement<?>) criteriaDelete, null );
		}
		catch (RuntimeException e) {
			markForRollbackIfJpaComplianceEnabled();
			throw getExceptionConverter().convert( e );
		}
	}

	protected <T> QueryImplementor<T> createCriteriaQuery(SqmStatement<T> criteria, Class<T> resultType) {
		final var query = new SqmQueryImpl<>( criteria, resultType, this );
		applyQuerySettingsAndHints( query );
		return query;
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
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Serializing " + getClass().getSimpleName() + " [" );
		}


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
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Deserializing " + getClass().getSimpleName() );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 1 :: read back non-transient state...
		ois.defaultReadObject();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 2 :: read back transient state...
		//		-- see above

		factory = SessionFactoryImpl.deserialize( ois );
		factoryOptions = factory.getSessionFactoryOptions();
		jdbcServices = factory.getJdbcServices();

		//TODO: this isn't quite right, see createSessionEventsManager()
		final SessionEventListener[] baseline = factoryOptions.buildSessionEventListeners();
		sessionEventsManager = new SessionEventListenerManagerImpl( baseline );

		jdbcSessionContext = createJdbcSessionContext( (StatementInspector) ois.readObject() );
		jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, this );

		cacheTransactionSynchronization = factory.getCache().getRegionFactory().createTransactionContext( this );
		transactionCoordinator =
				factory.transactionCoordinatorBuilder.buildTransactionCoordinator( jdbcCoordinator, this );

		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );
	}

}
