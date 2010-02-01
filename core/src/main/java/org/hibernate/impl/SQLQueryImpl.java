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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.MappingException;
import org.hibernate.LockOptions;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.query.ParameterMetadata;
import org.hibernate.engine.query.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.query.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.sql.NativeSQLQueryReturn;
import org.hibernate.type.Type;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.StringHelper;

/**
 * Implements SQL query passthrough.
 *
 * <pre>
 * <sql-query name="mySqlQuery">
 * <return alias="person" class="eg.Person"/>
 *   SELECT {person}.NAME AS {person.name}, {person}.AGE AS {person.age}, {person}.SEX AS {person.sex}
 *   FROM PERSON {person} WHERE {person}.NAME LIKE 'Hiber%'
 * </sql-query>
 * </pre>
 *
 * @author Max Andersen
 */
public class SQLQueryImpl extends AbstractQueryImpl implements SQLQuery {

	private final List queryReturns;
	private Collection querySpaces;
	private final boolean callable;
	private boolean autodiscovertypes;

	/**
	 * Constructs a SQLQueryImpl given a sql query defined in the mappings.
	 *
	 * @param queryDef The representation of the defined <sql-query/>.
	 * @param session The session to which this SQLQueryImpl belongs.
	 * @param parameterMetadata Metadata about parameters found in the query.
	 */
	SQLQueryImpl(NamedSQLQueryDefinition queryDef, SessionImplementor session, ParameterMetadata parameterMetadata) {
		super( queryDef.getQueryString(), queryDef.getFlushMode(), session, parameterMetadata );
		if ( queryDef.getResultSetRef() != null ) {
			ResultSetMappingDefinition definition = session.getFactory()
					.getResultSetMapping( queryDef.getResultSetRef() );
			if (definition == null) {
				throw new MappingException(
						"Unable to find resultset-ref definition: " +
						queryDef.getResultSetRef()
					);
			}
			this.queryReturns = Arrays.asList( definition.getQueryReturns() );
		}
		else if ( queryDef.getQueryReturns() != null && queryDef.getQueryReturns().length > 0 ) {
			this.queryReturns = Arrays.asList( queryDef.getQueryReturns() );
		}
		else {
			this.queryReturns = new ArrayList();
		}

		this.querySpaces = queryDef.getQuerySpaces();
		this.callable = queryDef.isCallable();
	}

	SQLQueryImpl(
			final String sql,
	        final List queryReturns,
	        final Collection querySpaces,
	        final FlushMode flushMode,
	        boolean callable,
	        final SessionImplementor session,
	        ParameterMetadata parameterMetadata) {
		// TODO : absolutely no usages of this constructor form; can it go away?
		super( sql, flushMode, session, parameterMetadata );
		this.queryReturns = queryReturns;
		this.querySpaces = querySpaces;
		this.callable = callable;
	}

	SQLQueryImpl(
			final String sql,
	        final String returnAliases[],
	        final Class returnClasses[],
	        final LockMode[] lockModes,
	        final SessionImplementor session,
	        final Collection querySpaces,
	        final FlushMode flushMode,
	        ParameterMetadata parameterMetadata) {
		// TODO : this constructor form is *only* used from constructor directly below us; can it go away?
		super( sql, flushMode, session, parameterMetadata );
		queryReturns = new ArrayList(returnAliases.length);
		for ( int i=0; i<returnAliases.length; i++ ) {
			NativeSQLQueryRootReturn ret = new NativeSQLQueryRootReturn(
					returnAliases[i],
					returnClasses[i].getName(),
					lockModes==null ? LockMode.NONE : lockModes[i]
			);
			queryReturns.add(ret);
		}
		this.querySpaces = querySpaces;
		this.callable = false;
	}

	SQLQueryImpl(
			final String sql,
	        final String returnAliases[],
	        final Class returnClasses[],
	        final SessionImplementor session,
	        ParameterMetadata parameterMetadata) {
		this( sql, returnAliases, returnClasses, null, session, null, null, parameterMetadata );
	}

	SQLQueryImpl(String sql, SessionImplementor session, ParameterMetadata parameterMetadata) {
		super( sql, null, session, parameterMetadata );
		queryReturns = new ArrayList();
		querySpaces = null;
		callable = false;
	}

	private static final NativeSQLQueryReturn[] NO_SQL_RETURNS = new NativeSQLQueryReturn[0];

	private NativeSQLQueryReturn[] getQueryReturns() {
		return ( NativeSQLQueryReturn[] ) queryReturns.toArray( NO_SQL_RETURNS );
	}

	public List list() throws HibernateException {
		verifyParameters();
		before();

		Map namedParams = getNamedParams();
		NativeSQLQuerySpecification spec = generateQuerySpecification( namedParams );

		try {
			return getSession().list( spec, getQueryParameters( namedParams ) );
		}
		finally {
			after();
		}
	}

	private NativeSQLQuerySpecification generateQuerySpecification(Map namedParams) {
		return new NativeSQLQuerySpecification(
		        expandParameterLists(namedParams),
		        getQueryReturns(),
		        querySpaces
		);
	}

	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		verifyParameters();
		before();

		Map namedParams = getNamedParams();
		NativeSQLQuerySpecification spec = generateQuerySpecification( namedParams );

		QueryParameters qp = getQueryParameters( namedParams );
		qp.setScrollMode( scrollMode );

