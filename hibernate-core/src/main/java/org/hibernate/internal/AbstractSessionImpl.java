/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.SharedSessionContract;
import org.hibernate.Transaction;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.ConnectionObserver;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.resource.jdbc.spi.JdbcObserver;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder.TransactionCoordinatorOptions;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.service.ServiceRegistry;

/**
 * Functionality common to stateless and stateful sessions
 *
 * @author Gavin King
 */
public abstract class AbstractSessionImpl
		implements Serializable, SharedSessionContract, SessionImplementor, JdbcSessionOwner, TransactionCoordinatorOptions {
	protected transient SessionFactoryImpl factory;
	private final String tenantIdentifier;
	private boolean closed;

	protected transient Transaction currentHibernateTransaction;

	protected AbstractSessionImpl(SessionFactoryImpl factory, String tenantIdentifier) {
		this.factory = factory;
		this.tenantIdentifier = tenantIdentifier;
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
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public abstract boolean shouldAutoJoinTransaction();

	@Override
	public <T> T execute(final LobCreationContext.Callback<T> callback) {
		return getJdbcCoordinator().coordinateWork(
				new WorkExecutorVisitable<T>() {
					@Override
					public T accept(WorkExecutor<T> workExecutor, Connection connection) throws SQLException {
						try {
							return callback.executeOnConnection( connection );
						}
						catch (SQLException e) {
							throw getFactory().getSQLExceptionHelper().convert(
									e,
									"Error creating contextual LOB : " + e.getMessage()
							);
						}
					}
				}
		);
	}

	@Override
	public boolean isClosed() {
		return closed || factory.isClosed();
	}

	protected void setClosed() {
		closed = true;
	}

	protected void errorIfClosed() {
		if ( isClosed() ) {
			throw new SessionException( "Session is closed!" );
		}
	}

	@Override
	public Query createQuery(NamedQueryDefinition namedQueryDefinition) {
		String queryString = namedQueryDefinition.getQueryString();
		final Query query = new QueryImpl(
				queryString,
				namedQueryDefinition.getFlushMode(),
				this,
				getHQLQueryPlan( queryString, false ).getParameterMetadata()
		);
		query.setComment( "named HQL query " + namedQueryDefinition.getName() );
		if ( namedQueryDefinition.getLockOptions() != null ) {
			query.setLockOptions( namedQueryDefinition.getLockOptions() );
		}

		return query;
	}

	@Override
	public SQLQuery createSQLQuery(NamedSQLQueryDefinition namedQueryDefinition) {
		final ParameterMetadata parameterMetadata = factory.getQueryPlanCache().getSQLParameterMetadata(
				namedQueryDefinition.getQueryString()
		);
		final SQLQuery query = new SQLQueryImpl(
				namedQueryDefinition,
				this,
				parameterMetadata
		);
		query.setComment( "named native SQL query " + namedQueryDefinition.getName() );
		return query;
	}

	@Override
	public Query getNamedQuery(String queryName) throws MappingException {
		errorIfClosed();
		NamedQueryDefinition nqd = factory.getNamedQuery( queryName );
		final Query query;
		if ( nqd != null ) {
			query = createQuery( nqd );
		}
		else {
			NamedSQLQueryDefinition nsqlqd = factory.getNamedSQLQuery( queryName );
			if ( nsqlqd==null ) {
				throw new MappingException( "Named query not known: " + queryName );
			}

			query = createSQLQuery( nsqlqd );
			nqd = nsqlqd;
		}
		initQuery( query, nqd );
		return query;
	}

	@Override
	public Query getNamedSQLQuery(String queryName) throws MappingException {
		errorIfClosed();
		NamedSQLQueryDefinition nsqlqd = factory.getNamedSQLQuery( queryName );
		if ( nsqlqd==null ) {
			throw new MappingException( "Named SQL query not known: " + queryName );
		}
		Query query = new SQLQueryImpl(
				nsqlqd,
				this,
				factory.getQueryPlanCache().getSQLParameterMetadata( nsqlqd.getQueryString() )
		);
		query.setComment( "named native SQL query " + queryName );
		initQuery( query, nsqlqd );
		return query;
	}

	private void initQuery(Query query, NamedQueryDefinition nqd) {
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
			query.setFlushMode( nqd.getFlushMode() );
		}
	}

	@Override
	public Query createQuery(String queryString) {
		errorIfClosed();
		final QueryImpl query = new QueryImpl(
				queryString,
				this,
				getHQLQueryPlan( queryString, false ).getParameterMetadata()
		);
		query.setComment( queryString );
		return query;
	}

	@Override
	public SQLQuery createSQLQuery(String sql) {
		errorIfClosed();
		final SQLQueryImpl query = new SQLQueryImpl(
				sql,
				this,
				factory.getQueryPlanCache().getSQLParameterMetadata( sql )
		);
		query.setComment( "dynamic native SQL query" );
		return query;
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall getNamedProcedureCall(String name) {
		errorIfClosed();

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
		errorIfClosed();
		final ProcedureCall procedureCall = new ProcedureCallImpl( this, procedureName );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		errorIfClosed();
		final ProcedureCall procedureCall = new ProcedureCallImpl( this, procedureName, resultClasses );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	@Override
	@SuppressWarnings("UnnecessaryLocalVariable")
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		errorIfClosed();
		final ProcedureCall procedureCall = new ProcedureCallImpl( this, procedureName, resultSetMappings );
//		call.setComment( "Dynamic stored procedure call" );
		return procedureCall;
	}

	protected HQLQueryPlan getHQLQueryPlan(String query, boolean shallow) throws HibernateException {
		return factory.getQueryPlanCache().getHQLQueryPlan( query, shallow, getLoadQueryInfluencers().getEnabledFilters() );
	}

	protected NativeSQLQueryPlan getNativeSQLQueryPlan(NativeSQLQuerySpecification spec) throws HibernateException {
		return factory.getQueryPlanCache().getNativeSQLQueryPlan( spec );
	}

	@Override
	public Transaction getTransaction() throws HibernateException {
		errorIfClosed();
		if ( this.currentHibernateTransaction == null || this.currentHibernateTransaction.getStatus() != TransactionStatus.ACTIVE ) {
			this.currentHibernateTransaction = new TransactionImpl( getTransactionCoordinator() );
		}
		getTransactionCoordinator().pulse();
		return currentHibernateTransaction;
	}

	@Override
	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException {
		return listCustomQuery( getNativeSQLQueryPlan( spec ).getCustomQuery(), queryParameters );
	}

	@Override
	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException {
		return scrollCustomQuery( getNativeSQLQueryPlan( spec ).getCustomQuery(), queryParameters );
	}

	@Override
	public String getTenantIdentifier() {
		return tenantIdentifier;
	}

	@Override
	public EntityKey generateEntityKey(Serializable id, EntityPersister persister) {
		return new EntityKey( id, persister );
	}

	private transient JdbcConnectionAccess jdbcConnectionAccess;

	@Override
	public JdbcConnectionAccess getJdbcConnectionAccess() {
		if ( jdbcConnectionAccess == null ) {
			if ( MultiTenancyStrategy.NONE == factory.getSettings().getMultiTenancyStrategy() ) {
				jdbcConnectionAccess = new NonContextualJdbcConnectionAccess(
						getEventListenerManager(),
						factory.getServiceRegistry().getService( ConnectionProvider.class )
				);
			}
			else {
				jdbcConnectionAccess = new ContextualJdbcConnectionAccess(
						getEventListenerManager(),
						factory.getServiceRegistry().getService( MultiTenantConnectionProvider.class )
				);
			}
		}
		return jdbcConnectionAccess;
	}

	private UUID sessionIdentifier;

	public UUID getSessionIdentifier() {
		if ( sessionIdentifier == null ) {
			sessionIdentifier = StandardRandomStrategy.INSTANCE.generateUUID( this );
		}
		return sessionIdentifier;
	}

	private static class NonContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
		private final SessionEventListener listener;
		private final ConnectionProvider connectionProvider;

		private NonContextualJdbcConnectionAccess(
				SessionEventListener listener,
				ConnectionProvider connectionProvider) {
			this.listener = listener;
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			try {
				listener.jdbcConnectionAcquisitionStart();
				return connectionProvider.getConnection();
			}
			finally {
				listener.jdbcConnectionAcquisitionEnd();
			}
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			try {
				listener.jdbcConnectionReleaseStart();
				connectionProvider.closeConnection( connection );
			}
			finally {
				listener.jdbcConnectionReleaseEnd();
			}
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	private class ContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
		private final SessionEventListener listener;
		private final MultiTenantConnectionProvider connectionProvider;

		private ContextualJdbcConnectionAccess(
				SessionEventListener listener,
				MultiTenantConnectionProvider connectionProvider) {
			this.listener = listener;
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "Tenant identifier required!" );
			}

			try {
				listener.jdbcConnectionAcquisitionStart();
				return connectionProvider.getConnection( tenantIdentifier );
			}
			finally {
				listener.jdbcConnectionAcquisitionEnd();
			}
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "Tenant identifier required!" );
			}

			try {
				listener.jdbcConnectionReleaseStart();
				connectionProvider.releaseConnection( tenantIdentifier, connection );
			}
			finally {
				listener.jdbcConnectionReleaseEnd();
			}
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	public class JdbcSessionContextImpl implements JdbcSessionContext {
		private final SessionFactoryImpl sessionFactory;
		private final StatementInspector inspector;
		private final transient ServiceRegistry serviceRegistry;
		private final transient JdbcObserver jdbcObserver;

		public JdbcSessionContextImpl(SessionFactoryImpl sessionFactory, StatementInspector inspector) {
			this.sessionFactory = sessionFactory;
			this.inspector = inspector;
			this.serviceRegistry = sessionFactory.getServiceRegistry();
			this.jdbcObserver = new JdbcObserverImpl();

			if ( inspector == null ) {
				throw new IllegalArgumentException( "StatementInspector cannot be null" );
			}
		}

		@Override
		public boolean isScrollableResultSetsEnabled() {
			return settings().isScrollableResultSetsEnabled();
		}

		@Override
		public boolean isGetGeneratedKeysEnabled() {
			return settings().isGetGeneratedKeysEnabled();
		}

		@Override
		public int getFetchSize() {
			return settings().getJdbcFetchSize();
		}

		@Override
		public ConnectionReleaseMode getConnectionReleaseMode() {
			return settings().getConnectionReleaseMode();
		}

		@Override
		public ConnectionAcquisitionMode getConnectionAcquisitionMode() {
			return ConnectionAcquisitionMode.DEFAULT;
		}

		@Override
		public StatementInspector getStatementInspector() {
			return inspector;
		}

		@Override
		public JdbcObserver getObserver() {
			return this.jdbcObserver;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return this.sessionFactory;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return this.serviceRegistry;
		}

		private SessionFactoryOptions settings() {
			return this.sessionFactory.getSessionFactoryOptions();
		}
	}

	public class JdbcObserverImpl implements JdbcObserver {

		private final transient List<ConnectionObserver> observers;

		public JdbcObserverImpl() {
			this.observers = new ArrayList<ConnectionObserver>();
			this.observers.add( new ConnectionObserverStatsBridge( factory ) );
		}

		@Override
		public void jdbcConnectionAcquisitionStart() {

		}

		@Override
		public void jdbcConnectionAcquisitionEnd(Connection connection) {
			for ( ConnectionObserver observer : observers ) {
				observer.physicalConnectionObtained( connection );
			}
		}

		@Override
		public void jdbcConnectionReleaseStart() {

		}

		@Override
		public void jdbcConnectionReleaseEnd() {
			for ( ConnectionObserver observer : observers ) {
				observer.physicalConnectionReleased();
			}
		}

		@Override
		public void jdbcPrepareStatementStart() {
			getEventListenerManager().jdbcPrepareStatementStart();
		}

		@Override
		public void jdbcPrepareStatementEnd() {
			for ( ConnectionObserver observer : observers ) {
				observer.statementPrepared();
			}
			getEventListenerManager().jdbcPrepareStatementEnd();
		}

		@Override
		public void jdbcExecuteStatementStart() {
			getEventListenerManager().jdbcExecuteStatementStart();
		}

		@Override
		public void jdbcExecuteStatementEnd() {
			getEventListenerManager().jdbcExecuteStatementEnd();
		}

		@Override
		public void jdbcExecuteBatchStart() {
			getEventListenerManager().jdbcExecuteBatchStart();
		}

		@Override
		public void jdbcExecuteBatchEnd() {
			getEventListenerManager().jdbcExecuteBatchEnd();
		}
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return factory.getServiceRegistry().getService( TransactionCoordinatorBuilder.class );
	}

}
