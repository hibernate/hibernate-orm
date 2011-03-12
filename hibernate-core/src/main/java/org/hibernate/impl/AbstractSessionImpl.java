/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.impl;

import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionException;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.query.HQLQueryPlan;
import org.hibernate.engine.query.NativeSQLQueryPlan;

import java.util.List;

/**
 * Functionality common to stateless and stateful sessions
 * 
 * @author Gavin King
 */
public abstract class AbstractSessionImpl implements SessionImplementor {

	protected transient SessionFactoryImpl factory;
	private boolean closed = false;

	protected AbstractSessionImpl(SessionFactoryImpl factory) {
		this.factory = factory;
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

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
		}
		else {
			NamedSQLQueryDefinition nsqlqd = factory.getNamedSQLQuery( queryName );
			if ( nsqlqd==null ) {
				throw new MappingException( "Named query not known: " + queryName );
			}
			query = new SQLQueryImpl(
					nsqlqd,
			        this,
			        factory.getQueryPlanCache().getSQLParameterMetadata( nsqlqd.getQueryString() )
			);
			query.setComment( "named native SQL query " + queryName );
			nqd = nsqlqd;
		}
		initQuery( query, nqd );
		return query;
	}

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

	public List list(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException {
		return listCustomQuery( getNativeSQLQueryPlan( spec ).getCustomQuery(), queryParameters );
	}

	public ScrollableResults scroll(NativeSQLQuerySpecification spec, QueryParameters queryParameters)
			throws HibernateException {
		return scrollCustomQuery( getNativeSQLQueryPlan( spec ).getCustomQuery(), queryParameters );
	}

}