		try {
			return getSession().scroll( spec, qp );
		}
		finally {
			after();
		}
	}

	public ScrollableResults scroll() throws HibernateException {
		return scroll(ScrollMode.SCROLL_INSENSITIVE);
	}

	public Iterator iterate() throws HibernateException {
		throw new UnsupportedOperationException("SQL queries do not currently support iteration");
	}

	public QueryParameters getQueryParameters(Map namedParams) {
		QueryParameters qp = super.getQueryParameters(namedParams);
		qp.setCallable(callable);
		qp.setAutoDiscoverScalarTypes(autodiscovertypes);
		return qp;
	}

	protected void verifyParameters() {
		verifyParameters( callable );
		boolean noReturns = queryReturns==null || queryReturns.isEmpty();
		if ( noReturns ) {
			this.autodiscovertypes = noReturns;
		}
		else {
			Iterator itr = queryReturns.iterator();
			while ( itr.hasNext() ) {
				NativeSQLQueryReturn rtn = ( NativeSQLQueryReturn ) itr.next();
				if ( rtn instanceof NativeSQLQueryScalarReturn ) {
					NativeSQLQueryScalarReturn scalar = ( NativeSQLQueryScalarReturn ) rtn;
					if ( scalar.getType() == null ) {
						autodiscovertypes = true;
						break;
					}
				}
			}
		}
	}

	public String[] getReturnAliases() throws HibernateException {
		throw new UnsupportedOperationException("SQL queries do not currently support returning aliases");
	}

	public Type[] getReturnTypes() throws HibernateException {
		throw new UnsupportedOperationException("not yet implemented for SQL queries");
	}

	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException("cannot set the lock mode for a native SQL query");
	}

	public Query setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException("cannot set lock options for a native SQL query");
	}

	public LockOptions getLockOptions() {
		//we never need to apply locks to the SQL
		return null;
	}

	public SQLQuery addScalar(String columnAlias, Type type) {
		queryReturns.add( new NativeSQLQueryScalarReturn( columnAlias, type ) );
		return this;
	}

	public SQLQuery addScalar(String columnAlias) {
		autodiscovertypes = true;
		queryReturns.add( new NativeSQLQueryScalarReturn( columnAlias, null ) );
		return this;
	}

	public SQLQuery addJoin(String alias, String path) {
		return addJoin(alias, path, LockMode.READ);
	}

	public SQLQuery addEntity(Class entityClass) {
		return addEntity( StringHelper.unqualify( entityClass.getName() ), entityClass );
	}

	public SQLQuery addEntity(String entityName) {
		return addEntity( StringHelper.unqualify( entityName ), entityName );
	}

	public SQLQuery addEntity(String alias, String entityName) {
		return addEntity(alias, entityName, LockMode.READ);
	}

	public SQLQuery addEntity(String alias, Class entityClass) {
		return addEntity( alias, entityClass.getName() );
	}

	public SQLQuery addJoin(String alias, String path, LockMode lockMode) {
		int loc = path.indexOf('.');
		if ( loc < 0 ) {
			throw new QueryException( "not a property path: " + path );
		}
		String ownerAlias = path.substring(0, loc);
		String role = path.substring(loc+1);
		queryReturns.add( new NativeSQLQueryJoinReturn(alias, ownerAlias, role, CollectionHelper.EMPTY_MAP, lockMode) );
		return this;
	}

	public SQLQuery addEntity(String alias, String entityName, LockMode lockMode) {
		queryReturns.add( new NativeSQLQueryRootReturn(alias, entityName, lockMode) );
		return this;
	}

	public SQLQuery addEntity(String alias, Class entityClass, LockMode lockMode) {
		return addEntity( alias, entityClass.getName(), lockMode );
	}

	public SQLQuery setResultSetMapping(String name) {
		ResultSetMappingDefinition mapping = session.getFactory().getResultSetMapping( name );
		if ( mapping == null ) {
			throw new MappingException( "Unknown SqlResultSetMapping [" + name + "]" );
		}
		NativeSQLQueryReturn[] returns = mapping.getQueryReturns();
		int length = returns.length;
		for ( int index = 0 ; index < length ; index++ ) {
			queryReturns.add( returns[index] );
		}
		return this;
	}

	public SQLQuery addSynchronizedQuerySpace(String querySpace) {
		if ( querySpaces == null ) {
			querySpaces = new ArrayList();
		}
		querySpaces.add( querySpace );
		return this;
	}

	public SQLQuery addSynchronizedEntityName(String entityName) {
		return addQuerySpaces( getSession().getFactory().getEntityPersister( entityName ).getQuerySpaces() );
	}

	public SQLQuery addSynchronizedEntityClass(Class entityClass) {
		return addQuerySpaces( getSession().getFactory().getEntityPersister( entityClass.getName() ).getQuerySpaces() );
	}

	private SQLQuery addQuerySpaces(Serializable[] spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new ArrayList();
			}
			for ( int i = 0; i < spaces.length; i++ ) {
				querySpaces.add( spaces[i] );
			}
		}
		return this;
	}

	public int executeUpdate() throws HibernateException {
		Map namedParams = getNamedParams();
		before();
		try {
			return getSession().executeNativeUpdate(
					generateQuerySpecification( namedParams ),
					getQueryParameters( namedParams )
			);
		}
		finally {
			after();
		}
	}

}
