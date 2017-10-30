/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.Limit;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.AbstractQuery;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.named.spi.NamedNativeQueryDescriptor;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.query.spi.NativeQueryInterpreter;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryInterpretations;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ResultSetMappingDescriptor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sql.spi.FetchBuilder;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NonSelectInterpretationsKey;
import org.hibernate.query.sql.spi.QueryResultBuilder;
import org.hibernate.query.sql.spi.QueryResultBuilderRootEntity;
import org.hibernate.query.sql.spi.QueryResultBuilderScalar;
import org.hibernate.query.sql.spi.SelectInterpretationsKey;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.exec.spi.RowTransformer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class NativeQueryImpl<R>
		extends AbstractQuery<R>
		implements NativeQueryImplementor<R>, ParameterBindingContext, ExecutionContext {
	private static final Logger log = Logger.getLogger( NativeQueryImpl.class );

	private final String sqlString;

	private List<QueryResultBuilder> resultBuilders;
	private List<FetchBuilder> fetchBuilders;
	private boolean resultSetMappingUsed;

	private final ParameterMetadataImpl parameterMetadata;
	private final QueryParameterBindings parameterBindings;

	private final List<JdbcParameterBinder> parameterBinders;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	private Set<String> affectedTableNames;

	private Serializable collectionKey;

	/**
	 * Form used to create a native query from query + ResultSetMapping
	 */
	public NativeQueryImpl(
			String sqlString,
			ResultSetMappingDescriptor resultSetMappingDescriptor,
			SharedSessionContractImplementor session) {
		this( sqlString, session );

		applyResultSetMapping( resultSetMappingDescriptor );
	}

	private void applyResultSetMapping(ResultSetMappingDescriptor resultSetMappingDescriptor) {
		resultSetMappingUsed = true;
		resultBuilders = new ArrayList<>( resultSetMappingDescriptor.getResultBuilders() );
		fetchBuilders = new ArrayList<>( resultSetMappingDescriptor.getFetchBuilders() );
	}

	public NativeQueryImpl(
			String sqlString,
			SharedSessionContractImplementor session) {
		super( session );
		this.sqlString = sqlString;

		final ParameterRecognizerImpl parameterRecognizer = new ParameterRecognizerImpl( session.getFactory() );
		session.getFactory().getServiceRegistry()
				.getService( NativeQueryInterpreter.class )
				.recognizeParameters( sqlString, parameterRecognizer );
		parameterRecognizer.validate();

		this.parameterMetadata = new ParameterMetadataImpl(
				parameterRecognizer.getNamedQueryParameters(),
				parameterRecognizer.getPositionalQueryParameters()
		);
		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		this.parameterBinders = parameterRecognizer.getParameterBinders();
	}

	/**
	 * Constructs a NativeQueryImpl given a sql query defined in the mappings.
	 *
	 * @param namedQueryDescriptor The representation of the defined <sql-query/>.
	 * @param session The session to which this NativeQuery belongs.
	 */
	public NativeQueryImpl(
			NamedNativeQueryDescriptor namedQueryDescriptor,
			SharedSessionContractImplementor session) {
		this( namedQueryDescriptor.getQueryString(), session );

		namedQueryDescriptor.getQuerySpaces().forEach( this::addSynchronizedQuerySpace );

		applyResultSetMapping( namedQueryDescriptor );
	}

	private void applyResultSetMapping(NamedNativeQueryDescriptor namedQueryDescriptor) {
		if ( namedQueryDescriptor.getResultSetMappingName() == null ) {
			return;
		}

		ResultSetMappingDescriptor resultSetMappingDescriptor = getSession().getFactory()
				.getQueryEngine()
				.getNamedQueryRepository()
				.getResultSetMappingDescriptor( namedQueryDescriptor.getResultSetMappingName() );
		if ( resultSetMappingDescriptor == null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Unable to find ResultSetMappingDescriptor [%s] specified for named native query [%s]",
							namedQueryDescriptor.getResultSetMappingName(),
							namedQueryDescriptor.getName()
					)
			);
		}

		applyResultSetMapping( resultSetMappingDescriptor );
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public ParameterBindingContext getParameterBindingContext() {
		return this;
	}

	@Override
	public Callback getCallback() {
		return null;
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Result handling

	@Override
	public NativeQuery<R> setResultSetMapping(String name) {
		if ( resultSetMappingUsed ) {
			throw new QueryException( "Cannot apply multiple SQL-ResultSet-mappings to a single native query" );
		}

		if ( resultBuilders != null && !resultBuilders.isEmpty() ) {
			throw new QueryException( "Cannot mix SQL-ResultSet-mappings and manually defined mappings in a single native query" );
		}

		// todo (6.0) : see the notes on ResultSetMappingDefinition.
		//		really this should be something like:
		//
		final ResultSetMappingDescriptor mappingDefinition = getSession().getFactory()
				.getQueryEngine()
				.getNamedQueryRepository()
				.getResultSetMappingDescriptor( name );
		if ( mappingDefinition == null ) {
			throw new MappingException( "Unknown SqlResultSetMapping [" + name + "]" );
		}

		this.resultSetMappingUsed = true;

		if ( resultBuilders == null ) {
			resultBuilders = new ArrayList<>();
		}

		for ( ResultSetMappingDescriptor.QueryResultDefinition queryResultDefinition : mappingDefinition.getQueryResultDefinitions() ) {
			resultBuilders.add( queryResultDefinition.resolve() );
		}

		return this;
	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias) {
		addReturnBuilder( new QueryResultBuilderScalar( columnAlias, null) );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias, AllowableParameterType type) {

		return this;
	}

	@SuppressWarnings("WeakerAccess")
	protected void addReturnBuilder(QueryResultBuilder builder) {
		if ( resultBuilders == null ) {
			resultBuilders = new ArrayList<>();
		}

		resultBuilders.add( builder );
	}

	@Override
	public RootReturn addRoot(String tableAlias, String entityName) {
		final EntityDescriptor entityDescriptor = getPersistenceContext().getFactory()
				.getTypeConfiguration()
				.resolveEntityDescriptor( entityName );
		QueryResultBuilderRootEntity builder = new QueryResultBuilderRootEntity( tableAlias, entityDescriptor );
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
		final FetchBuilder builder = new FetchBuilder( tableAlias, ownerTableAlias, joinPropertyName );
		fetchBuilders.add( builder );
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
			throw new QueryException(
					String.format(
							Locale.ROOT,
							"Not a property path [%s]; should be in form [owner_alias].[fetched_attribute_name] - see Javadocs",
							path
					)
			);
		}

		if ( loc > 1 ) {
			throw new QueryException(
					String.format(
							Locale.ROOT,
							"Cannot join composite property path [%s]; should be in form [owner_alias].[fetched_attribute_name] - see Javadocs",
							path
					)
			);
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
		if ( getSynchronizedQuerySpaces() != null && !getSynchronizedQuerySpaces().isEmpty() ) {
			// The application defined query spaces on the Hibernate NativeQuery
			// which means the query will already perform a partial flush
			// according to the defined query spaces, no need to do a full flush.
			return;
		}

		// otherwise we need to flush.  the query itself is not required to execute
		// in a transaction; if there is no transaction, the flush would throw a
		// TransactionRequiredException which would potentially break existing
		// apps, so we only do the flush if a transaction is in progress.
		//
		// NOTE : this was added for JPA initially.  Perhaps we want to only do
		// this from JPA usage?
		if ( shouldFlush() ) {
			getSession().flush();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<R> doList() {
		getSession().prepareForQueryExecution( false );

		return resolveSelectQueryPlan().performList( this );
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

	@SuppressWarnings("unchecked")
	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		SelectQueryPlan<R> queryPlan = null;

		final org.hibernate.sql.results.spi.ResultSetMappingDescriptor resultSetMapping = resolveResultMapping();
		final RowTransformer rowTransformer = resolveRowTransformer();

		final QueryInterpretations.Key cacheKey = generateSelectInterpretationsKey( resultSetMapping );
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryEngine().getQueryInterpretations().getSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = getNativeQueryInterpreter().createQueryPlan(
					generateSelectQueryDefinition(),
					getSessionFactory()
			);
			if ( cacheKey != null ) {
				getSession().getFactory()
						.getQueryEngine()
						.getQueryInterpretations()
						.cacheSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	private NativeQueryInterpreter getNativeQueryInterpreter() {
		return getSessionFactory().getServiceRegistry().getService( NativeQueryInterpreter.class );
	}

	private NativeSelectQueryDefinition generateSelectQueryDefinition() {
		return new NativeSelectQueryDefinition() {
			@Override
			public String getSqlString() {
				return NativeQueryImpl.this.getQueryString();
			}

			@Override
			public boolean isCallable() {
				return NativeQueryImpl.this.isCallable();
			}

			@Override
			public List<JdbcParameterBinder> getParameterBinders() {
				return NativeQueryImpl.this.getParameterBinders();
			}

			@Override
			public org.hibernate.sql.results.spi.ResultSetMappingDescriptor getResultSetMapping() {
				return NativeQueryImpl.this.resolveResultMapping();
			}

			@Override
			public RowTransformer getRowTransformer() {
				return NativeQueryImpl.this.resolveRowTransformer();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return affectedTableNames;
			}
		};
	}

	private org.hibernate.sql.results.spi.ResultSetMappingDescriptor resolveResultMapping() {
		// todo (6.0) - need to resolve SqlSelections as well as resolving ResultBuilders and FetchBuilders into QueryResult trees
		// 		also need to account for the edge case where the user passed just the
		//		query string and no mappings (see ResultSetMappingUndefinedImpl)
		throw new NotYetImplementedFor6Exception(  );
	}

	private RowTransformer resolveRowTransformer() {
		// todo (6.0) - need to resolve the RowTransformer to use, if one.
		// todo (6.0) - what about ResultListTransformer?
		throw new NotYetImplementedFor6Exception(  );
	}

	private SelectInterpretationsKey generateSelectInterpretationsKey(org.hibernate.sql.results.spi.ResultSetMappingDescriptor resultSetMapping) {
		if ( !isCacheable( this ) ) {
			return null;
		}

		return new SelectInterpretationsKey(
				getQueryString(),
				resultSetMapping,
				getQueryOptions().getTupleTransformer(),
				getQueryOptions().getResultListTransformer()
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	private static boolean isCacheable(NativeQueryImpl query) {
		if ( hasLimit( query.getQueryOptions().getLimit() ) ) {
			return false;
		}

		return true;
	}

	private static boolean hasLimit(Limit limit) {
		return limit.getFirstRow() != null || limit.getMaxRows() != null;
	}


	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		getSession().prepareForQueryExecution( false );
		prepareForExecution();

		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}

	protected int doExecuteUpdate() {
		// trigger the transaction-in-progress checks...
		getSession().prepareForQueryExecution( true );

		return resolveNonSelectQueryPlan().executeUpdate(
				getSession(),
				getQueryOptions(),
				this
		);
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		NonSelectQueryPlan queryPlan = null;

		final QueryInterpretations.Key cacheKey = generateNonSelectInterpretationsKey();
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryEngine().getQueryInterpretations().getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = new NativeNonSelectQueryPlanImpl( this );
			if ( cacheKey != null ) {
				getSession().getFactory().getQueryEngine().getQueryInterpretations().cacheNonSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}


	protected NonSelectInterpretationsKey generateNonSelectInterpretationsKey() {
		// todo (6.0) - should this account for query-spaces in determining "cacheable"?
		return new NonSelectInterpretationsKey(
				getQueryString(),
				getSynchronizedQuerySpaces()
		);
	}

	@Override
	public NativeQueryImplementor setCollectionKey(Serializable key) {
		this.collectionKey = key;
		return this;
	}

	@Override
	public Collection<String> getSynchronizedQuerySpaces() {
		return affectedTableNames;
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedQuerySpace(String querySpace) {
		addQuerySpaces( querySpace );
		return this;
	}

	protected void addQuerySpaces(String... spaces) {
		if ( spaces != null ) {
			if ( affectedTableNames == null ) {
				affectedTableNames = new HashSet<>();
			}
			affectedTableNames.addAll( Arrays.asList( (String[]) spaces ) );
		}
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedEntityName(String entityName) throws MappingException {
		addQuerySpaces( getSession().getFactory().getTypeConfiguration().resolveEntityDescriptor( entityName ).getAffectedTableNames() );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public NativeQueryImplementor<R> addSynchronizedEntityClass(Class entityClass) throws MappingException {
		addQuerySpaces( getSession().getFactory().getTypeConfiguration().resolveEntityDescriptor( entityClass ).getAffectedTableNames() );
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
	protected void applyEntityGraphQueryHint(String hintName, EntityGraphImplementor entityGraph) {
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
	public NativeQueryImplementor<R> setParameter(QueryParameter parameter, Object value, AllowableParameterType type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Object value, AllowableParameterType type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Object value, AllowableParameterType type) {
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
	public NativeQueryImplementor<R> setParameterList(String name, Collection values, AllowableParameterType type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Object[] values, AllowableParameterType type) {
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

	@Override
	public <T> Collection<T> getLoadIdentifiers() {
		return null;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public NamedNativeQueryDescriptor toNamedDescriptor(String name) {
		// todo (6.0) : need to account for creating and registering a result mapping
		throw new NotYetImplementedFor6Exception();
	}
}
