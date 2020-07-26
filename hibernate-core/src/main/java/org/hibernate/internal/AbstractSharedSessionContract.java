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
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import javax.persistence.FlushModeType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Selection;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityNameResolver;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.spi.CriteriaQueryTupleTransformer;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.jpa.spi.NativeQueryTupleTransformer;
import org.hibernate.jpa.spi.TupleBuilderTransformer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.internal.compile.CompilableCriteria;
import org.hibernate.query.criteria.internal.compile.CriteriaCompiler;
import org.hibernate.query.criteria.internal.expression.CompoundSelectionImpl;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.query.internal.QueryImpl;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Base class for SharedSessionContract/SharedSessionContractImplementor
 * implementations.  Intended for Session and StatelessSession implementations
 * <P/>
 * NOTE: This implementation defines access to a number of instance state values
 * in a manner that is not exactly concurrent-access safe.  However, a Session/EntityManager
 * is never intended to be used concurrently; therefore the condition is not expected
 * and so a more synchronized/concurrency-safe is not defined to be as negligent
 * (performance-wise) as possible.  Some of these methods include:<ul>
 *     <li>{@link #getEventListenerManager()}</li>
 *     <li>{@link #getJdbcConnectionAccess()}</li>
 *     <li>{@link #getJdbcServices()}</li>
 *     <li>{@link #getTransaction()} (and therefore related methods such as {@link #beginTransaction()}, etc)</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractSharedSessionContract implements SharedSessionContractImplementor {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( SessionImpl.class );

	private transient SessionFactoryImpl factory;
	private final String tenantIdentifier;
	protected transient FastSessionServices fastSessionServices;
	private UUID sessionIdentifier;

	private transient JdbcConnectionAccess jdbcConnectionAccess;
	private transient JdbcSessionContext jdbcSessionContext;
	private transient JdbcCoordinator jdbcCoordinator;

	private transient TransactionImplementor currentHibernateTransaction;
	private transient TransactionCoordinator transactionCoordinator;
	private transient CacheTransactionSynchronization cacheTransactionSync;

	private final boolean isTransactionCoordinatorShared;
	private final Interceptor interceptor;

	private final TimeZone jdbcTimeZone;

	private FlushMode flushMode;
	private boolean autoJoinTransactions;

	private CacheMode cacheMode;

	protected boolean closed;
	protected boolean waitingForAutoClose;

	// transient & non-final for Serialization purposes - ugh
	private transient SessionEventListenerManagerImpl sessionEventsManager;
	private transient EntityNameResolver entityNameResolver;

	private Integer jdbcBatchSize;

	//Lazily initialized
	private transient ExceptionConverter exceptionConverter;

	private CriteriaCompiler criteriaCompiler;

	public AbstractSharedSessionContract(SessionFactoryImpl factory, SessionCreationOptions options) {
		this.factory = factory;
		this.fastSessionServices = factory.getFastSessionServices();
		this.cacheTransactionSync = factory.getCache().getRegionFactory().createTransactionContext( this );


		this.flushMode = options.getInitialSessionFlushMode();

		this.tenantIdentifier = options.getTenantIdentifier();
		if ( MultiTenancyStrategy.NONE == factory.getSettings().getMultiTenancyStrategy() ) {
			if ( tenantIdentifier != null ) {
				throw new HibernateException( "SessionFactory was not configured for multi-tenancy" );
			}
		}
		else {
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "SessionFactory configured for multi-tenancy, but no tenant identifier specified" );
			}
		}

		this.interceptor = interpret( options.getInterceptor() );
		this.jdbcTimeZone = options.getJdbcTimeZone();
		final List<SessionEventListener> customSessionEventListener = options.getCustomSessionEventListener();
		if ( customSessionEventListener == null ) {
			sessionEventsManager = new SessionEventListenerManagerImpl( fastSessionServices.defaultSessionEventListeners.buildBaseline() );
		}
		else {
			sessionEventsManager = new SessionEventListenerManagerImpl( customSessionEventListener.toArray( new SessionEventListener[0] ) );
		}

		final StatementInspector statementInspector = interpret( options.getStatementInspector() );
		this.jdbcSessionContext = new JdbcSessionContextImpl( this, statementInspector, fastSessionServices );

		this.entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );

		if ( options instanceof SharedSessionCreationOptions && ( (SharedSessionCreationOptions) options ).isTransactionCoordinatorShared() ) {
			if ( options.getConnection() != null ) {
				throw new SessionException( "Cannot simultaneously share transaction context and specify connection" );
			}

			this.isTransactionCoordinatorShared = true;

			final SharedSessionCreationOptions sharedOptions = (SharedSessionCreationOptions) options;
			this.transactionCoordinator = sharedOptions.getTransactionCoordinator();
			this.jdbcCoordinator = sharedOptions.getJdbcCoordinator();

			// todo : "wrap" the transaction to no-op comit/rollback attempts?
			this.currentHibernateTransaction = sharedOptions.getTransaction();

			if ( sharedOptions.shouldAutoJoinTransactions() ) {
				log.debug(
						"Session creation specified 'autoJoinTransactions', which is invalid in conjunction " +
								"with sharing JDBC connection between sessions; ignoring"
				);
				autoJoinTransactions = false;
			}
			if ( sharedOptions.getPhysicalConnectionHandlingMode() != this.jdbcCoordinator.getLogicalConnection().getConnectionHandlingMode() ) {
				log.debug(
						"Session creation specified 'PhysicalConnectionHandlingMode which is invalid in conjunction " +
								"with sharing JDBC connection between sessions; ignoring"
				);
			}

			addSharedSessionTransactionObserver( transactionCoordinator );
		}
		else {
			this.isTransactionCoordinatorShared = false;
			this.autoJoinTransactions = options.shouldAutoJoinTransactions();
			this.jdbcCoordinator = new JdbcCoordinatorImpl( options.getConnection(), this, fastSessionServices.jdbcServices );
			this.transactionCoordinator = fastSessionServices.transactionCoordinatorBuilder.buildTransactionCoordinator( jdbcCoordinator, this );
		}
	}

	protected void addSharedSessionTransactionObserver(TransactionCoordinator transactionCoordinator) {
	}

	protected void removeSharedSessionTransactionObserver(TransactionCoordinator transactionCoordinator) {
		transactionCoordinator.invalidate();
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return autoJoinTransactions;
	}

	private Interceptor interpret(Interceptor interceptor) {
		return interceptor == null ? EmptyInterceptor.INSTANCE : interceptor;
	}

	private StatementInspector interpret(StatementInspector statementInspector) {
		if ( statementInspector == null ) {
			// If there is no StatementInspector specified, map to the call
			//		to the (deprecated) Interceptor #onPrepareStatement method
			return (StatementInspector) interceptor::onPrepareStatement;
		}
		return statementInspector;
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
		return this.jdbcSessionContext;
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
		if ( this.sessionIdentifier == null ) {
			//Lazily initialized: otherwise all the UUID generations will cause of significant amount of contention.
			this.sessionIdentifier = StandardRandomStrategy.INSTANCE.generateUUID( null );
		}
		return sessionIdentifier;
	}

	@Override
	public String getTenantIdentifier() {
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

		if ( currentHibernateTransaction != null ) {
			currentHibernateTransaction.invalidate();
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

	protected void checkOpenOrWaitingForAutoClose() {
		if ( !waitingForAutoClose ) {
			checkOpen();
		}
	}

	/**
	 * @deprecated (since 5.2) use {@link #checkOpen()} instead
	 */
	@Deprecated
	protected void errorIfClosed() {
		checkOpen();
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
		if ( this.currentHibernateTransaction == null ) {
			this.currentHibernateTransaction = new TransactionImpl(
					getTransactionCoordinator(),
					this
			);
		}
		if ( !isClosed() || ( waitingForAutoClose && factory.isOpen() ) ) {
			getTransactionCoordinator().pulse();
		}
		return this.currentHibernateTransaction;
	}

	@Override
	public void startTransactionBoundary() {
		this.getCacheTransactionSynchronization().transactionJoined();
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
	public long getTransactionStartTimestamp() {
		return getCacheTransactionSynchronization().getCurrentTransactionStartTimestamp();
	}

	@Override
	public Transaction beginTransaction() {
		checkOpen();

		Transaction result = getTransaction();
		result.begin();

		return result;
	}

	protected void checkTransactionSynchStatus() {
		pulseTransactionCoordinator();
		delayedAfterCompletion();
	}

	protected void pulseTransactionCoordinator() {
		if ( !isClosed() ) {
			transactionCoordinator.pulse();
		}
	}

	protected void delayedAfterCompletion() {
		if ( transactionCoordinator instanceof JtaTransactionCoordinatorImpl ) {
			( (JtaTransactionCoordinatorImpl) transactionCoordinator ).getSynchronizationCallbackCoordinator()
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
						fastSessionServices.connectionProvider
				);
			}
			else {
				jdbcConnectionAccess = new ContextualJdbcConnectionAccess(
						getTenantIdentifier(),
						getEventListenerManager(),
						fastSessionServices.multiTenantConnectionProvider
				);
			}
		}
		return jdbcConnectionAccess;
	}

	@Override
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
		return new EntityKey( id, persister );
	}

	@Override
	public boolean useStreamForLobBinding() {
		return fastSessionServices.useStreamForLobBinding;
	}

	@Override
	public LobCreator getLobCreator() {
		return Hibernate.getLobCreator( this );
	}

	@Override
	public <T> T execute(final LobCreationContext.Callback<T> callback) {
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
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		return fastSessionServices.remapSqlTypeDescriptor( sqlTypeDescriptor );
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
	public void setFlushMode(FlushMode flushMode) {
		setHibernateFlushMode( flushMode );
	}

	@Override
	public FlushModeType getFlushMode() {
		checkOpen();
		return FlushModeTypeHelper.getFlushModeType( this.flushMode );
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

	protected HQLQueryPlan getQueryPlan(String query, boolean shallow) throws HibernateException {
		return getFactory().getQueryPlanCache().getHQLQueryPlan( query, shallow, getLoadQueryInfluencers().getEnabledFilters() );
	}

	protected NativeSQLQueryPlan getNativeQueryPlan(NativeSQLQuerySpecification spec) throws HibernateException {
		return getFactory().getQueryPlanCache().getNativeSQLQueryPlan( spec );
	}

	@Override
	public QueryImplementor getNamedQuery(String name) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		// look as HQL/JPQL first
		final NamedQueryDefinition queryDefinition = factory.getNamedQueryRepository().getNamedQueryDefinition( name );
		if ( queryDefinition != null ) {
			return createQuery( queryDefinition );
		}

		// then as a native query
		final NamedSQLQueryDefinition nativeQueryDefinition = factory.getNamedQueryRepository().getNamedSQLQueryDefinition( name );
		if ( nativeQueryDefinition != null ) {
			return createNativeQuery( nativeQueryDefinition, true );
		}

		throw getExceptionConverter().convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
	}

	protected QueryImplementor createQuery(NamedQueryDefinition queryDefinition) {
		String queryString = queryDefinition.getQueryString();
		final QueryImpl query = new QueryImpl(
				this,
				getQueryPlan( queryString, false ).getParameterMetadata(),
				queryString
		);
		applyQuerySettingsAndHints( query );
		query.setHibernateFlushMode( queryDefinition.getFlushMode() );
		query.setComment( queryDefinition.getComment() != null ? queryDefinition.getComment() : queryDefinition.getName() );
		if ( queryDefinition.getLockOptions() != null ) {
			query.setLockOptions( queryDefinition.getLockOptions() );
		}

		initQueryFromNamedDefinition( query, queryDefinition );

		return query;
	}

	private NativeQueryImplementor createNativeQuery(NamedSQLQueryDefinition queryDefinition, boolean isOrdinalParameterZeroBased) {
		final ParameterMetadata parameterMetadata = factory.getQueryPlanCache().getSQLParameterMetadata(
				queryDefinition.getQueryString(),
				isOrdinalParameterZeroBased
		);
		return getNativeQueryImplementor( queryDefinition, parameterMetadata );
	}

	private NativeQueryImplementor getNativeQueryImplementor(
			NamedSQLQueryDefinition queryDefinition,
			ParameterMetadata parameterMetadata) {
		final NativeQueryImpl query = new NativeQueryImpl(
				queryDefinition,
				this,
				parameterMetadata
		);
		applyQuerySettingsAndHints( query );
		query.setComment( queryDefinition.getComment() != null ? queryDefinition.getComment() : queryDefinition.getName() );

		initQueryFromNamedDefinition( query, queryDefinition );

		return query;
	}

	protected void initQueryFromNamedDefinition(Query query, NamedQueryDefinition nqd) {
		// todo : cacheable and readonly should be Boolean rather than boolean...
		query.setCacheable( nqd.isCacheable() );
		query.setCacheRegion( nqd.getCacheRegion() );
		query.setReadOnly( nqd.isReadOnly() );

		if ( nqd.getTimeout() != null ) {
			query.setTimeout( nqd.getTimeout() );
		}
		if ( nqd.getFetchSize() != null ) {
			query.setFetchSize( nqd.getFetchSize() );
		}
		if ( nqd.getCacheMode() != null ) {
			query.setCacheMode( nqd.getCacheMode() );
		}
		if ( nqd.getComment() != null ) {
			query.setComment( nqd.getComment() );
		}
		if ( nqd.getFirstResult() != null ) {
			query.setFirstResult( nqd.getFirstResult() );
		}
		if ( nqd.getMaxResults() != null ) {
			query.setMaxResults( nqd.getMaxResults() );
		}
		if ( nqd.getFlushMode() != null ) {
			query.setHibernateFlushMode( nqd.getFlushMode() );
		}
		if ( nqd.getPassDistinctThrough() != null ) {
			query.setHint( QueryHints.HINT_PASS_DISTINCT_THROUGH, nqd.getPassDistinctThrough() );
		}
	}

	@Override
	public QueryImplementor createQuery(String queryString) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			final QueryImpl query = new QueryImpl(
					this,
					getQueryPlan( queryString, false ).getParameterMetadata(),
					queryString
			);
			applyQuerySettingsAndHints( query );
			query.setComment( queryString );
			return query;
		}
		catch (RuntimeException e) {
			markForRollbackOnly();
			throw getExceptionConverter().convert( e );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected CriteriaCompiler criteriaCompiler() {
		if ( criteriaCompiler == null ) {
			criteriaCompiler = new CriteriaCompiler( this );
		}
		return criteriaCompiler;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		checkOpen();
		try {
			return (QueryImplementor<T>) criteriaCompiler().compile( (CompilableCriteria) criteriaQuery );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public QueryImplementor createQuery(CriteriaUpdate criteriaUpdate) {
		checkOpen();
		try {
			return criteriaCompiler().compile( (CompilableCriteria) criteriaUpdate );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public QueryImplementor createQuery(CriteriaDelete criteriaDelete) {
		checkOpen();
		try {
			return criteriaCompiler().compile( (CompilableCriteria) criteriaDelete );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> createQuery(
			String jpaqlString,
			Class<T> resultClass,
			Selection selection,
			HibernateEntityManagerImplementor.QueryOptions queryOptions) {
		try {
			final QueryImplementor query = createQuery( jpaqlString );

			if ( queryOptions.getValueHandlers() == null ) {
				if ( queryOptions.getResultMetadataValidator() != null ) {
					queryOptions.getResultMetadataValidator().validate( query.getReturnTypes() );
				}
			}

			// determine if we need a result transformer
			List tupleElements = Tuple.class.equals( resultClass )
					? ( (CompoundSelectionImpl<Tuple>) selection ).getCompoundSelectionItems()
					: null;
			if ( queryOptions.getValueHandlers() != null || tupleElements != null ) {
				query.setResultTransformer(
						new CriteriaQueryTupleTransformer( queryOptions.getValueHandlers(), tupleElements )
				);
			}

			return query;
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	protected void applyQuerySettingsAndHints(Query query) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultClass) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			// do the translation
			final QueryImplementor<T> query = createQuery( queryString );
			resultClassChecking( resultClass, query );
			return query;
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@SuppressWarnings({"unchecked", "WeakerAccess", "StatementWithEmptyBody"})
	protected void resultClassChecking(Class resultClass, org.hibernate.Query hqlQuery) {
		// make sure the query is a select -> HHH-7192
		final HQLQueryPlan queryPlan = getFactory().getQueryPlanCache().getHQLQueryPlan(
				hqlQuery.getQueryString(),
				false,
				getLoadQueryInfluencers().getEnabledFilters()
		);
		if ( queryPlan.getTranslators()[0].isManipulationStatement() ) {
			throw new IllegalArgumentException( "Update/delete queries cannot be typed" );
		}

		// do some return type validation checking
		if ( Object[].class.equals( resultClass ) ) {
			// no validation needed
		}
		else if ( Tuple.class.equals( resultClass ) ) {
			TupleBuilderTransformer tupleTransformer = new TupleBuilderTransformer( hqlQuery );
			hqlQuery.setResultTransformer( tupleTransformer  );
		}
		else {
			final Class dynamicInstantiationClass = queryPlan.getDynamicInstantiationResultType();
			if ( dynamicInstantiationClass != null ) {
				if ( ! resultClass.isAssignableFrom( dynamicInstantiationClass ) ) {
					throw new IllegalArgumentException(
							"Mismatch in requested result type [" + resultClass.getName() +
									"] and actual result type [" + dynamicInstantiationClass.getName() + "]"
					);
				}
			}
			else if ( queryPlan.getTranslators()[0].getReturnTypes().length == 1 ) {
				// if we have only a single return expression, its java type should match with the requested type
				final Type queryResultType = queryPlan.getTranslators()[0].getReturnTypes()[0];
				if ( !resultClass.isAssignableFrom( queryResultType.getReturnedClass() ) ) {
					throw new IllegalArgumentException(
							"Type specified for TypedQuery [" +
									resultClass.getName() +
									"] is incompatible with query return type [" +
									queryResultType.getReturnedClass() + "]"
					);
				}
			}
			else {
				throw new IllegalArgumentException(
						"Cannot create TypedQuery for query with more than one return using requested result type [" +
								resultClass.getName() + "]"
				);
			}
		}
	}

	@Override
	public QueryImplementor createNamedQuery(String name) {
		return buildQueryFromName( name, null );
	}

	protected  <T> QueryImplementor<T> buildQueryFromName(String name, Class<T> resultType) {
		checkOpen();
		try {
			pulseTransactionCoordinator();
			delayedAfterCompletion();

			// todo : apply stored setting at the JPA Query level too

			final NamedQueryDefinition namedQueryDefinition = getFactory().getNamedQueryRepository().getNamedQueryDefinition( name );
			if ( namedQueryDefinition != null ) {
				return createQuery( namedQueryDefinition, resultType );
			}

			final NamedSQLQueryDefinition nativeQueryDefinition = getFactory().getNamedQueryRepository().getNamedSQLQueryDefinition( name );
			if ( nativeQueryDefinition != null ) {
				return (QueryImplementor<T>) createNativeQuery( nativeQueryDefinition, resultType );
			}

			throw getExceptionConverter().convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
		}
		catch (RuntimeException e) {
			throw !( e instanceof IllegalArgumentException ) ? new IllegalArgumentException( e ) : e;
		}
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected <T> QueryImplementor<T> createQuery(NamedQueryDefinition namedQueryDefinition, Class<T> resultType) {
		final QueryImplementor query = createQuery( namedQueryDefinition );
		if ( resultType != null ) {
			resultClassChecking( resultType, query );
		}
		return query;
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected <T> NativeQueryImplementor createNativeQuery(NamedSQLQueryDefinition queryDefinition, Class<T> resultType) {
		if ( resultType != null && !Tuple.class.equals( resultType ) ) {
			resultClassChecking( resultType, queryDefinition );
		}

		final NativeQueryImpl query = new NativeQueryImpl(
				queryDefinition,
				this,
				factory.getQueryPlanCache().getSQLParameterMetadata( queryDefinition.getQueryString(), false )
		);
		if ( Tuple.class.equals( resultType ) ) {
			query.setResultTransformer( new NativeQueryTupleTransformer() );
		}
		applyQuerySettingsAndHints( query );
		query.setHibernateFlushMode( queryDefinition.getFlushMode() );
		query.setComment( queryDefinition.getComment() != null ? queryDefinition.getComment() : queryDefinition.getName() );
		if ( queryDefinition.getLockOptions() != null ) {
			query.setLockOptions( queryDefinition.getLockOptions() );
		}

		initQueryFromNamedDefinition( query, queryDefinition );

		return query;
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected void resultClassChecking(Class resultType, NamedSQLQueryDefinition namedQueryDefinition) {
		final NativeSQLQueryReturn[] queryReturns;
		if ( namedQueryDefinition.getQueryReturns() != null ) {
			queryReturns = namedQueryDefinition.getQueryReturns();
		}
		else if ( namedQueryDefinition.getResultSetRef() != null ) {
			final ResultSetMappingDefinition rsMapping = getFactory().getNamedQueryRepository().getResultSetMappingDefinition( namedQueryDefinition.getResultSetRef() );
			queryReturns = rsMapping.getQueryReturns();
		}
		else {
			throw new AssertionFailure( "Unsupported named query model. Please report the bug in Hibernate EntityManager");
		}

		if ( queryReturns.length > 1 ) {
			throw new IllegalArgumentException( "Cannot create TypedQuery for query with more than one return" );
		}

		final NativeSQLQueryReturn nativeSQLQueryReturn = queryReturns[0];

		if ( nativeSQLQueryReturn instanceof NativeSQLQueryRootReturn ) {
			final Class<?> actualReturnedClass;
			final String entityClassName = ( (NativeSQLQueryRootReturn) nativeSQLQueryReturn ).getReturnEntityName();
			try {
				actualReturnedClass = fastSessionServices.classLoaderService.classForName( entityClassName );
			}
			catch ( ClassLoadingException e ) {
				throw new AssertionFailure(
						"Unable to load class [" + entityClassName + "] declared on named native query [" +
								namedQueryDefinition.getName() + "]"
				);
			}
			if ( !resultType.isAssignableFrom( actualReturnedClass ) ) {
				throw buildIncompatibleException( resultType, actualReturnedClass );
			}
		}
		else if ( nativeSQLQueryReturn instanceof NativeSQLQueryConstructorReturn ) {
			final NativeSQLQueryConstructorReturn ctorRtn = (NativeSQLQueryConstructorReturn) nativeSQLQueryReturn;
			if ( !resultType.isAssignableFrom( ctorRtn.getTargetClass() ) ) {
				throw buildIncompatibleException( resultType, ctorRtn.getTargetClass() );
			}
		}
		else {
			log.debugf( "Skiping unhandled NativeSQLQueryReturn type : " + nativeSQLQueryReturn );
		}
	}

	private IllegalArgumentException buildIncompatibleException(Class<?> resultClass, Class<?> actualResultClass) {
		return new IllegalArgumentException(
				"Type specified for TypedQuery [" + resultClass.getName() +
						"] is incompatible with query return type [" + actualResultClass + "]"
		);
	}

	@Override
	public <R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass) {
		return buildQueryFromName( name, resultClass );
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString) {
		return getNativeQueryImplementor( sqlString, false );
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			NativeQueryImplementor query = createNativeQuery( sqlString );
			handleNativeQueryResult(query, resultClass);
			return query;
		}
		catch ( RuntimeException he ) {
			throw getExceptionConverter().convert( he );
		}
	}

	private void handleNativeQueryResult(NativeQueryImplementor query, Class resultClass) {
		if ( Tuple.class.equals( resultClass ) ) {
			query.setResultTransformer( new NativeQueryTupleTransformer() );
		}
		else {
			query.addEntity( "alias1", resultClass.getName(), LockMode.READ );
		}
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			NativeQueryImplementor query = createNativeQuery( sqlString );
			query.setResultSetMapping( resultSetMapping );
			return query;
		}
		catch ( RuntimeException he ) {
			throw getExceptionConverter().convert( he );
		}
	}

	@Override
	public NativeQueryImplementor getNamedNativeQuery(String name) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		final NamedSQLQueryDefinition nativeQueryDefinition = factory.getNamedQueryRepository().getNamedSQLQueryDefinition( name );
		if ( nativeQueryDefinition != null ) {
			return createNativeQuery( nativeQueryDefinition, true );
		}

		throw getExceptionConverter().convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
	}

	@Override
	public NativeQueryImplementor createSQLQuery(String queryString) {
		return getNativeQueryImplementor( queryString, true );
	}

	@Override
	public void doWork(final Work work) throws HibernateException {
		WorkExecutorVisitable<Void> realWork = (workExecutor, connection) -> {
			workExecutor.executeWork( work, connection );
			return null;
		};
		doWork( realWork );
	}

	@Override
	public <T> T doReturningWork(final ReturningWork<T> work) throws HibernateException {
		WorkExecutorVisitable<T> realWork = (workExecutor, connection) -> workExecutor.executeReturningWork(
				work,
				connection
		);
		return doWork( realWork );
	}

	private <T> T doWork(WorkExecutorVisitable<T> work) throws HibernateException {
		return getJdbcCoordinator().coordinateWork( work );
	}

	protected NativeQueryImplementor getNativeQueryImplementor(
			String queryString,
			boolean isOrdinalParameterZeroBased) {
		checkOpen();
		pulseTransactionCoordinator();
		delayedAfterCompletion();

		try {
			NativeQueryImpl query = new NativeQueryImpl(
					queryString,
					false,
					this,
					getFactory().getQueryPlanCache().getSQLParameterMetadata( queryString, isOrdinalParameterZeroBased )
			);
			query.setComment( "dynamic native SQL query" );
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch ( RuntimeException he ) {
			throw getExceptionConverter().convert( he );
		}
	}


	@Override
	public NativeQueryImplementor getNamedSQLQuery(String name) {
		return getNamedNativeQuery( name );
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall getNamedProcedureCall(String name) {
		checkOpen();

		final ProcedureCallMemento memento = factory.getNamedQueryRepository().getNamedProcedureCallMemento( name );
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
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		checkOpen();
		final ProcedureCall procedureCall = new ProcedureCallImpl( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		checkOpen();
		final ProcedureCall procedureCall = new ProcedureCallImpl( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		checkOpen();
		final ProcedureCall procedureCall = new ProcedureCallImpl( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	protected abstract Object load(String entityName, Serializable identifier);

	@Override
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) {
		return listCustomQuery( getNativeQueryPlan( spec ).getCustomQuery(), queryParameters );
	}

	@Override
	public ScrollableResultsImplementor scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters) {
		return scrollCustomQuery( getNativeQueryPlan( spec ).getCustomQuery(), queryParameters );
	}

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
	public void setJdbcBatchSize(Integer jdbcBatchSize) {
		this.jdbcBatchSize = jdbcBatchSize;
	}

	@SuppressWarnings("unused")
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
		jdbcSessionContext = new JdbcSessionContextImpl( this, (StatementInspector) ois.readObject(), fastSessionServices );
		jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, this );

		cacheTransactionSync = factory.getCache().getRegionFactory().createTransactionContext( this );

		transactionCoordinator = factory.getServiceRegistry()
				.getService( TransactionCoordinatorBuilder.class )
				.buildTransactionCoordinator( jdbcCoordinator, this );

		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );
	}

}
