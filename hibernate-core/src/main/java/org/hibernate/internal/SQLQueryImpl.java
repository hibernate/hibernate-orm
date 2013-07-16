/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.FetchReturnBuilder;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.query.spi.sql.ReturnBuilder;
import org.hibernate.engine.query.spi.sql.RootReturnBuilder;
import org.hibernate.engine.query.spi.sql.ScalarReturnBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.Type;

/**
 * Implementation of the {@link SQLQuery} contract.
 *
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class SQLQueryImpl extends AbstractQueryImpl implements SQLQuery {

	private List<NativeSQLQueryReturn> queryReturns;
	private List<ReturnBuilder> queryReturnBuilders;
	private boolean autoDiscoverTypes;

	private Collection<String> querySpaces;

	private final boolean callable;
	private final LockOptions lockOptions = new LockOptions();

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
			this.queryReturns = Arrays.asList( getResultSetMappingDefinition( session, queryDef.getResultSetRef() ).getQueryReturns() );
		}
		else if ( queryDef.getQueryReturns() != null && queryDef.getQueryReturns().length > 0 ) {
			this.queryReturns = Arrays.asList( queryDef.getQueryReturns() );
		}
		else {
			this.queryReturns = new ArrayList<NativeSQLQueryReturn>();
		}

		this.querySpaces = queryDef.getQuerySpaces();
		this.callable = queryDef.isCallable();
	}

	SQLQueryImpl(String sql, SessionImplementor session, ParameterMetadata parameterMetadata) {
		this( sql, false, session, parameterMetadata );
	}

	SQLQueryImpl(String sql, boolean callable, SessionImplementor session, ParameterMetadata parameterMetadata) {
		super( sql, null, session, parameterMetadata );
		this.queryReturns = new ArrayList<NativeSQLQueryReturn>();
		this.querySpaces = null;
		this.callable = callable;
	}

	@Override
	public List<NativeSQLQueryReturn>  getQueryReturns() {
		prepareQueryReturnsIfNecessary();
		return queryReturns;
	}

	@Override
	public Collection<String> getSynchronizedQuerySpaces() {
		return querySpaces;
	}

	@Override
	public boolean isCallable() {
		return callable;
	}

	@Override
	public List list() throws HibernateException {
		verifyParameters();
		before();

		Map<String, TypedValue> namedParams = getNamedParams();
		NativeSQLQuerySpecification spec = generateQuerySpecification( namedParams );

		try {
			return getSession().list( spec, getQueryParameters( namedParams ) );
		}
		finally {
			after();
		}
	}

	private static ResultSetMappingDefinition getResultSetMappingDefinition(SessionImplementor session, String resultSetMappingName){
		ResultSetMappingDefinition definition = session.getFactory().getResultSetMapping( resultSetMappingName );
		if ( definition == null ) {
			throw new MappingException( "Unable to find resultset-ref definition: " + resultSetMappingName );
		}
		return definition;
	}

	private NativeSQLQuerySpecification generateQuerySpecification(Map<String, TypedValue> namedParams) {
		return new NativeSQLQuerySpecification(
		        expandParameterLists(namedParams),
				queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] ),
		        querySpaces
		);
	}
	@Override
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		verifyParameters();
		before();

		Map<String, TypedValue> namedParams = getNamedParams();
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
	@Override
	public ScrollableResults scroll() throws HibernateException {
		return scroll(ScrollMode.SCROLL_INSENSITIVE);
	}
	@Override
	public Iterator iterate() throws HibernateException {
		throw new UnsupportedOperationException("SQL queries do not currently support iteration");
	}

	@Override
    public QueryParameters getQueryParameters(Map<String, TypedValue> namedParams) {
		QueryParameters qp = super.getQueryParameters(namedParams);
		qp.setCallable(callable);
		qp.setAutoDiscoverScalarTypes( autoDiscoverTypes );
		return qp;
	}

	@Override
    protected void verifyParameters() {
		// verifyParameters is called at the start of all execution type methods, so we use that here to perform
		// some preparation work.
		prepareQueryReturnsIfNecessary();
		verifyParameters( callable );
		if ( CollectionHelper.isEmpty( queryReturns ) ) {
			this.autoDiscoverTypes = true;
		}
		else {
			for ( final NativeSQLQueryReturn queryReturn : queryReturns ) {
				if ( queryReturn.getNature() != NativeSQLQueryReturn.Nature.SCALAR ) {
					continue;
				}
				NativeSQLQueryScalarReturn scalar = (NativeSQLQueryScalarReturn) queryReturn;
				if ( scalar.getType() == null ) {
					autoDiscoverTypes = true;
					break;
				}
			}
		}
	}

	private void prepareQueryReturnsIfNecessary() {
		if ( CollectionHelper.isNotEmpty( queryReturnBuilders ) ) {
			queryReturns = new ArrayList<NativeSQLQueryReturn>();
			for ( ReturnBuilder queryReturnBuilder : queryReturnBuilders ) {
				queryReturns.add( queryReturnBuilder.buildReturn() );
			}
			queryReturnBuilders = null;
		}
	}

	@Override
    public String[] getReturnAliases() throws HibernateException {
		throw new UnsupportedOperationException("SQL queries do not currently support returning aliases");
	}

	@Override
    public Type[] getReturnTypes() throws HibernateException {
		throw new UnsupportedOperationException("not yet implemented for SQL queries");
	}
	@Override
	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException("cannot set the lock mode for a native SQL query");
	}
	@Override
	public Query setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException("cannot set lock options for a native SQL query");
	}

	@Override
    public LockOptions getLockOptions() {
		//we never need to apply locks to the SQL, however the native-sql loader handles this specially
		return lockOptions;
	}
	@Override
	public SQLQuery addScalar(final String columnAlias, final Type type) {
		addReturnBuilder( new ScalarReturnBuilder(columnAlias, type) );
		return this;
	}
	@Override
	public SQLQuery addScalar(String columnAlias) {
		return addScalar( columnAlias, null );
	}
	@Override
	public RootReturn addRoot(String tableAlias, String entityName) {
		return addReturnBuilder( new RootReturnBuilder( tableAlias, entityName ) );
	}
	@Override
	public RootReturn addRoot(String tableAlias, Class entityType) {
		return addRoot( tableAlias, entityType.getName() );
	}
	@Override
	public SQLQuery addEntity(String entityName) {
		return addEntity( StringHelper.unqualify( entityName ), entityName );
	}
	@Override
	public SQLQuery addEntity(String alias, String entityName) {
		addRoot( alias, entityName );
		return this;
	}
	@Override
	public SQLQuery addEntity(String alias, String entityName, LockMode lockMode) {
		addRoot( alias, entityName ).setLockMode( lockMode );
		return this;
	}
	@Override
	public SQLQuery addEntity(Class entityType) {
		return addEntity( entityType.getName() );
	}
	@Override
	public SQLQuery addEntity(String alias, Class entityClass) {
		return addEntity( alias, entityClass.getName() );
	}
	@Override
	public SQLQuery addEntity(String alias, Class entityClass, LockMode lockMode) {
		return addEntity( alias, entityClass.getName(), lockMode );
	}
	@Override
	public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		return addReturnBuilder( new FetchReturnBuilder( tableAlias, ownerTableAlias, joinPropertyName ) );
	}

	private <T extends ReturnBuilder> T addReturnBuilder(T t){
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<ReturnBuilder>();
		}
		queryReturnBuilders.add( t );
		return t;
	}

	@Override
	public SQLQuery addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		addFetch( tableAlias, ownerTableAlias, joinPropertyName );
		return this;
	}
	@Override
	public SQLQuery addJoin(String alias, String path) {
		createFetchJoin( alias, path );
		return this;
	}

	private FetchReturn createFetchJoin(String tableAlias, String path) {
		int loc = path.indexOf('.');
		if ( loc < 0 ) {
			throw new QueryException( "not a property path: " + path );
		}
		final String ownerTableAlias = path.substring( 0, loc );
		final String joinedPropertyName = path.substring( loc+1 );
		return addFetch( tableAlias, ownerTableAlias, joinedPropertyName );
	}
	@Override
	public SQLQuery addJoin(String alias, String path, LockMode lockMode) {
		createFetchJoin( alias, path ).setLockMode( lockMode );
		return this;
	}
	@Override
	public SQLQuery setResultSetMapping(String name) {
		queryReturns.addAll( Arrays.asList( getResultSetMappingDefinition( session, name ).getQueryReturns() ) );
		return this;
	}

	@Override
	public SQLQuery addSynchronizedQuerySpace(String querySpace) {
		if ( querySpaces == null ) {
			querySpaces = new ArrayList<String>();
		}
		querySpaces.add( querySpace );
		return this;
	}
	       @Override
	public SQLQuery addSynchronizedEntityName(String entityName) {
		return addQuerySpaces( getSession().getFactory().getEntityPersister( entityName ).getQuerySpaces() );
	}
	 @Override
	public SQLQuery addSynchronizedEntityClass(Class entityClass) {
		return addQuerySpaces( getSession().getFactory().getEntityPersister( entityClass.getName() ).getQuerySpaces() );
	}

	private SQLQuery addQuerySpaces(Serializable[] spaces) {
		if ( spaces != null && spaces.length > 0) {
			if ( querySpaces == null ) {
				querySpaces = new ArrayList<String>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
		return this;
	}
	@Override
	public int executeUpdate() throws HibernateException {
		Map<String, TypedValue> namedParams = getNamedParams();
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
