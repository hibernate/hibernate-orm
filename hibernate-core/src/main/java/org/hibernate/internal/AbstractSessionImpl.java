/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.SharedSessionContract;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
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
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.type.Type;

/**
 * Functionality common to stateless and stateful sessions
 *
 * @author Gavin King
 */
public abstract class AbstractSessionImpl
		implements Serializable, SharedSessionContract, SessionImplementor, TransactionContext {
	protected transient SessionFactoryImpl factory;
	private final String tenantIdentifier;
	private boolean closed;

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

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public TransactionEnvironment getTransactionEnvironment() {
		return factory.getTransactionEnvironment();
	}

	@Override
	public <T> T execute(final LobCreationContext.Callback<T> callback) {
		return getTransactionCoordinator().getJdbcCoordinator().coordinateWork(
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
		final ParameterMetadata parameterMetadata = factory.getQueryPlanCache().getSQLParameterMetadata( namedQueryDefinition.getQueryString() );
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
		return factory.getQueryPlanCache().getHQLQueryPlan( query, shallow, getEnabledFilters() );
	}

	protected NativeSQLQueryPlan getNativeSQLQueryPlan(NativeSQLQuerySpecification spec) throws HibernateException {
		return factory.getQueryPlanCache().getNativeSQLQueryPlan( spec );
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

	@Override
	public CacheKey generateCacheKey(Serializable id, Type type, String entityOrRoleName) {
		return new CacheKey( id, type, entityOrRoleName, getTenantIdentifier(), getFactory() );
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
}
