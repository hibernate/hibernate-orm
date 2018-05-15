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
import java.util.TimeZone;
import java.util.UUID;
import javax.persistence.FlushModeType;
import javax.persistence.TransactionRequiredException;

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
import org.hibernate.cache.spi.CacheTransactionSynchronization;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.internal.SessionEventListenerManagerImpl;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.query.Query;
import org.hibernate.query.named.spi.NamedCallableQueryMemento;
import org.hibernate.query.named.spi.NamedHqlQueryMemento;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryPlanCache;
import org.hibernate.query.spi.ResultSetMappingDescriptor;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Base class for SharedSessionContract/SharedSessionContractImplementor
 * implementations.  Intended for Session and StatelessSession implementations
 * <p/>
 * NOTE: This implementation defines access to a number of instance state values
 * in a manner that is not exactly concurrent-access safe.  However, a Session/EntityManager
 * is never intended to be used concurrently; therefore the condition is not expected
 * and so a more synchronized/concurrency-safe is not defined to be as negligent
 * (performance-wise) as possible.  Some of these methods include:<ul>
 * <li>{@link #getEventListenerManager()}</li>
 * <li>{@link #getJdbcConnectionAccess()}</li>
 * <li>{@link #getJdbcServices()}</li>
 * <li>{@link #getTransaction()} (and therefore related methods such as {@link #beginTransaction()}, etc)</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractSharedSessionContract implements SharedSessionContractImplementor {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( SessionImpl.class );

	private transient SessionFactoryImpl factory;
	private final String tenantIdentifier;
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


	// NOTE : these fields are transient & non-final for Serialization purposes - ugh
	private transient EntityNameResolver entityNameResolver;

	private transient SessionEventListenerManagerImpl sessionEventsManager = new SessionEventListenerManagerImpl();
	protected transient ExceptionConverter exceptionConverter;

	private transient Boolean useStreamForLobBinding;
	private Integer jdbcBatchSize;


	public AbstractSharedSessionContract(SessionFactoryImpl factory, SessionCreationOptions options) {
		this.factory = factory;
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
				throw new HibernateException(
						"SessionFactory configured for multi-tenancy, but no tenant identifier specified" );
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
			if ( sharedOptions.getPhysicalConnectionHandlingMode() != this.jdbcCoordinator.getLogicalConnection()
					.getConnectionHandlingMode() ) {
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
		return statementInspector;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return this;
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
		catch (HibernateException e) {
			if ( getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
				throw this.exceptionConverter.convert( e );
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
	public void prepareForQueryExecution(boolean requiresTxn) {
		checkOpen();
		checkTransactionSynchStatus();

		if ( requiresTxn && !isTransactionInProgress() ) {
			throw new TransactionRequiredException(
					"Query requires transaction be in progress, but no transaction is known to be in progress"
			);
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
	public Transaction getTransaction() throws HibernateException {
		if ( getFactory().getSessionFactoryOptions().getJpaCompliance().isJpaTransactionComplianceEnabled() ) {
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
		if ( this.currentHibernateTransaction == null ) {
			this.currentHibernateTransaction = new TransactionImpl(
					getTransactionCoordinator(),
					getExceptionConverter(),
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
		checkTransactionSynchStatus();
		return jdbcCoordinator.getLogicalConnection().isOpen();
	}

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		// See class-level JavaDocs for a discussion of the concurrent-access safety of this method
		if ( jdbcConnectionAccess == null ) {
			if ( !factory.getSettings().getMultiTenancyStrategy().requiresMultiTenantConnectionProvider() ) {
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
	public EntityKey generateEntityKey(Object id, EntityTypeDescriptor descriptor) {
		return new EntityKey( id, descriptor );
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

	@Override
	public boolean isDefaultReadOnly() {
		return getPersistenceContext().isDefaultReadOnly();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// dynamic HQL handling

	@Override
	public QueryImplementor createQuery(String queryString) {
		return createQuery( queryString, null );
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> createQuery(String queryString, Class<T> resultClass) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			final QueryEngine queryEngine = getFactory().getQueryEngine();
			final QueryPlanCache queryPlanCache = queryEngine.getQueryPlanCache();
			SqmStatement sqm = queryPlanCache.getSqmStatement( queryString );
			if ( sqm == null ) {
				sqm = queryEngine.getSemanticQueryProducer().interpret( queryString );
				queryPlanCache.cacheSqmStatement( queryString, sqm );
			}

			final QuerySqmImpl query = new QuerySqmImpl(
					queryString,
					sqm,
					resultClass,
					this
			);

			query.setComment( queryString );
			applyQuerySettingsAndHints( query );

			return query;
		}
		catch (RuntimeException e) {
			markForRollbackOnly();
			throw exceptionConverter.convert( e );
		}
	}

	protected void applyQuerySettingsAndHints(Query query) {
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// dynamic native (SQL) query handling

	protected NativeQueryImplementor getNativeQueryImplementor(
			String queryString,
			boolean isOrdinalParameterZeroBased) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			NativeQueryImpl query = new NativeQueryImpl( queryString, this );
			query.setComment( "dynamic native SQL query" );
			return query;
		}
		catch (RuntimeException he) {
			throw exceptionConverter.convert( he );
		}
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString) {
		final NativeQueryImpl query = (NativeQueryImpl) getNativeQueryImplementor( sqlString, false );
		return query;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor createNativeQuery(String sqlString, Class resultClass) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		try {
			NativeQueryImplementor query = createNativeQuery( sqlString );
			query.addEntity( "alias1", resultClass.getName(), LockMode.READ );
			return query;
		}
		catch (RuntimeException he) {
			throw exceptionConverter.convert( he );
		}
	}

	@Override
	public NativeQueryImplementor createNativeQuery(String sqlString, String resultSetMapping) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		final NativeQueryImplementor query;
		try {
			if ( StringHelper.isNotEmpty( resultSetMapping ) ) {
				final ResultSetMappingDescriptor resultSetMappingDescriptor = getFactory().getQueryEngine()
						.getNamedQueryRepository()
						.getResultSetMappingDescriptor( resultSetMapping );

				if ( resultSetMappingDescriptor == null ) {
					throw new HibernateException( "Could not resolve specified result-set mapping name : " + resultSetMapping );
				}

				query = new NativeQueryImpl( sqlString, resultSetMappingDescriptor, this );
			}
			else {
				query = new NativeQueryImpl( sqlString, this );
			}
		}
		catch (RuntimeException he) {
			throw exceptionConverter.convert( he );
		}

		return query;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// procedure/funtion call support

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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// named query handling

	@Override
	public QueryImplementor getNamedQuery(String queryName) {
		return buildNamedQuery( queryName, null );
	}

	@SuppressWarnings("unchecked")
	protected <T> QueryImplementor<T> buildNamedQuery(String queryName, Class<T> resultType) {
		checkOpen();
		checkTransactionSynchStatus();
		delayedAfterCompletion();

		// this method can be called for either a named HQL query or a named native query

		// first see if it is a named HQL query
		final NamedHqlQueryMemento namedHqlDescriptor = getFactory().getQueryEngine()
				.getNamedQueryRepository()
				.getHqlQueryMemento( queryName );

		if ( namedHqlDescriptor != null ) {
			return namedHqlDescriptor.toQuery( this, resultType );
		}

		// otherwise, see if it is a named native query
		final NamedNativeQueryMemento namedNativeDescriptor = getFactory().getQueryEngine()
				.getNamedQueryRepository()
				.getNativeQueryMemento( queryName );

		if ( namedNativeDescriptor != null ) {
			return namedNativeDescriptor.toQuery( this, resultType );
		}

		// todo (6.0) : allow this for named stored procedures as well?
		//		ultimately they are treated as a Query

		throw exceptionConverter.convert( new IllegalArgumentException( "No query defined for that name [" + queryName + "]" ) );
	}

	@Override
	public QueryImplementor createNamedQuery(String name) {
		return buildNamedQuery( name, null );
	}

	@Override
	public <R> QueryImplementor<R> createNamedQuery(String name, Class<R> resultClass) {
		return buildNamedQuery( name, resultClass );
	}

	@Override
	public NativeQueryImplementor getNamedNativeQuery(String name) {
		checkOpen();

		try {
			checkTransactionSynchStatus();
			delayedAfterCompletion();

			final NamedNativeQueryMemento namedQueryDescriptor = factory.getQueryEngine().getNamedQueryRepository()
					.getNativeQueryMemento( name );
			if ( namedQueryDescriptor != null ) {
				return namedQueryDescriptor.toQuery( this, null );
			}

			throw exceptionConverter.convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
		}
		catch (RuntimeException e) {
			throw !( e instanceof IllegalArgumentException ) ? new IllegalArgumentException( e ) : e;
		}
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall getNamedProcedureCall(String name) {
		checkOpen();

		final NamedCallableQueryMemento memento = factory.getQueryEngine()
				.getNamedQueryRepository()
				.getCallableQueryMemento( name );
		if ( memento == null ) {
			throw new IllegalArgumentException(
					"Could not find named stored procedure call with that registration name : " + name
			);
		}
		final ProcedureCall procedureCall = memento.toQuery( this, null );
//		procedureCall.setComment( "Named stored procedure call [" + name + "]" );
		return procedureCall;
	}

	protected abstract Object load(String entityName, Object identifier);

	@Override
	public ExceptionConverter getExceptionConverter() {
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
		sessionEventsManager = new SessionEventListenerManagerImpl();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Step 2 :: read back transient state...
		//		-- see above

		factory = SessionFactoryImpl.deserialize( ois );
		jdbcSessionContext = new JdbcSessionContextImpl( this, (StatementInspector) ois.readObject() );
		jdbcCoordinator = JdbcCoordinatorImpl.deserialize( ois, this );

		cacheTransactionSync = factory.getCache().getRegionFactory().createTransactionContext( this );

		transactionCoordinator = factory.getServiceRegistry()
				.getService( TransactionCoordinatorBuilder.class )
				.buildTransactionCoordinator( jdbcCoordinator, this );

		entityNameResolver = new CoordinatingEntityNameResolver( factory, interceptor );
		exceptionConverter = new ExceptionConverterImpl( this );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	public AllowableParameterType resolveParameterBindType(Object bindValue) {
		if ( bindValue == null ) {
			return getSessionFactory().getTypeConfiguration()
					.getBasicTypeRegistry()
					.getBasicType( Serializable.class );
		}
		else {
			return resolveParameterBindType( bindValue.getClass() );
		}
	}

	@Override
	public AllowableParameterType resolveParameterBindType(Class javaType) {
		return getSessionFactory().getTypeConfiguration()
				.getBasicTypeRegistry()
				.getBasicType( javaType );
	}
}
