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
import javax.persistence.Tuple;

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
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
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
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.spi.TupleBuilderTransformer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
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
import org.hibernate.resource.transaction.spi.TransactionStatus;
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
	private final UUID sessionIdentifier;

	private final boolean isTransactionCoordinatorShared;
	private final Interceptor interceptor;

	private final TimeZone jdbcTimeZone;

	private FlushMode flushMode;
	private boolean autoJoinTransactions;

	private CacheMode cacheMode;

	protected boolean closed;
	protected boolean waitingForAutoClose;

	// transient & non-final for Serialization purposes - ugh
	private transient SessionEventListenerManagerImpl sessionEventsManager = new SessionEventListenerManagerImpl();
	private transient EntityNameResolver entityNameResolver;
	private transient JdbcConnectionAccess jdbcConnectionAccess;
	private transient JdbcSessionContext jdbcSessionContext;
	private transient JdbcCoordinator jdbcCoordinator;
	private transient TransactionImplementor currentHibernateTransaction;
	private transient TransactionCoordinator transactionCoordinator;
	private transient Boolean useStreamForLobBinding;
	private transient long timestamp;

	private transient Integer jdbcBatchSize;

	protected transient ExceptionConverter exceptionConverter;

	public AbstractSharedSessionContract(SessionFactoryImpl factory, SessionCreationOptions options) {
		this.factory = factory;
		this.sessionIdentifier = StandardRandomStrategy.INSTANCE.generateUUID( null );
		this.timestamp = factory.getCache().getRegionFactory().nextTimestamp();

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

		final StatementInspector statementInspector = interpret( options.getStatementInspector() );
		this.jdbcSessionContext = new JdbcSessionContextImpl( this, statementInspector );

		this.entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );

		if ( options instanceof SharedSessionCreationOptions && ( (SharedSessionCreationOptions) options ).isTransactionCoordinatorShared() ) {
			if ( options.getConnection() != null ) {
				throw new SessionException( "Cannot simultaneously share transaction context and specify connection" );
			}

			this.isTransactionCoordinatorShared = true;

			final SharedSessionCreationOptions sharedOptions = (SharedSessionCreationOptions) options;
			this.transactionCoordinator = sharedOptions.getTransactionCoordinator();
			this.jdbcCoordinator = sharedOptions.getJdbcCoordinator();

			// todo : "wrap" the transaction to no-op cmmit/rollback attempts?
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

			this.jdbcCoordinator = new JdbcCoordinatorImpl( options.getConnection(), this );
			this.transactionCoordinator = factory.getServiceRegistry()
					.getService( TransactionCoordinatorBuilder.class )
					.buildTransactionCoordinator( jdbcCoordinator, this );
		}
		exceptionConverter = new ExceptionConverterImpl( this );
	}

	protected void addSharedSessionTransactionObserver(TransactionCoordinator transactionCoordinator) {
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
		checkTransactionSynchStatus();
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
		return sessionIdentifier;
	}

	@Override
	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
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

		if ( sessionEventsManager != null ) {
			sessionEventsManager.end();
		}

		if ( currentHibernateTransaction != null ) {
			currentHibernateTransaction.invalidate();
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
	public void checkOpen(boolean markForRollbackIfClosed) {
		if ( isClosed() ) {
			if ( markForRollbackIfClosed ) {
				markForRollbackOnly();
			}
			throw new IllegalStateException( "Session/EntityManager is closed" );
		}
	}

	protected void checkOpenOrWaitingForAutoClose() {
		if ( !waitingForAutoClose ) {
			checkOpen();
		}
		else if ( factory.isClosed() ) {
			markForRollbackOnly();
			throw new IllegalStateException( "SessionFactory is closed" );
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
		accessTransaction().markRollbackOnly();
	}

	@Override
	public boolean isTransactionInProgress() {
		if ( waitingForAutoClose ) {
			return factory.isOpen() && transactionCoordinator.isTransactionActive();
		}
		return !isClosed() && transactionCoordinator.isTransactionActive();
	}

	@Override
	public Transaction getTransaction() throws HibernateException {
		if ( getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
			// JPA requires that we throw IllegalStateException if this is called
			// on a JTA EntityManager
			if ( getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta() ) {
				if ( !getFactory().getSessionFactoryOptions().isJtaTransactionAccessEnabled() ) {
					throw new IllegalStateException( "A JTA EntityManager cannot use getTransaction()" );
				}
			}
		}
		return accessTransaction();
	}

	@Override
	public Transaction accessTransaction() {
		if ( this.currentHibernateTransaction == null || this.currentHibernateTransaction.getStatus() != TransactionStatus.ACTIVE ) {
			this.currentHibernateTransaction = new TransactionImpl(
					getTransactionCoordinator(),
					getExceptionConverter()
			);

		}
		if ( !isClosed() || (waitingForAutoClose && factory.isOpen()) ) {
			getTransactionCoordinator().pulse();
		}
		return this.currentHibernateTransaction;
	}

	@Override
	public Transaction beginTransaction() {
		checkOpen();

		Transaction result = getTransaction();
		result.begin();

		this.timestamp = factory.getCache().getRegionFactory().nextTimestamp();

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
		checkTransactionSynchStatus();
		return jdbcCoordinator.getLogicalConnection().isOpen();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		// See class-level JavaDocs for a discussion of the concurrent-access safety of this method
		if ( jdbcConnectionAccess == null ) {
			if ( MultiTenancyStrategy.NONE == factory.getSettings().getMultiTenancyStrategy() ) {
				jdbcConnectionAccess = new NonContextualJdbcConnectionAccess(
						getEventListenerManager(),
						factory.getServiceRegistry().getService( ConnectionProvider.class )
				);
			}
			else {
				jdbcConnectionAccess = new ContextualJdbcConnectionAccess(
						getTenantIdentifier(),
						getEventListenerManager(),
						factory.getServiceRegistry().getService( MultiTenantConnectionProvider.class )
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
		if ( useStreamForLobBinding == null ) {
			useStreamForLobBinding = Environment.useStreamsForBinary()
					|| getJdbcServices().getJdbcEnvironment().getDialect().useInputStreamToInsertBlob();
		}
		return useStreamForLobBinding;
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
						throw exceptionConverter.convert(
								e,
								"Error creating contextual LOB : " + e.getMessage()
						);
					}
				}
		);
	}

	@Override
	public SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if ( !sqlTypeDescriptor.canBeRemapped() ) {
			return sqlTypeDescriptor;
		}

		final Dialect dialect = getJdbcServices().getJdbcEnvironment().getDialect();
		final SqlTypeDescriptor remapped = dialect.remapSqlTypeDescriptor( sqlTypeDescriptor );
		return remapped == null ? sqlTypeDescriptor : remapped;
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
		checkTransactionSynchStatus();
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

		throw exceptionConverter.convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
	}

	protected QueryImplementor createQuery(NamedQueryDefinition queryDefinition) {
		String queryString = queryDefinition.getQueryString();
		final QueryImpl query = new QueryImpl(
				this,
				getQueryPlan( queryString, false ).getParameterMetadata(),
				queryString
		);
		query.setHibernateFlushMode( queryDefinition.getFlushMode() );
		query.setComment( queryDefinition.getComment() != null ? queryDefinition.getComment() : queryDefinition.getName() );
		if ( queryDefinition.getLockOptions() != null ) {
			query.setLockOptions( queryDefinition.getLockOptions() );
		}

		initQueryFromNamedDefinition( query, queryDefinition );
//		applyQuerySettingsAndHints( query );

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
		query.setComment( queryDefinition.getComment() != null ? queryDefinition.getComment() : queryDefinition.getName() );

		initQueryFromNamedDefinition( query, queryDefinition );
		applyQuerySettingsAndHints( query );

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
	}

	@Override
	public QueryImplementor createQuery(String queryString) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			final QueryImpl query = new QueryImpl(
					this,
					getQueryPlan( queryString, false ).getParameterMetadata(),
					queryString
			);
			query.setComment( queryString );
			applyQuerySettingsAndHints( query );
			return query;
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e );
		}
	}

	protected void applyQuerySettingsAndHints(Query query) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultClass) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			// do the translation
			final QueryImplementor<T> query = createQuery( queryString );
			resultClassChecking( resultClass, query );
			return query;
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
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
		final QueryImplementor<Object> query = buildQueryFromName( name, null );
		query.getParameterMetadata().setOrdinalParametersZeroBased( false );
		return query;
	}

	protected  <T> QueryImplementor<T> buildQueryFromName(String name, Class<T> resultType) {
		checkOpen();
		checkTransactionSynchStatus();
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

		throw exceptionConverter.convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
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
		if ( resultType != null ) {
			resultClassChecking( resultType, queryDefinition );
		}

		final NativeQueryImpl query = new NativeQueryImpl(
				queryDefinition,
				this,
				factory.getQueryPlanCache().getSQLParameterMetadata( queryDefinition.getQueryString(), false )
		);
		query.setHibernateFlushMode( queryDefinition.getFlushMode() );
		query.setComment( queryDefinition.getComment() != null ? queryDefinition.getComment() : queryDefinition.getName() );
		if ( queryDefinition.getLockOptions() != null ) {
			query.setLockOptions( queryDefinition.getLockOptions() );
		}

		initQueryFromNamedDefinition( query, queryDefinition );
		applyQuerySettingsAndHints( query );

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
				actualReturnedClass = getFactory().getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityClassName );
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
		final NativeQueryImpl query = (NativeQueryImpl) getNativeQueryImplementor( sqlString, false );
		query.setZeroBasedParametersIndex( false );
		return query;
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			NativeQueryImplementor query = createNativeQuery( sqlString );
			query.addEntity( "alias1", resultClass.getName(), LockMode.READ );
			return query;
		}
		catch ( RuntimeException he ) {
			throw exceptionConverter.convert( he );
		}
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			NativeQueryImplementor query = createNativeQuery( sqlString );
			query.setResultSetMapping( resultSetMapping );
			return query;
		}
		catch ( RuntimeException he ) {
			throw exceptionConverter.convert( he );
		}
	}

	@Override
	public NativeQueryImplementor getNamedNativeQuery(String name) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		final NamedSQLQueryDefinition nativeQueryDefinition = factory.getNamedQueryRepository().getNamedSQLQueryDefinition( name );
		if ( nativeQueryDefinition != null ) {
			return createNativeQuery( nativeQueryDefinition, true );
		}

		throw exceptionConverter.convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
	}

	@Override
	public NativeQueryImplementor createSQLQuery(String queryString) {
		return getNativeQueryImplementor( queryString, true );
	}

	protected NativeQueryImplementor getNativeQueryImplementor(
			String queryString,
			boolean isOrdinalParameterZeroBased) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			NativeQueryImpl query = new NativeQueryImpl(
					queryString,
					false,
					this,
					getFactory().getQueryPlanCache().getSQLParameterMetadata( queryString, isOrdinalParameterZeroBased )
			);
			query.setComment( "dynamic native SQL query" );
			return query;
		}
		catch ( RuntimeException he ) {
			throw exceptionConverter.convert( he );
		}
	}


	@Override
	public NativeQueryImplementor getNamedSQLQuery(String name) {
		final NativeQueryImpl nativeQuery = (NativeQueryImpl) getNamedNativeQuery( name );
		nativeQuery.setZeroBasedParametersIndex( true );
		return nativeQuery;
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
	public ExceptionConverter getExceptionConverter(){
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
		log.trace( "Serializing " + getClass().getSimpleName() + " [" );

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
		log.trace( "Deserializing " + getClass().getSimpleName() );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 1 :: read back non-transient state...
		ois.defaultReadObject();
		sessionEventsManager = new SessionEventListenerManagerImpl();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 2 :: read back transient state...
		//		-- see above

		factory = SessionFactoryImpl.deserialize( ois );
		jdbcSessionContext = new JdbcSessionContextImpl( this, (StatementInspector) ois.readObject() );
		jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, this );

		this.transactionCoordinator = factory.getServiceRegistry()
				.getService( TransactionCoordinatorBuilder.class )
				.buildTransactionCoordinator( jdbcCoordinator, this );

		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );
	}
}
