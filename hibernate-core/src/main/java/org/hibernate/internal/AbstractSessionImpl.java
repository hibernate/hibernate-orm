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
import org.hibernate.SessionException;
import org.hibernate.SharedSessionContract;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.engine.jdbc.LobCreationContext;
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
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.type.Type;

/**
 * Functionality common to stateless and stateful sessions
 *
 * @author Gavin King
 */
public abstract class AbstractSessionImpl implements Serializable, SharedSessionContract,
													 SessionImplementor, TransactionContext {
	protected transient SessionFactoryImpl factory;
	private final String tenantIdentifier;
	private boolean closed = false;

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
		return closed;
	}

	protected void setClosed() {
		closed = true;
	}

	protected void errorIfClosed() {
		if ( closed ) {
			throw new SessionException( "Session is closed!" );
		}
	}

	@Override
	public Query getNamedQuery(String queryName) throws MappingException {
		errorIfClosed();
		NamedQueryDefinition nqd = factory.getNamedQuery( queryName );
		final Query query;
		if ( nqd != null ) {
			String queryString = nqd.getQueryString();
			query = new QueryImpl(
					queryString,
			        nqd.getFlushMode(),
			        this,
			        getHQLQueryPlan( queryString, false ).getParameterMetadata()
			);
			query.setComment( "named HQL query " + queryName );
			if ( nqd.getLockTimeout() != null ) {
				( (QueryImpl) query ).getLockOptions().setTimeOut( nqd.getLockTimeout() );
			}
		}
		else {
			NamedSQLQueryDefinition nsqlqd = factory.getNamedSQLQuery( queryName );
			if ( nsqlqd==null ) {
				throw new MappingException( "Named query not known: " + queryName );
			}
			ParameterMetadata parameterMetadata = factory.getQueryPlanCache().getSQLParameterMetadata( nsqlqd.getQueryString() );
			query = new SQLQueryImpl(
					nsqlqd,
			        this,
					parameterMetadata
			);
			query.setComment( "named native SQL query " + queryName );
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
		query.setCacheable( nqd.isCacheable() );
		query.setCacheRegion( nqd.getCacheRegion() );
		if ( nqd.getTimeout()!=null ) query.setTimeout( nqd.getTimeout().intValue() );
		if ( nqd.getFetchSize()!=null ) query.setFetchSize( nqd.getFetchSize().intValue() );
		if ( nqd.getCacheMode() != null ) query.setCacheMode( nqd.getCacheMode() );
		query.setReadOnly( nqd.isReadOnly() );
		if ( nqd.getComment() != null ) query.setComment( nqd.getComment() );
	}

	@Override
	public Query createQuery(String queryString) {
		errorIfClosed();
		QueryImpl query = new QueryImpl(
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
		SQLQueryImpl query = new SQLQueryImpl(
				sql,
		        this,
		        factory.getQueryPlanCache().getSQLParameterMetadata( sql )
		);
		query.setComment( "dynamic native SQL query" );
		return query;
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
		return new EntityKey( id, persister, getTenantIdentifier() );
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
						factory.getServiceRegistry().getService( ConnectionProvider.class )
				);
			}
			else {
				jdbcConnectionAccess = new ContextualJdbcConnectionAccess(
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
		private final ConnectionProvider connectionProvider;

		private NonContextualJdbcConnectionAccess(ConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			return connectionProvider.getConnection();
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			connectionProvider.closeConnection( connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}

	private class ContextualJdbcConnectionAccess implements JdbcConnectionAccess, Serializable {
		private final MultiTenantConnectionProvider connectionProvider;

		private ContextualJdbcConnectionAccess(MultiTenantConnectionProvider connectionProvider) {
			this.connectionProvider = connectionProvider;
		}

		@Override
		public Connection obtainConnection() throws SQLException {
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "Tenant identifier required!" );
			}
			return connectionProvider.getConnection( tenantIdentifier );
		}

		@Override
		public void releaseConnection(Connection connection) throws SQLException {
			if ( tenantIdentifier == null ) {
				throw new HibernateException( "Tenant identifier required!" );
			}
			connectionProvider.releaseConnection( tenantIdentifier, connection );
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return connectionProvider.supportsAggressiveRelease();
		}
	}
}
