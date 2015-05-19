/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.StringHelper;
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
			ResultSetMappingDefinition definition = session.getFactory()
					.getResultSetMapping( queryDef.getResultSetRef() );
			if ( definition == null ) {
				throw new MappingException(
						"Unable to find resultset-ref definition: " +
								queryDef.getResultSetRef()
				);
			}
			this.queryReturns = new ArrayList<NativeSQLQueryReturn>( Arrays.asList( definition.getQueryReturns() ) );
		}
		else if ( queryDef.getQueryReturns() != null && queryDef.getQueryReturns().length > 0 ) {
			this.queryReturns = new ArrayList<NativeSQLQueryReturn>( Arrays.asList( queryDef.getQueryReturns() ) );
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
	public List<NativeSQLQueryReturn> getQueryReturns() {
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
				expandParameterLists( namedParams ),
				queryReturns.toArray( new NativeSQLQueryReturn[queryReturns.size()] ),
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
		return scroll( session.getFactory().getDialect().defaultScrollMode() );
	}

	public Iterator iterate() throws HibernateException {
		throw new UnsupportedOperationException( "SQL queries do not currently support iteration" );
	}

	@Override
	public QueryParameters getQueryParameters(Map namedParams) {
		QueryParameters qp = super.getQueryParameters( namedParams );
		qp.setCallable( callable );
		qp.setAutoDiscoverScalarTypes( autoDiscoverTypes );
		return qp;
	}

	@Override
	protected void verifyParameters() {
		// verifyParameters is called at the start of all execution type methods, so we use that here to perform
		// some preparation work.
		prepareQueryReturnsIfNecessary();
		verifyParameters( callable );
		boolean noReturns = queryReturns == null || queryReturns.isEmpty();
		if ( noReturns ) {
			this.autoDiscoverTypes = noReturns;
		}
		else {
			for ( NativeSQLQueryReturn queryReturn : queryReturns ) {
				if ( queryReturn instanceof NativeSQLQueryScalarReturn ) {
					NativeSQLQueryScalarReturn scalar = (NativeSQLQueryScalarReturn) queryReturn;
					if ( scalar.getType() == null ) {
						autoDiscoverTypes = true;
						break;
					}
				}
				else if ( NativeSQLQueryConstructorReturn.class.isInstance( queryReturn ) ) {
					autoDiscoverTypes = true;
					break;
				}
			}
		}
	}

	private void prepareQueryReturnsIfNecessary() {
		if ( queryReturnBuilders != null ) {
			if ( !queryReturnBuilders.isEmpty() ) {
				if ( queryReturns != null ) {
					queryReturns.clear();
					queryReturns = null;
				}
				queryReturns = new ArrayList<NativeSQLQueryReturn>();
				for ( ReturnBuilder builder : queryReturnBuilders ) {
					queryReturns.add( builder.buildReturn() );
				}
				queryReturnBuilders.clear();
			}
			queryReturnBuilders = null;
		}
	}

	@Override
	public String[] getReturnAliases() throws HibernateException {
		throw new UnsupportedOperationException( "SQL queries do not currently support returning aliases" );
	}

	@Override
	public Type[] getReturnTypes() throws HibernateException {
		throw new UnsupportedOperationException( "not yet implemented for SQL queries" );
	}

	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException( "cannot set the lock mode for a native SQL query" );
	}

	public Query setLockOptions(LockOptions lockOptions) {
		throw new UnsupportedOperationException( "cannot set lock options for a native SQL query" );
	}

	@Override
	public LockOptions getLockOptions() {
		//we never need to apply locks to the SQL, however the native-sql loader handles this specially
		return lockOptions;
	}

	public SQLQuery addScalar(final String columnAlias, final Type type) {
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<ReturnBuilder>();
		}
		queryReturnBuilders.add(
				new ReturnBuilder() {
					public NativeSQLQueryReturn buildReturn() {
						return new NativeSQLQueryScalarReturn( columnAlias, type );
					}
				}
		);
		return this;
	}

	public SQLQuery addScalar(String columnAlias) {
		return addScalar( columnAlias, null );
	}

	public RootReturn addRoot(String tableAlias, String entityName) {
		RootReturnBuilder builder = new RootReturnBuilder( tableAlias, entityName );
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<ReturnBuilder>();
		}
		queryReturnBuilders.add( builder );
		return builder;
	}

	public RootReturn addRoot(String tableAlias, Class entityType) {
		return addRoot( tableAlias, entityType.getName() );
	}

	public SQLQuery addEntity(String entityName) {
		return addEntity( StringHelper.unqualify( entityName ), entityName );
	}

	public SQLQuery addEntity(String alias, String entityName) {
		addRoot( alias, entityName );
		return this;
	}

	public SQLQuery addEntity(String alias, String entityName, LockMode lockMode) {
		addRoot( alias, entityName ).setLockMode( lockMode );
		return this;
	}

	public SQLQuery addEntity(Class entityType) {
		return addEntity( entityType.getName() );
	}

	public SQLQuery addEntity(String alias, Class entityClass) {
		return addEntity( alias, entityClass.getName() );
	}

	public SQLQuery addEntity(String alias, Class entityClass, LockMode lockMode) {
		return addEntity( alias, entityClass.getName(), lockMode );
	}

	public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		FetchReturnBuilder builder = new FetchReturnBuilder( tableAlias, ownerTableAlias, joinPropertyName );
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<ReturnBuilder>();
		}
		queryReturnBuilders.add( builder );
		return builder;
	}

	public SQLQuery addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		addFetch( tableAlias, ownerTableAlias, joinPropertyName );
		return this;
	}

	public SQLQuery addJoin(String alias, String path) {
		createFetchJoin( alias, path );
		return this;
	}

	private FetchReturn createFetchJoin(String tableAlias, String path) {
		int loc = path.indexOf( '.' );
		if ( loc < 0 ) {
			throw new QueryException( "not a property path: " + path );
		}
		final String ownerTableAlias = path.substring( 0, loc );
		final String joinedPropertyName = path.substring( loc + 1 );
		return addFetch( tableAlias, ownerTableAlias, joinedPropertyName );
	}

	public SQLQuery addJoin(String alias, String path, LockMode lockMode) {
		createFetchJoin( alias, path ).setLockMode( lockMode );
		return this;
	}

	public SQLQuery setResultSetMapping(String name) {
		ResultSetMappingDefinition mapping = session.getFactory().getResultSetMapping( name );
		if ( mapping == null ) {
			throw new MappingException( "Unknown SqlResultSetMapping [" + name + "]" );
		}
		NativeSQLQueryReturn[] returns = mapping.getQueryReturns();
		queryReturns.addAll( Arrays.asList( returns ) );
		return this;
	}

	public SQLQuery addSynchronizedQuerySpace(String querySpace) {
		if ( querySpaces == null ) {
			querySpaces = new ArrayList<String>();
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
				querySpaces = new ArrayList<String>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
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

	private class RootReturnBuilder implements RootReturn, ReturnBuilder {
		private final String alias;
		private final String entityName;
		private LockMode lockMode = LockMode.READ;
		private Map<String, String[]> propertyMappings;

		private RootReturnBuilder(String alias, String entityName) {
			this.alias = alias;
			this.entityName = entityName;
		}

		public RootReturn setLockMode(LockMode lockMode) {
			this.lockMode = lockMode;
			return this;
		}

		public RootReturn setDiscriminatorAlias(String alias) {
			addProperty( "class", alias );
			return this;
		}

		public RootReturn addProperty(String propertyName, String columnAlias) {
			addProperty( propertyName ).addColumnAlias( columnAlias );
			return this;
		}

		public ReturnProperty addProperty(final String propertyName) {
			if ( propertyMappings == null ) {
				propertyMappings = new HashMap<String, String[]>();
			}
			return new ReturnProperty() {
				public ReturnProperty addColumnAlias(String columnAlias) {
					String[] columnAliases = propertyMappings.get( propertyName );
					if ( columnAliases == null ) {
						columnAliases = new String[] {columnAlias};
					}
					else {
						String[] newColumnAliases = new String[columnAliases.length + 1];
						System.arraycopy( columnAliases, 0, newColumnAliases, 0, columnAliases.length );
						newColumnAliases[columnAliases.length] = columnAlias;
						columnAliases = newColumnAliases;
					}
					propertyMappings.put( propertyName, columnAliases );
					return this;
				}
			};
		}

		public NativeSQLQueryReturn buildReturn() {
			return new NativeSQLQueryRootReturn( alias, entityName, propertyMappings, lockMode );
		}
	}

	private class FetchReturnBuilder implements FetchReturn, ReturnBuilder {
		private final String alias;
		private String ownerTableAlias;
		private final String joinedPropertyName;
		private LockMode lockMode = LockMode.READ;
		private Map<String, String[]> propertyMappings;

		private FetchReturnBuilder(String alias, String ownerTableAlias, String joinedPropertyName) {
			this.alias = alias;
			this.ownerTableAlias = ownerTableAlias;
			this.joinedPropertyName = joinedPropertyName;
		}

		public FetchReturn setLockMode(LockMode lockMode) {
			this.lockMode = lockMode;
			return this;
		}

		public FetchReturn addProperty(String propertyName, String columnAlias) {
			addProperty( propertyName ).addColumnAlias( columnAlias );
			return this;
		}

		public ReturnProperty addProperty(final String propertyName) {
			if ( propertyMappings == null ) {
				propertyMappings = new HashMap<String, String[]>();
			}
			return new ReturnProperty() {
				public ReturnProperty addColumnAlias(String columnAlias) {
					String[] columnAliases = propertyMappings.get( propertyName );
					if ( columnAliases == null ) {
						columnAliases = new String[] {columnAlias};
					}
					else {
						String[] newColumnAliases = new String[columnAliases.length + 1];
						System.arraycopy( columnAliases, 0, newColumnAliases, 0, columnAliases.length );
						newColumnAliases[columnAliases.length] = columnAlias;
						columnAliases = newColumnAliases;
					}
					propertyMappings.put( propertyName, columnAliases );
					return this;
				}
			};
		}

		public NativeSQLQueryReturn buildReturn() {
			return new NativeSQLQueryJoinReturn(
					alias,
					ownerTableAlias,
					joinedPropertyName,
					propertyMappings,
					lockMode
			);
		}
	}

	private interface ReturnBuilder {
		NativeSQLQueryReturn buildReturn();
	}
}
