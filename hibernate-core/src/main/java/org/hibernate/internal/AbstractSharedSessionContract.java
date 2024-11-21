/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;

import jakarta.persistence.EntityGraph;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
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
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.jpa.spi.NativeQueryConstructorTransformer;
import org.hibernate.jpa.spi.NativeQueryListTransformer;
import org.hibernate.jpa.spi.NativeQueryMapTransformer;
import org.hibernate.jpa.spi.NativeQueryTupleTransformer;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
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
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.internal.SqmSelectionQueryImpl;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.resource.jdbc.internal.EmptyStatementInspector;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.TransactionRequiredForJoinException;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.UnknownBasicJavaType;

import static java.lang.Boolean.TRUE;
import static org.hibernate.internal.util.ReflectHelper.isClass;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.jpa.internal.util.FlushModeTypeHelper.getFlushModeType;

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
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( SessionImpl.class );

	private transient SessionFactoryImpl factory;
	protected transient FastSessionServices fastSessionServices;

	private UUID sessionIdentifier;
	private Object sessionToken;

	private transient JdbcConnectionAccess jdbcConnectionAccess;
	private transient JdbcSessionContext jdbcSessionContext;
	private transient JdbcCoordinator jdbcCoordinator;

	private transient TransactionImplementor currentHibernateTransaction;
	private transient TransactionCoordinator transactionCoordinator;
	private transient CacheTransactionSynchronization cacheTransactionSync;

	private final boolean autoJoinTransactions;
	private final boolean isTransactionCoordinatorShared;
	private final PhysicalConnectionHandlingMode connectionHandlingMode;

	private final Interceptor interceptor;

	private final Object tenantIdentifier;
	private final TimeZone jdbcTimeZone;

	// mutable state
	private FlushMode flushMode;
	private CacheMode cacheMode;
	private Integer jdbcBatchSize;

	private boolean criteriaCopyTreeEnabled;

	private boolean nativeJdbcParametersIgnored;

	protected boolean closed;
	protected boolean waitingForAutoClose;

	// transient & non-final for serialization purposes
	private transient SessionEventListenerManager sessionEventsManager;
	private transient EntityNameResolver entityNameResolver;

	//Lazily initialized
	private transient ExceptionConverter exceptionConverter;

	public AbstractSharedSessionContract(SessionFactoryImpl factory, SessionCreationOptions options) {
		this.factory = factory;

		fastSessionServices = factory.getFastSessionServices();
		cacheTransactionSync = factory.getCache().getRegionFactory().createTransactionContext( this );
		flushMode = options.getInitialSessionFlushMode();
		tenantIdentifier = getTenantId( factory, options );
		interceptor = interpret( options.getInterceptor() );
		jdbcTimeZone = options.getJdbcTimeZone();
		sessionEventsManager = createSessionEventsManager(options);
		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );
		setCriteriaCopyTreeEnabled( factory.getSessionFactoryOptions().isCriteriaCopyTreeEnabled() );
		setNativeJdbcParametersIgnored( factory.getSessionFactoryOptions().getNativeJdbcParametersIgnored() );

		final StatementInspector statementInspector = interpret( options.getStatementInspector() );

		isTransactionCoordinatorShared = isTransactionCoordinatorShared( options );
		if ( isTransactionCoordinatorShared ) {
			final SharedSessionCreationOptions sharedOptions = (SharedSessionCreationOptions) options;
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
			transactionCoordinator = fastSessionServices.transactionCoordinatorBuilder
					.buildTransactionCoordinator( jdbcCoordinator, this );
		}
	}

	private static boolean isTransactionCoordinatorShared(SessionCreationOptions options) {
		return options instanceof SharedSessionCreationOptions
			&& ( (SharedSessionCreationOptions) options ).isTransactionCoordinatorShared();
	}

	protected final void setUpMultitenancy(SessionFactoryImplementor factory, LoadQueryInfluencers loadQueryInfluencers) {
		if ( factory.getDefinedFilterNames().contains( TenantIdBinder.FILTER_NAME ) ) {
			final Object tenantIdentifier = getTenantIdentifierValue();
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "SessionFactory configured for multi-tenancy, but no tenant identifier specified" );
			}
			else {
				final CurrentTenantIdentifierResolver<Object> resolver = factory.getCurrentTenantIdentifierResolver();
				if ( resolver==null || !resolver.isRoot( tenantIdentifier ) ) {
					// turn on the filter, unless this is the "root" tenant with access to all partitions
					loadQueryInfluencers
							.enableFilter( TenantIdBinder.FILTER_NAME )
							.setParameter( TenantIdBinder.PARAMETER_NAME, tenantIdentifier );
				}
			}
		}
	}

	private void logInconsistentOptions(SharedSessionCreationOptions sharedOptions) {
		if ( sharedOptions.shouldAutoJoinTransactions() ) {
			log.debug(
					"Session creation specified 'autoJoinTransactions', which is invalid in conjunction " +
							"with sharing JDBC connection between sessions; ignoring"
			);
		}
		if ( sharedOptions.getPhysicalConnectionHandlingMode() != connectionHandlingMode ) {
			log.debug(
					"Session creation specified 'PhysicalConnectionHandlingMode' which is invalid in conjunction " +
							"with sharing JDBC connection between sessions; ignoring"
			);
		}
	}

	private JdbcCoordinatorImpl createJdbcCoordinator(SessionCreationOptions options) {
		return new JdbcCoordinatorImpl( options.getConnection(), this, fastSessionServices.jdbcServices );
	}

	private JdbcSessionContextImpl createJdbcSessionContext(StatementInspector statementInspector) {
		return new JdbcSessionContextImpl(
				factory,
				statementInspector,
				connectionHandlingMode,
				fastSessionServices.jdbcServices,
				fastSessionServices.batchBuilder,
				// TODO: this object is deprecated and should be removed
				new JdbcObserverImpl(
						fastSessionServices.getDefaultJdbcObserver(),
						sessionEventsManager,
						() -> jdbcCoordinator.abortBatch() // since jdbcCoordinator not yet initialized here
				)
		);
	}

	private Object getTenantId( SessionFactoryImpl factory, SessionCreationOptions options ) {
		final Object tenantIdentifier = options.getTenantIdentifierValue();
		if ( factory.getSessionFactoryOptions().isMultiTenancyEnabled() && tenantIdentifier == null ) {
			throw new HibernateException( "SessionFactory configured for multi-tenancy, but no tenant identifier specified" );
		}
		return tenantIdentifier;
	}

	private SessionEventListenerManager createSessionEventsManager(SessionCreationOptions options) {
		final List<SessionEventListener> customSessionEventListener = options.getCustomSessionEventListener();
		return customSessionEventListener == null
				? new SessionEventListenerManagerImpl( fastSessionServices.defaultSessionEventListeners.buildBaseline() )
				: new SessionEventListenerManagerImpl( customSessionEventListener.toArray( new SessionEventListener[0] ) );
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
				? fastSessionServices.defaultJdbcBatchSize
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
			log.exceptionInBeforeTransactionCompletionInterceptor( t );
		}
	}

	void afterTransactionCompletionEvents(boolean successful) {
		getEventListenerManager().transactionCompletion(successful);

		final StatisticsImplementor statistics = getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.endTransaction(successful);
		}

		try {
			getInterceptor().afterTransactionCompletion( getTransactionIfAccessible() );
		}
		catch (Throwable t) {
			log.exceptionInAfterTransactionCompletionInterceptor( t );
		}
	}

	private Transaction getTransactionIfAccessible() {
		// We do not want an exception to be thrown if the transaction
		// is not accessible. If the transaction is not accessible,
		// then return null.
		return fastSessionServices.isJtaTransactionAccessible ? accessTransaction() : null;
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
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public Interceptor getInterceptor() {
		return interceptor;
	}

	@Override
	public JdbcCoordinator getJdbcCoordinator() {
		return jdbcCoordinator;
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return transactionCoordinator;
	}

	@Override
	public JdbcSessionContext getJdbcSessionContext() {
		return jdbcSessionContext;
	}

	public EntityNameResolver getEntityNameResolver() {
		return entityNameResolver;
	}

	@Override
	public SessionEventListenerManager getEventListenerManager() {
		return sessionEventsManager;
	}

	@Override
	public UUID getSessionIdentifier() {
		if ( sessionIdentifier == null ) {
			//Lazily initialized: otherwise all the UUID generations will cause significant amount of contention.
			sessionIdentifier = StandardRandomStrategy.INSTANCE.generateUUID( null );
		}
		return sessionIdentifier;
	}

	@Override
	public Object getSessionToken() {
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
	public Object getTenantIdentifierValue() {
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
		if ( closed && !waitingForAutoClose ) {
			return;
		}

		try {
			delayedAfterCompletion();
		}
		catch ( HibernateException e ) {
			if ( getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
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

		try {
			if ( shouldCloseJdbcCoordinatorOnClose( isTransactionCoordinatorShared ) ) {
				jdbcCoordinator.close();
			}
		}
		finally {
			setClosed();
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
		return !isClosed() || waitingForAutoClose;
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

	@Override
	public void prepareForQueryExecution(boolean requiresTxn) {
		checkOpen();
		checkTransactionSynchStatus();

		if ( requiresTxn && !isTransactionInProgress() ) {
			throw new TransactionRequiredException(
					"Query requires transaction be in progress, but no transaction is known to be in progress"
			);
		}
	}

	protected void checkOpenOrWaitingForAutoClose() {
		if ( !waitingForAutoClose ) {
			checkOpen();
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
		if ( waitingForAutoClose ) {
			return factory.isOpen() && transactionCoordinator.isTransactionActive();
		}
		return !isClosed() && transactionCoordinator.isTransactionActive();
	}

	@Override
	public void checkTransactionNeededForUpdateOperation(String exceptionMessage) {
		if ( fastSessionServices.disallowOutOfTransactionUpdateOperations && !isTransactionInProgress() ) {
			throw new TransactionRequiredException( exceptionMessage );
		}
	}

	@Override
	public Transaction getTransaction() throws HibernateException {
		if ( ! fastSessionServices.isJtaTransactionAccessible ) {
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
		if ( !isClosed() || waitingForAutoClose && factory.isOpen() ) {
			getTransactionCoordinator().pulse();
		}
		return currentHibernateTransaction;
	}

	@Override
	public void startTransactionBoundary() {
		getCacheTransactionSynchronization().transactionJoined();
	}

	@Override
	public void beforeTransactionCompletion() {
		getCacheTransactionSynchronization().transactionCompleting();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		getCacheTransactionSynchronization().transactionCompleted( successful );
	}

	@Override
	public CacheTransactionSynchronization getCacheTransactionSynchronization() {
		return cacheTransactionSync;
	}

	@Override
	public Transaction beginTransaction() {
		checkOpen();
		final Transaction result = getTransaction();
		result.begin();
		return result;
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
		if ( !getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta() ) {
			log.callingJoinTransactionOnNonJtaEntityManager();
			return;
		}

		try {
			getTransactionCoordinator().explicitJoin();
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
		return getTransactionCoordinator().isJoined();
	}

	protected void delayedAfterCompletion() {
		if ( transactionCoordinator instanceof JtaTransactionCoordinatorImpl ) {
			( (JtaTransactionCoordinatorImpl) transactionCoordinator )
					.getSynchronizationCallbackCoordinator()
					.processAnyDelayedAfterCompletion();
		}
	}

	protected TransactionImplementor getCurrentTransaction() {
		return currentHibernateTransaction;
	}

	@Override
	public boolean isConnected() {
		pulseTransactionCoordinator();
		return jdbcCoordinator.getLogicalConnection().isOpen();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		// See class-level JavaDocs for a discussion of the concurrent-access safety of this method
		if ( jdbcConnectionAccess == null ) {
			if ( ! fastSessionServices.requiresMultiTenantConnectionProvider ) {
				jdbcConnectionAccess = new NonContextualJdbcConnectionAccess(
						getEventListenerManager(),
						fastSessionServices.connectionProvider,
						this
				);
			}
			else {
				jdbcConnectionAccess = new ContextualJdbcConnectionAccess(
						getTenantIdentifierValue(),
						getEventListenerManager(),
						fastSessionServices.multiTenantConnectionProvider,
						this
				);
			}
		}
		return jdbcConnectionAccess;
	}

	@Override
	public EntityKey generateEntityKey(Object id, EntityPersister persister) {
		return new EntityKey( id, persister );
	}

	@Override
	public boolean useStreamForLobBinding() {
		return fastSessionServices.useStreamForLobBinding;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return fastSessionServices.preferredSqlTypeCodeForBoolean;
	}

	@Override
	public LobCreator getLobCreator() {
		return getFactory().getFastSessionServices().jdbcServices.getLobCreator( this );
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
	public JdbcServices getJdbcServices() {
		return getFactory().getJdbcServices();
	}

	@Override
	public FlushModeType getFlushMode() {
		checkOpen();
		return getFlushModeType( flushMode );
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return flushMode;
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
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			final HqlInterpretation<R> interpretation = interpretHql( hql, resultType );
			checkSelectionQuery( hql, interpretation );
			return createSelectionQuery( hql, resultType, interpretation );
		}
		catch ( RuntimeException e ) {
			markForRollbackOnly();
			throw e;
		}
	}

	private <R> SelectionQuery<R> createSelectionQuery(String hql, Class<R> resultType, HqlInterpretation<R> interpretation) {
		final SqmSelectionQueryImpl<R> query = new SqmSelectionQueryImpl<>( hql, interpretation, resultType, this );
		if ( resultType != null ) {
			checkResultType( resultType, query );
		}
		query.setComment( hql );
		applyQuerySettingsAndHints( query );
		return query;
	}

	protected <R> HqlInterpretation<R> interpretHql(String hql, Class<R> resultType) {
		final QueryEngine queryEngine = getFactory().getQueryEngine();
		return queryEngine.interpretHql( hql, resultType );
	}

	protected static void checkSelectionQuery(String hql, HqlInterpretation<?> hqlInterpretation) {
		if ( !( hqlInterpretation.getSqmStatement() instanceof SqmSelectStatement ) ) {
			throw new IllegalSelectQueryException( "Expecting a selection query, but found `" + hql + "`", hql);
		}
	}

	protected static <R> void checkResultType(Class<R> expectedResultType, SqmSelectionQueryImpl<R> query) {
		final Class<?> resultType = query.getResultType();
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
	public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
		if ( criteria instanceof CriteriaDefinition ) {
			return ((CriteriaDefinition<R>) criteria).createSelectionQuery(this);
		}
		else {
			SqmUtil.verifyIsSelectStatement( (SqmStatement<?>) criteria, null );
			return new SqmSelectionQueryImpl<>( (SqmSelectStatement<R>) criteria, criteria.getResultType(), this );
		}
	}

	@Override
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> expectedResultType) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			final HqlInterpretation<T> interpretation = interpretHql( queryString, expectedResultType );
			final QuerySqmImpl<T> query = new QuerySqmImpl<>( queryString, interpretation, expectedResultType, this );
			applyQuerySettingsAndHints( query );
			query.setComment( queryString );
			return query;
		}
		catch (RuntimeException e) {
			markForRollbackOnly();
			throw getExceptionConverter().convert( e );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// dynamic native (SQL) query handling

	@Override @SuppressWarnings("rawtypes")
	public NativeQueryImplementor createNativeQuery(String sqlString) {
		return createNativeQuery( sqlString, (Class) null );
	}

	@Override @SuppressWarnings("rawtypes")
	public NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMappingName) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			return isNotEmpty( resultSetMappingName )
					? new NativeQueryImpl<>( sqlString, getResultSetMappingMemento( resultSetMappingName ), this )
					: new NativeQueryImpl<>( sqlString, this );
			//TODO: why no applyQuerySettingsAndHints( query ); ???
		}
		catch ( RuntimeException he ) {
			throw getExceptionConverter().convert( he );
		}
	}

	protected NamedResultSetMappingMemento getResultSetMappingMemento(String resultSetMappingName) {
		final NamedResultSetMappingMemento resultSetMappingMemento = getFactory().getQueryEngine()
				.getNamedObjectRepository().getResultSetMappingMemento( resultSetMappingName );
		if ( resultSetMappingMemento == null ) {
			throw new HibernateException( "Could not resolve specified result-set mapping name: "
					+ resultSetMappingName );
		}
		return resultSetMappingMemento;
	}

	@Override @SuppressWarnings({"rawtypes", "unchecked"})
	//note: we're doing something a bit funny here to work around
	//      the clashing signatures declared by the supertypes
	public NativeQueryImplementor createNativeQuery(String sqlString, @Nullable Class resultClass) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			final NativeQueryImpl query = new NativeQueryImpl<>( sqlString, resultClass, this );
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

	/**
	 * @deprecated Use {@link NativeQueryImpl#NativeQueryImpl(String, Class, SharedSessionContractImplementor)} instead
	 */
	@Deprecated(forRemoval = true)
	protected <T> void addResultType(Class<T> resultClass, NativeQueryImplementor<T> query) {
		if ( Tuple.class.equals( resultClass ) ) {
			query.setTupleTransformer( NativeQueryTupleTransformer.INSTANCE );
		}
		else if ( Map.class.equals( resultClass ) ) {
			query.setTupleTransformer( NativeQueryMapTransformer.INSTANCE );
		}
		else if ( List.class.equals( resultClass ) ) {
			query.setTupleTransformer( NativeQueryListTransformer.INSTANCE );
		}
		else if ( getFactory().getMappingMetamodel().isEntityClass( resultClass ) ) {
			query.addEntity( resultClass, LockMode.READ );
		}
		else if ( resultClass != Object.class && resultClass != Object[].class ) {
			if ( isClass( resultClass ) && !hasJavaTypeDescriptor( resultClass ) ) {
				// not a basic type
				query.setTupleTransformer( new NativeQueryConstructorTransformer<>( resultClass ) );
			}
			else {
				query.addResultTypeClass( resultClass );
			}
		}
	}

	private <T> boolean hasJavaTypeDescriptor(Class<T> resultClass) {
		final JavaType<Object> descriptor = getTypeConfiguration().getJavaTypeRegistry().findDescriptor( resultClass );
		return descriptor != null && descriptor.getClass() != UnknownBasicJavaType.class;
	}

	@Override
	public <T> NativeQueryImplementor<T> createNativeQuery(String sqlString, Class<T> resultClass, String tableAlias) {
		@SuppressWarnings("unchecked")
		final NativeQueryImplementor<T> query = createNativeQuery( sqlString );
		if ( getFactory().getMappingMetamodel().isEntityClass( resultClass ) ) {
			query.addEntity( tableAlias, resultClass.getName(), LockMode.READ );
			return query;
		}
		else {
			throw new UnknownEntityTypeException( "unable to locate persister: " + resultClass.getName() );
		}
	}

	@Override
	public <T> NativeQueryImplementor<T>  createNativeQuery(String sqlString, String resultSetMappingName, Class<T> resultClass) {
		@SuppressWarnings("unchecked")
		final NativeQueryImplementor<T> query = createNativeQuery( sqlString, resultSetMappingName );
		if ( Tuple.class.equals( resultClass ) ) {
			query.setTupleTransformer( NativeQueryTupleTransformer.INSTANCE );
		}
		else if ( Map.class.equals( resultClass ) ) {
			query.setTupleTransformer( NativeQueryMapTransformer.INSTANCE );
		}
		else if ( List.class.equals( resultClass ) ) {
			query.setTupleTransformer( NativeQueryListTransformer.INSTANCE );
		}
		return query;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// named query handling

	@Override @SuppressWarnings("rawtypes")
	public QueryImplementor getNamedQuery(String queryName) {
		return buildNamedQuery( queryName, null );
	}

	@Override @SuppressWarnings("rawtypes")
	public QueryImplementor createNamedQuery(String name) {
		return buildNamedQuery( name, null );
	}

	@Override
	public <R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass) {
		return buildNamedQuery( name, resultClass );
	}

	@Override
	public SelectionQuery<?> createNamedSelectionQuery(String queryName) {
		return createNamedSelectionQuery( queryName, null );
	}

	@Override
	public <R> SelectionQuery<R> createNamedSelectionQuery(String queryName, Class<R> expectedResultType) {
		return buildNamedQuery(
				queryName,
				memento -> createNamedSqmSelectionQuery( memento, expectedResultType ),
				memento -> createNamedNativeSelectionQuery( memento, expectedResultType )
		);
	}

	private NamedSqmQueryMemento getSqmQueryMemento(String queryName) {
		return getFactory().getQueryEngine()
				.getNamedObjectRepository()
				.getSqmQueryMemento( queryName );
	}

	private NamedNativeQueryMemento getNativeQueryMemento(String queryName) {
		return getFactory().getQueryEngine()
				.getNamedObjectRepository()
				.getNativeQueryMemento( queryName );
	}

	private <R> SelectionQuery<R> createNamedNativeSelectionQuery(
			NamedNativeQueryMemento memento,
			Class<R> expectedResultType) {
		throw new UnsupportedOperationException(
				String.format(
						Locale.ROOT,
						"Support for `@%s` + `%s` is not (yet) implemented",
						NamedNativeQuery.class.getName(),
						SelectionQuery.class.getName()
				)
		);
	}

	private <R> SqmSelectionQuery<R> createNamedSqmSelectionQuery(
			NamedSqmQueryMemento memento,
			Class<R> expectedResultType) {
		final SqmSelectionQuery<R> selectionQuery = memento.toSelectionQuery( expectedResultType, this );
		if ( isEmpty( memento.getComment() ) ) {
			selectionQuery.setComment( "Named query : " + memento.getRegistrationName() );
		}
		else {
			selectionQuery.setComment( memento.getComment() );
		}
		applyQuerySettingsAndHints( selectionQuery );
		if ( memento.getLockOptions() != null ) {
			selectionQuery.getLockOptions().overlay( memento.getLockOptions() );
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

	protected <Q> Q buildNamedQuery(
			String queryName,
			Function<NamedSqmQueryMemento, Q> sqlCreator,
			Function<NamedNativeQueryMemento, Q> nativeCreator) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		// this method can be called for either a named HQL query or a named native query

		// first see if it is a named HQL query
		final NamedSqmQueryMemento namedSqmQueryMemento = getSqmQueryMemento( queryName );
		if ( namedSqmQueryMemento != null ) {
			return sqlCreator.apply( namedSqmQueryMemento );
		}

		// otherwise, see if it is a named native query
		final NamedNativeQueryMemento namedNativeDescriptor = getNativeQueryMemento( queryName );
		if ( namedNativeDescriptor != null ) {
			return nativeCreator.apply( namedNativeDescriptor );
		}

		throw new UnknownNamedQueryException( queryName );
	}

	protected <T> QueryImplementor<T> buildNamedQuery(String queryName, Class<T> resultType) {
		try {
			return buildNamedQuery(
					queryName,
					memento -> createSqmQueryImplementor( resultType, memento ),
					memento -> createNativeQueryImplementor( resultType, memento )
			);
		}
		catch ( UnknownNamedQueryException e ) {
			// JPA expects this to mark the transaction for rollback only
			transactionCoordinator.getTransactionDriverControl().markRollbackOnly();
			// it also expects an IllegalArgumentException, so wrap UnknownNamedQueryException
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( IllegalArgumentException e ) {
			throw e;
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	protected <T> NativeQueryImplementor<T> createNativeQueryImplementor(Class<T> resultType, NamedNativeQueryMemento memento) {
		final NativeQueryImplementor<T> query = resultType == null
				? memento.toQuery(this )
				: memento.toQuery(this, resultType );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic native-SQL query" );
		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	protected <T> SqmQueryImplementor<T> createSqmQueryImplementor(Class<T> resultType, NamedSqmQueryMemento memento) {
		final SqmQueryImplementor<T> query = memento.toQuery( this, resultType );
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic query" );
		}
		applyQuerySettingsAndHints( query );
		if ( memento.getLockOptions() != null ) {
			query.setLockOptions( memento.getLockOptions() );
		}
		return query;
	}

	@Override @SuppressWarnings("rawtypes")
	public NativeQueryImplementor getNamedNativeQuery(String queryName) {
		final NamedNativeQueryMemento namedNativeDescriptor = getNativeQueryMemento( queryName );
		if ( namedNativeDescriptor != null ) {
			return namedNativeDescriptor.toQuery( this );
		}
		else {
			throw noQueryForNameException( queryName );
		}
	}

	@Override @SuppressWarnings("rawtypes")
	public NativeQueryImplementor getNamedNativeQuery(String queryName, String resultSetMapping) {
		final NamedNativeQueryMemento namedNativeDescriptor = getNativeQueryMemento( queryName );
		if ( namedNativeDescriptor != null ) {
			return namedNativeDescriptor.toQuery( this, resultSetMapping );
		}
		else {
			throw noQueryForNameException( queryName );
		}
	}

	private RuntimeException noQueryForNameException(String queryName) {
		return getExceptionConverter()
				.convert( new IllegalArgumentException( "No query defined for that name [" + queryName + "]" ) );
	}

	@Override
	public MutationQuery createMutationQuery(String hqlString) {
		final QueryImplementor<?> query = createQuery( hqlString );
		final SqmStatement<?> sqmStatement = ( (SqmQueryImplementor<?>) query ).getSqmStatement();
		checkMutationQuery( hqlString, sqmStatement );
		return query;
	}

	protected static void checkMutationQuery(String hqlString, SqmStatement<?> sqmStatement) {
		if ( !( sqmStatement instanceof SqmDmlStatement ) ) {
			throw new IllegalMutationQueryException( "Expecting a mutation query, but found `" + hqlString + "`" );
		}
	}

	@Override
	public MutationQuery createNativeMutationQuery(String sqlString) {
		final NativeQueryImplementor<?> query = createNativeQuery( sqlString );
		if ( query.isSelectQuery() == TRUE ) {
			throw new IllegalMutationQueryException( "Expecting a native mutation query, but found `" + sqlString + "`" );
		}
		return query;
	}

	@Override
	public MutationQuery createNamedMutationQuery(String queryName) {
		return buildNamedQuery(
				queryName,
				memento -> createSqmQueryImplementor( queryName, memento ),
				memento -> createNativeQueryImplementor( queryName, memento )
		);
	}

	protected NativeQueryImplementor<?> createNativeQueryImplementor(String queryName, NamedNativeQueryMemento memento) {
		final NativeQueryImplementor<?> query = memento.toQuery( this );
		final Boolean isUnequivocallySelect = query.isSelectQuery();
		if ( isUnequivocallySelect == TRUE ) {
			throw new IllegalMutationQueryException(
					"Expecting named native query (" + queryName + ") to be a mutation query, but found `"
							+ memento.getSqlString() + "`"
			);
		}
		if ( isEmpty( query.getComment() ) ) {
			query.setComment( "dynamic native-SQL query" );
		}
		applyQuerySettingsAndHints( query );
		return query;
	}

	protected SqmQueryImplementor<?> createSqmQueryImplementor(String queryName, NamedSqmQueryMemento memento) {
		final SqmQueryImplementor<?> query = memento.toQuery( this );
		final SqmStatement<?> sqmStatement = query.getSqmStatement();
		if ( !( sqmStatement instanceof SqmDmlStatement ) ) {
			throw new IllegalMutationQueryException(
					"Expecting a named mutation query (" + queryName + "), but found a select statement"
			);
		}
		if ( memento.getLockOptions() != null && ! memento.getLockOptions().isEmpty() ) {
			throw new IllegalNamedQueryOptionsException(
					"Named mutation query `" + queryName + "` specified lock-options"
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
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") JpaCriteriaInsertSelect insertSelect) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmInsertSelectStatement<?>) insertSelect, null );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") JpaCriteriaInsert insertSelect) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmInsertStatement<?>) insertSelect, null );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall getNamedProcedureCall(String name) {
		checkOpen();

		final NamedCallableQueryMemento memento = factory.getQueryEngine()
				.getNamedObjectRepository().getCallableQueryMemento( name );
		if ( memento == null ) {
			throw new IllegalArgumentException(
					"Could not find named stored procedure call with that registration name : " + name
			);
		}
		final ProcedureCall procedureCall = memento.makeProcedureCall( this );
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
		final ProcedureCall procedureCall = new ProcedureCallImpl<>( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final ProcedureCall procedureCall = new ProcedureCallImpl<>( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final ProcedureCall procedureCall = new ProcedureCallImpl<>( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final ProcedureCall procedureCall = new ProcedureCallImpl<>( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final ProcedureCall procedureCall = new ProcedureCallImpl<>( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		@SuppressWarnings("UnnecessaryLocalVariable")
		final ProcedureCall procedureCall = new ProcedureCallImpl<>( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	protected abstract Object load(String entityName, Object identifier);

	@Override
	public ExceptionConverter getExceptionConverter() {
		if ( exceptionConverter == null ) {
			exceptionConverter = new ExceptionConverterImpl( this );
		}
		return exceptionConverter;
	}

	public Integer getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	@Override
	public EventManager getEventManager() {
		return fastSessionServices.getEventManager();
	}

	@Override
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		this.jdbcBatchSize = jdbcBatchSize;
	}

	@Override
	public HibernateCriteriaBuilder getCriteriaBuilder() {
		checkOpen();
		return getFactory().getCriteriaBuilder();
	}

	@Override
	public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		checkOpen();
		if ( criteriaQuery instanceof CriteriaDefinition ) {
			return (QueryImplementor<T>) ((CriteriaDefinition<T>) criteriaQuery).createSelectionQuery(this);
		}
		else {
			try {
				final SqmSelectStatement<T> selectStatement = (SqmSelectStatement<T>) criteriaQuery;
				if ( ! ( selectStatement.getQueryPart() instanceof SqmQueryGroup ) ) {
					final SqmQuerySpec<T> querySpec = selectStatement.getQuerySpec();
					if ( querySpec.getSelectClause().getSelections().isEmpty() ) {
						if ( querySpec.getFromClause().getRoots().size() == 1 ) {
							querySpec.getSelectClause().setSelection( querySpec.getFromClause().getRoots().get(0) );
						}
					}
				}

				return createCriteriaQuery( selectStatement, criteriaQuery.getResultType() );
			}
			catch (RuntimeException e) {
				if ( getSessionFactory().getJpaMetamodel().getJpaCompliance().isJpaTransactionComplianceEnabled() ) {
					markForRollbackOnly();
				}
				throw getExceptionConverter().convert( e );
			}
		}
	}

	@Override @SuppressWarnings({"unchecked", "rawtypes"})
	public QueryImplementor createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate criteriaUpdate) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmUpdateStatement<Void>) criteriaUpdate, null );
		}
		catch (RuntimeException e) {
			if ( getSessionFactory().getJpaMetamodel().getJpaCompliance().isJpaTransactionComplianceEnabled() ) {
				markForRollbackOnly();
			}
			throw getExceptionConverter().convert( e );
		}
	}

	@Override @SuppressWarnings({"unchecked", "rawtypes"})
	public QueryImplementor createQuery(@SuppressWarnings("rawtypes") CriteriaDelete criteriaDelete) {
		checkOpen();
		try {
			return createCriteriaQuery( (SqmDeleteStatement<Void>) criteriaDelete, null );
		}
		catch (RuntimeException e) {
			if ( getSessionFactory().getJpaMetamodel().getJpaCompliance().isJpaTransactionComplianceEnabled() ) {
				markForRollbackOnly();
			}
			throw getExceptionConverter().convert( e );
		}
	}

	private <T> QueryImplementor<T> createCriteriaQuery(SqmStatement<T> criteria, Class<T> resultType) {
		final QuerySqmImpl<T> query = new QuerySqmImpl<>( criteria, resultType, this );
		applyQuerySettingsAndHints( query );
		return query;
	}

	@Override
	public <T> RootGraphImplementor<T> createEntityGraph(Class<T> rootType) {
		checkOpen();
		return new RootGraphImpl<>( null, getFactory().getJpaMetamodel().entity( rootType ) );
	}

	@Override @SuppressWarnings("unchecked")
	public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
		final RootGraph<?> entityGraph = createEntityGraph( graphName );
		if ( entityGraph == null ) {
			return null;
		}
		final ManagedDomainType<T> type = getFactory().getJpaMetamodel().managedType( rootType );
		final ManagedDomainType<?> graphedType = entityGraph.getGraphedType();
		if ( !Objects.equals( graphedType.getTypeName(), type.getTypeName() ) ) {
			throw new IllegalArgumentException( "Named entity graph '" + graphName
					+ "' is for type '" + graphedType.getTypeName() + "'");
		}
		return (RootGraph<T>) entityGraph;
	}


	@Override
	public RootGraphImplementor<?> createEntityGraph(String graphName) {
		checkOpen();
		final RootGraphImplementor<?> named = getFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			return null;
		}
		return named.makeRootGraph( graphName, true );
	}

	@Override
	public RootGraphImplementor<?> getEntityGraph(String graphName) {
		checkOpen();
		final RootGraphImplementor<?> named = getFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			throw new IllegalArgumentException( "Could not locate EntityGraph with given name : " + graphName );
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

	private void writeObject(ObjectOutputStream oos) throws IOException {
		if ( log.isTraceEnabled() ) {
			log.trace( "Serializing " + getClass().getSimpleName() + " [" );
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

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException, SQLException {
		if ( log.isTraceEnabled() ) {
			log.trace( "Deserializing " + getClass().getSimpleName() );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 1 :: read back non-transient state...
		ois.defaultReadObject();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 2 :: read back transient state...
		//		-- see above

		factory = SessionFactoryImpl.deserialize( ois );
		fastSessionServices = factory.getFastSessionServices();
		sessionEventsManager = new SessionEventListenerManagerImpl( fastSessionServices.defaultSessionEventListeners.buildBaseline() );
		jdbcSessionContext = createJdbcSessionContext( (StatementInspector) ois.readObject() );
		jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, this );

		cacheTransactionSync = factory.getCache().getRegionFactory().createTransactionContext( this );

		transactionCoordinator = factory.getServiceRegistry()
				.requireService( TransactionCoordinatorBuilder.class )
				.buildTransactionCoordinator( jdbcCoordinator, this );

		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );
	}

}
