/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractQuery;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.NativeQueryInterpreter;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryInterpretations;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NativeQueryImpl<R> extends AbstractQuery<R> implements NativeQueryImplementor<R> {
	private static final Logger log = Logger.getLogger( NativeQueryImpl.class );

	private final String sqlString;

	private List<NativeSQLQueryReturn> queryReturns;
	private List<NativeQueryReturnBuilder> queryReturnBuilders;
	private boolean autoDiscoverTypes;

	private final boolean hasMainOutputParameter;
	private final ParameterMetadataImpl parameterMetadata;
	private final QueryParameterBindings parameterBindings;

	private final List<JdbcParameterBinder> parameterBinders;

	private Collection<String> querySpaces;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
	private final boolean callable;

	private Serializable collectionKey;


	/**
	 * Constructs a NativeQueryImpl given a sql query defined in the mappings.
	 *
	 * @param queryDef The representation of the defined <sql-query/>.
	 * @param session The session to which this NativeQuery belongs.
	 */
	public NativeQueryImpl(
			NamedSQLQueryDefinition queryDef,
			SharedSessionContractImplementor session) {
		this(
				session,
				queryDef.getQueryString(),
				queryDef.isCallable(),
				queryDef.getQuerySpaces(),
				collectQueryReturns( queryDef, session )
		);
	}

	private NativeQueryImpl(
			SharedSessionContractImplementor session,
			String queryString,
			boolean callable,
			List<String> querySpaces,
			List<NativeSQLQueryReturn> queryReturns) {
		super( session );

		this.sqlString = queryString;
		this.callable = callable;
		this.querySpaces = querySpaces;

		this.queryReturns = queryReturns;

		final ParameterRecognizerImpl parameterRecognizer = new ParameterRecognizerImpl( session.getFactory() );
		session.getFactory().getServiceRegistry()
				.getService( NativeQueryInterpreter.class )
				.recognizeParameters( sqlString, parameterRecognizer );
		parameterRecognizer.validate();
		// todo : validate hasMainOutputParameter against callable?
		this.hasMainOutputParameter = parameterRecognizer.hadMainOutputParameter();
		this.parameterMetadata = new ParameterMetadataImpl(
				parameterRecognizer.getNamedQueryParameters(),
				parameterRecognizer.getPositionalQueryParameters()
		);
		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		this.parameterBinders = parameterRecognizer.getParameterBinders();
	}

	private static List<NativeSQLQueryReturn> collectQueryReturns(
			NamedSQLQueryDefinition queryDef,
			SharedSessionContractImplementor session) {
		if ( queryDef.getResultSetRef() != null ) {
			ResultSetMappingDefinition definition = session.getFactory()
					.getNamedQueryRepository()
					.getResultSetMappingDefinition( queryDef.getResultSetRef() );
			if ( definition == null ) {
				throw new MappingException(
						"Unable to find resultset-ref definition: " +
								queryDef.getResultSetRef()
				);
			}
			return new ArrayList<>( Arrays.asList( definition.getQueryReturns() ) );
		}
		else if ( queryDef.getQueryReturns() != null && queryDef.getQueryReturns().length > 0 ) {
			return new ArrayList<>( Arrays.asList( queryDef.getQueryReturns() ) );
		}
		else {
			return new ArrayList<>();
		}
	}

	public NativeQueryImpl(
			String sqlString,
			boolean callable,
			SharedSessionContractImplementor session) {
		this(
				session,
				sqlString,
				callable,
				new ArrayList<>(),
				new ArrayList<>()
		);
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public ParameterMetadata getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public String getQueryString() {
		return sqlString;
	}

	@Override
	public boolean isCallable() {
		return callable;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		final HashSet<Parameter<?>> parameters = new HashSet<>();
		parameterMetadata.collectAllParameters( parameters::add );
		return parameters;
	}

	@Override
	protected QueryParameterBindings queryParameterBindings() {
		return parameterBindings;
	}

	@SuppressWarnings("WeakerAccess")
	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}

	@SuppressWarnings("WeakerAccess")
	public boolean isAutoDiscoverTypes() {
		return autoDiscoverTypes;
	}

	@Override
	public List<NativeSQLQueryReturn> getQueryReturns() {
		prepareQueryReturnsIfNecessary();
		return queryReturns;
	}

	@Override
	public NativeQuery<R> setResultSetMapping(String name) {
		ResultSetMappingDefinition mapping = getSession().getFactory().getNamedQueryRepository().getResultSetMappingDefinition( name );
		if ( mapping == null ) {
			throw new MappingException( "Unknown SqlResultSetMapping [" + name + "]" );
		}
		NativeSQLQueryReturn[] returns = mapping.getQueryReturns();
		queryReturns.addAll( Arrays.asList( returns ) );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		if ( cls.isInstance( parameterMetadata ) ) {
			return (T) parameterMetadata;
		}

		if ( cls.isInstance( parameterBindings ) ) {
			return (T) parameterBindings;
		}

		if ( cls.isInstance( queryOptions ) ) {
			return (T) queryOptions;
		}


		throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Execution


	@Override
	protected void prepareForExecution() {
		prepareQueryReturnsIfNecessary();

		boolean noReturns = queryReturns == null || queryReturns.isEmpty();
		if ( noReturns ) {
			this.autoDiscoverTypes = true;
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

		if ( getSynchronizedQuerySpaces() != null && !getSynchronizedQuerySpaces().isEmpty() ) {
			// The application defined query spaces on the Hibernate native SQLQuery which means the query will already
			// perform a partial flush according to the defined query spaces, no need to do a full flush.
			return;
		}

		// otherwise we need to flush.  the query itself is not required to execute in a transaction; if there is
		// no transaction, the flush would throw a TransactionRequiredException which would potentially break existing
		// apps, so we only do the flush if a transaction is in progress.
		//
		// NOTE : this was added for JPA initially.  Perhaps we want to only do this from JPA usage?
		if ( shouldFlush() ) {
			getSession().flush();
		}
	}

	private void prepareQueryReturnsIfNecessary() {
		if ( queryReturnBuilders != null ) {
			if ( !queryReturnBuilders.isEmpty() ) {
				if ( queryReturns != null ) {
					queryReturns.clear();
					queryReturns = null;
				}
				queryReturns = new ArrayList<>();
				for ( NativeQueryReturnBuilder builder : queryReturnBuilders ) {
					queryReturns.add( builder.buildReturn() );
				}
				queryReturnBuilders.clear();
			}
			queryReturnBuilders = null;
		}
	}

	private boolean shouldFlush() {
		if ( getSession().isTransactionInProgress() ) {
			FlushMode effectiveFlushMode = getHibernateFlushMode();
			if ( effectiveFlushMode == null ) {
				effectiveFlushMode = getSession().getHibernateFlushMode();
			}

			if ( effectiveFlushMode == FlushMode.ALWAYS ) {
				return true;
			}

			if ( effectiveFlushMode == FlushMode.AUTO ) {
				if ( getSession().getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<R> doList() {
		getSession().prepareForQueryExecution( false );

		return resolveSelectQueryPlan().performList(
				getSession(),
				getQueryOptions(),
				parameterBindings
		);
	}

	@SuppressWarnings("unchecked")
	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		SelectQueryPlan<R> queryPlan = null;

		final QueryInterpretations.Key cacheKey = NativeInterpretationsKey.generateFrom( this );
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryInterpretations().getSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = new SelectQueryPlanImpl<>( this );
			if ( cacheKey != null ) {
				getSession().getFactory().getQueryInterpretations().cacheSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}


	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		getSession().prepareForQueryExecution( false );

		return resolveSelectQueryPlan().performScroll(
				getSession(),
				getQueryOptions(),
				parameterBindings,
				scrollMode
		);
	}

	@Override
	protected Iterator<R> doIterate() {
		log.debugf( "NativeQuery#iterate being executed as NativeQuery#list#iterator" );
		return doList().iterator();
	}

	protected int doExecuteUpdate() {
		// trigger the transaction-in-progress checks...
		getSession().prepareForQueryExecution( true );

		return resolveNonSelectQueryPlan().executeUpdate(
				getSession(),
				getQueryOptions(),
				parameterBindings
		);
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		NonSelectQueryPlan queryPlan = null;

		final QueryInterpretations.Key cacheKey = NativeInterpretationsKey.generateFrom( this );
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryInterpretations().getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = new NonSelectQueryPlanImpl( this );
			if ( cacheKey != null ) {
				getSession().getFactory().getQueryInterpretations().cacheNonSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	@Override
	public NativeQueryImplementor setCollectionKey(Serializable key) {
		this.collectionKey = key;
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias) {
		return addScalar( columnAlias, null );
	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias, Type type) {
		addReturnBuilder(
				() -> new NativeSQLQueryScalarReturn( columnAlias, type )
		);
		return this;
	}

	@SuppressWarnings("WeakerAccess")
	protected void addReturnBuilder(NativeQueryReturnBuilder builder) {
		if ( queryReturnBuilders == null ) {
			queryReturnBuilders = new ArrayList<>();
		}

		queryReturnBuilders.add( builder );
	}

	@Override
	public RootReturn addRoot(String tableAlias, String entityName) {
		NativeQueryReturnBuilderRootImpl builder = new NativeQueryReturnBuilderRootImpl( tableAlias, entityName );
		addReturnBuilder( builder );
		return builder;
	}

	@Override
	public RootReturn addRoot(String tableAlias, Class entityType) {
		return addRoot( tableAlias, entityType.getName() );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String entityName) {
		return addEntity( StringHelper.unqualify( entityName ), entityName );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, String entityName) {
		addRoot( tableAlias, entityName );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, String entityName, LockMode lockMode) {
		addRoot( tableAlias, entityName ).setLockMode( lockMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addEntity(Class entityType) {
		return addEntity( entityType.getName() );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, Class entityClass) {
		return addEntity( tableAlias, entityClass.getName() );
	}

	@Override
	public NativeQueryImplementor<R> addEntity(String tableAlias, Class entityClass, LockMode lockMode) {
		return addEntity( tableAlias, entityClass.getName(), lockMode );
	}

	@Override
	public FetchReturn addFetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		NativeQueryReturnBuilderFetchImpl builder = new NativeQueryReturnBuilderFetchImpl( tableAlias, ownerTableAlias, joinPropertyName );
		addReturnBuilder( builder );
		return builder;
	}

	@Override
	public NativeQueryImplementor<R> addJoin(String tableAlias, String path) {
		createFetchJoin( tableAlias, path );
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

	@Override
	public NativeQueryImplementor<R> addJoin(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		addFetch( tableAlias, ownerTableAlias, joinPropertyName );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addJoin(String tableAlias, String path, LockMode lockMode) {
		createFetchJoin( tableAlias, path ).setLockMode( lockMode );
		return this;
	}

	@Override
	public Collection<String> getSynchronizedQuerySpaces() {
		return querySpaces;
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedQuerySpace(String querySpace) {
		addQuerySpaces( querySpace );
		return this;
	}

	protected void addQuerySpaces(String... spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new ArrayList<>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
	}

	protected void addQuerySpaces(Serializable... spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new ArrayList<>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedEntityName(String entityName) throws MappingException {
		addQuerySpaces( getSession().getFactory().getTypeConfiguration().resolveEntityPersister( entityName ).getQuerySpaces() );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor<R> addSynchronizedEntityClass(Class entityClass) throws MappingException {
		addQuerySpaces( getSession().getFactory().getTypeConfiguration().resolveEntityPersister( entityClass ).getQuerySpaces() );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFlushMode(FlushModeType flushModeType) {
		super.setFlushMode( flushModeType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setLockOptions(LockOptions lockOptions) {
		super.setLockOptions( lockOptions );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setLockMode(String alias, LockMode lockMode) {
		super.setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setLockMode(LockModeType lockModeType) {
		throw new IllegalStateException( "Illegal attempt to set lock mode on a native SQL query" );
	}

	@Override
	public NativeQueryImplementor<R> setComment(String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addQueryHint(String hint) {
		super.addQueryHint( hint );
		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, EntityGraphImpl entityGraph) {
		// technically we *could* support EntityGraph applied to NativeQuery
		// 		but that would mean that loading the eager state that was not loaded by
		// 		the SQL would need to be performed as a follow-up (N+1)
		throw new UnsupportedOperationException( "EntityGraph cannot be applied to NativeQuery" );
	}

	@Override
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

//		putIfNotNull( hints, HINT_NATIVE_LOCKMODE, getLockOptions().getLockMode() );
	}

	@Override
	protected boolean applyNativeQueryLockMode(Object value) {
		// todo : no longer supported - Exception or deprecation warning?
		return false;
//		if ( LockMode.class.isInstance( value ) ) {
//			applyHibernateLockModeHint( (LockMode) value );
//		}
//		else if ( LockModeType.class.isInstance( value ) ) {
//			applyLockModeTypeHint( (LockModeType) value );
//		}
//		else {
//			throw new IllegalArgumentException(
//					String.format(
//							"Native lock-mode hint [%s] must specify %s or %s.  Encountered type : %s",
//							HINT_NATIVE_LOCKMODE,
//							LockMode.class.getName(),
//							LockModeType.class.getName(),
//							value.getClass().getName()
//					)
//			);
//		}
//
//		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor<R> setParameter(QueryParameter parameter, Object value) {
		super.setParameter( (Parameter<Object>) parameter, value );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor<R> setParameter(QueryParameter parameter, Object value, Type type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Object value, Type type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Object value, Type type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, TemporalType temporalType) {
		super.setParameter( parameter, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Object value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Object value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor<R> setParameterList(QueryParameter parameter, Collection values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Collection values, Type type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Object[] values, Type type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor<R> setParameter(Parameter param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor<R> setParameter(Parameter param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setProperties(Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	protected boolean canApplyAliasSpecificLockModes() {
		return false;
	}

	@Override
	protected void verifySettingLockMode() {
		throw new IllegalStateException( "Illegal attempt to set lock mode on a native query" );
	}

	@Override
	protected void verifySettingAliasSpecificLockModes() {
		throw new IllegalStateException( "Illegal attempt to set lock mode on a native query" );
	}

	@Override
	public NativeQueryImplementor<R> setMaxResults(int maxResult) {
		super.setMaxResults( maxResult );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setFirstResult(int startPosition) {
		super.setFirstResult( startPosition );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}
}
