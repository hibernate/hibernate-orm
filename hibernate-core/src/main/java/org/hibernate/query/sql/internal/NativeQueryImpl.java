/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.engine.query.spi.NativeQueryInterpreter;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.Limit;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.internal.NativeQueryReturnBuilder;
import org.hibernate.query.internal.NativeQueryReturnBuilderFetchImpl;
import org.hibernate.query.internal.NativeQueryReturnBuilderRootImpl;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryDefinition;
import org.hibernate.query.sql.spi.NonSelectInterpretationsKey;
import org.hibernate.query.sql.spi.ParameterInterpretation;
import org.hibernate.query.sql.spi.SelectInterpretationsKey;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.DomainParameterBindingContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.JdbcValuesMappingDescriptor;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import static org.hibernate.jpa.QueryHints.HINT_NATIVE_LOCKMODE;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class NativeQueryImpl<R>
		extends AbstractQuery<R>
		implements NativeQueryImplementor<R>, DomainParameterBindingContext, ExecutionContext {
	private final String sqlString;

	private final ParameterMetadataImplementor parameterMetadata;
	private final List<QueryParameterImplementor<?>> occurrenceOrderedParamList;
	private final QueryParameterBindings parameterBindings;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();

	private Set<String> querySpaces;



	private List<NativeSQLQueryReturn> queryReturns;
	private List<NativeQueryReturnBuilder> queryReturnBuilders;
	private boolean autoDiscoverTypes;

	private Serializable collectionKey;

	/**
	 * Constructs a NativeQueryImpl given a sql query defined in the mappings.
	 */
	public NativeQueryImpl(
			NamedNativeQueryMemento memento,
			SharedSessionContractImplementor session) {
		super( session );

		this.sqlString = memento.getSqlString();

		final ParameterInterpretation parameterInterpretation = resolveParameterInterpretation( session );

		this.parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		this.occurrenceOrderedParamList = parameterInterpretation.getOccurrenceOrderedParameters();
		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		applyOptions( memento );
	}

	/**
	 * Constructs a NativeQueryImpl given a sql query defined in the mappings.
	 */
	public NativeQueryImpl(
			NamedNativeQueryMemento memento,
			Class<R> resultJavaType,
			SharedSessionContractImplementor session) {
		this( memento, session );

		// todo (6.0) : validate `resultJavaType` against specified result-set mapping
	}

	/**
	 * Constructs a NativeQueryImpl given a sql query defined in the mappings.
	 */
	public NativeQueryImpl(
			NamedNativeQueryMemento memento,
			String resultSetMappingName,
			SharedSessionContractImplementor session) {
		this( memento, session );
	}

	private ParameterInterpretation resolveParameterInterpretation(SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final QueryEngine queryEngine = sessionFactory.getQueryEngine();
		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();

		return interpretationCache.resolveNativeQueryParameters(
					sqlString,
					s -> {
						final ParameterRecognizerImpl parameterRecognizer = new ParameterRecognizerImpl( session.getFactory() );

						session.getFactory().getServiceRegistry()
								.getService( NativeQueryInterpreter.class )
								.recognizeParameters( sqlString, parameterRecognizer );

						return new ParameterInterpretation() {
							@Override
							public List<QueryParameterImplementor<?>> getOccurrenceOrderedParameters() {
								return parameterRecognizer.getParameterList();
							}

							@Override
							public ParameterMetadataImplementor toParameterMetadata(SharedSessionContractImplementor session1) {
								return new ParameterMetadataImpl(
										parameterRecognizer.getPositionalQueryParameters(),
										parameterRecognizer.getNamedQueryParameters()
								);
							}
						};
					}
			);
	}

	protected void applyOptions(NamedNativeQueryMemento memento) {
		super.applyOptions( memento );

		this.querySpaces = CollectionHelper.makeCopy( memento.getQuerySpaces() );

		// todo (6.0) : query returns
	}

	public NativeQueryImpl(String sqlString, SharedSessionContractImplementor session) {
		super( session );

		this.sqlString = sqlString;

		this.queryReturns = new ArrayList<>();
		this.querySpaces = new HashSet<>();

		final ParameterInterpretation parameterInterpretation = resolveParameterInterpretation( session );
		this.parameterMetadata = parameterInterpretation.toParameterMetadata( session );
		this.occurrenceOrderedParamList = parameterInterpretation.getOccurrenceOrderedParameters();
		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );
	}

	@Override
	public String getQueryString() {
		return sqlString;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public DomainParameterBindingContext getDomainParameterBindingContext() {
		return this;
	}

	@Override
	public Callback getCallback() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}

	@Override
	public <T> List<T> getLoadIdentifiers() {
		//noinspection unchecked
		return (List) Collections.singletonList( collectionKey );
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return getQueryParameterBindings();
	}

	@Override
	public NamedNativeQueryMemento toMemento(String name) {
		throw new NotYetImplementedFor6Exception(  );
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
	public Query<R> applyGraph(RootGraph graph, GraphSemantic semantic) {
		throw new HibernateException( "A native SQL query cannot use EntityGraphs" );
	}

	@Override
	public NativeQueryImplementor<R> setTupleTransformer(TupleTransformer transformer) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setTupleTransformer( transformer );
	}

	@Override
	public NativeQueryImplementor<R> setResultListTransformer(ResultListTransformer transformer) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setResultListTransformer( transformer );
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
	protected List<R> doList() {
		//noinspection unchecked
		return resolveSelectQueryPlan().performList( this );
	}

	@SuppressWarnings("unchecked")
	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		final NativeQueryInterpreter interpreter = getSessionFactory().getServiceRegistry().getService( NativeQueryInterpreter.class );

		SelectQueryPlan<R> queryPlan = null;


		final JdbcValuesMappingDescriptor resultSetMapping = resolveResultMapping();
		final RowTransformer rowTransformer = resolveRowTransformer();

		final QueryInterpretationCache.Key cacheKey = generateSelectInterpretationsKey( resultSetMapping );
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryEngine().getInterpretationCache().getSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = interpreter.createQueryPlan(
					generateSelectQueryDefinition(),
					getSessionFactory()
			);
			if ( cacheKey != null ) {
				getSession().getFactory()
						.getQueryEngine()
						.getInterpretationCache()
						.cacheSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	private JdbcValuesMappingDescriptor resolveResultMapping() {
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

	private SelectInterpretationsKey generateSelectInterpretationsKey(JdbcValuesMappingDescriptor resultSetMapping) {
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

	private NativeSelectQueryDefinition<R> generateSelectQueryDefinition() {
		return new NativeSelectQueryDefinition() {
			@Override
			public String getSqlString() {
				return NativeQueryImpl.this.getQueryString();
			}

			@Override
			public boolean isCallable() {
				return false;
			}

			@Override
			public List<QueryParameterImplementor<?>> getQueryParameterList() {
				return NativeQueryImpl.this.occurrenceOrderedParamList;
			}

			@Override
			public JdbcValuesMappingDescriptor getResultSetMapping() {
				return NativeQueryImpl.this.resolveResultMapping();
			}

			@Override
			public RowTransformer getRowTransformer() {
				return NativeQueryImpl.this.resolveRowTransformer();
			}

			@Override
			public Set<String> getAffectedTableNames() {
				return querySpaces;
			}
		};
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

	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}

	@Override
	protected void beforeQuery(boolean txnRequired) {
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

		super.beforeQuery( txnRequired );

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

	protected int doExecuteUpdate() {
			return resolveNonSelectQueryPlan().executeUpdate( this );
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		NonSelectQueryPlan queryPlan = null;

		final QueryInterpretationCache.Key cacheKey = generateNonSelectInterpretationsKey();
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryEngine().getInterpretationCache().getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = new NativeNonSelectQueryPlanImpl( this );
			if ( cacheKey != null ) {
				getSession().getFactory().getQueryEngine().getInterpretationCache().cacheNonSelectQueryPlan( cacheKey, queryPlan );
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
	public NativeQueryImplementor<R> addScalar(String columnAlias) {
		return addScalar( columnAlias, null );
	}

//	@Override
//	public NativeQueryImplementor<R> addScalar(String columnAlias, BasicDomainType type) {
//		return null;
//	}

	@Override
	public NativeQueryImplementor<R> addScalar(String columnAlias, BasicDomainType type) {
		addReturnBuilder(
				() -> new NativeSQLQueryScalarReturn( columnAlias, type )
		);
		return this;
	}

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
				querySpaces = new HashSet<>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
	}

	protected void addQuerySpaces(Serializable... spaces) {
		if ( spaces != null ) {
			if ( querySpaces == null ) {
				querySpaces = new HashSet<>();
			}
			querySpaces.addAll( Arrays.asList( (String[]) spaces ) );
		}
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedEntityName(String entityName) throws MappingException {
		addQuerySpaces( getSession().getFactory().getMetamodel().entityPersister( entityName ).getQuerySpaces() );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> addSynchronizedEntityClass(Class entityClass) throws MappingException {
		addQuerySpaces( getSession().getFactory().getMetamodel().entityPersister( entityClass.getName() ).getQuerySpaces() );
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
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> javaType) {
		if ( javaType.isAssignableFrom( getClass() ) ) {
			return (T) this;
		}

		if ( javaType.isAssignableFrom( ParameterMetadata.class ) ) {
			return (T) parameterMetadata;
		}

		if ( javaType.isAssignableFrom( QueryParameterBindings.class ) ) {
			return (T) parameterBindings;
		}

		if ( javaType.isAssignableFrom( EntityManager.class ) ) {
			return (T) getSession();
		}

		if ( javaType.isAssignableFrom( EntityManagerFactory.class ) ) {
			return (T) getSession().getFactory();
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + javaType.getName() + "]" );
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
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		putIfNotNull( hints, HINT_NATIVE_LOCKMODE, getLockOptions().getLockMode() );
	}

	@Override
	protected boolean applyNativeQueryLockMode(Object value) {
		if ( value instanceof LockMode ) {
			applyHibernateLockModeHint( (LockMode) value );
		}
		else if ( value instanceof LockModeType ) {
			applyLockModeTypeHint( (LockModeType) value );
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							"Native lock-mode hint [%s] must specify %s or %s.  Encountered type : %s",
							HINT_NATIVE_LOCKMODE,
							LockMode.class.getName(),
							LockModeType.class.getName(),
							value.getClass().getName()
					)
			);
		}

		return true;
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

//	@Override
//	@SuppressWarnings("unchecked")
//	public NativeQueryImplementor<R> setParameter(QueryParameter parameter, Object value, Type type) {
//		super.setParameter( parameter, value, type );
//		return this;
//	}
//
//	@Override
//	public NativeQueryImplementor<R> setParameter(String name, Object value, Type type) {
//		super.setParameter( name, value, type );
//		return this;
//	}
//
//	@Override
//	public NativeQueryImplementor<R> setParameter(int position, Object value, Type type) {
//		super.setParameter( position, value, type );
//		return this;
//	}

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
	public NativeQueryImplementor<R> setParameter(Parameter<Instant> param, Instant value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(Parameter<LocalDateTime> param, LocalDateTime value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(Parameter<ZonedDateTime> param, ZonedDateTime value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(Parameter<OffsetDateTime> param, OffsetDateTime value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}
//
//	@Override
//	public NativeQueryImplementor<R> setParameterList(int position, Collection values, Type type) {
//		return setParameterList( position, values, ( AllowableParameterType) type );
//	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Collection values, AllowableParameterType type) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( name, values, type );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(int position, Collection values, AllowableParameterType type) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( position, values, type );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Object[] values, AllowableParameterType type) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( name, values, type );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(int position, Object[] values, AllowableParameterType type) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( position, values, type );
	}

//	@Override
//	public NativeQueryImplementor<R> setParameterList(int position, Object[] values, Type type) {
//		return null;
//	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, LocalDateTime value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, ZonedDateTime value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, OffsetDateTime value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, LocalDateTime value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, ZonedDateTime value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, OffsetDateTime value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(QueryParameter parameter, Collection values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

//	@Override
//	public NativeQueryImplementor<R> setParameterList(String name, Collection values, Type type) {
//		super.setParameterList( name, values, type );
//		return this;
//	}
//
//	@Override
//	public NativeQueryImplementor<R> setParameterList(String name, Object[] values, Type type) {
//		super.setParameterList( name, values, type );
//		return this;
//	}

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
	public NativeQueryImplementor<R> setResultTransformer(ResultTransformer transformer) {
		super.setResultTransformer( transformer );
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
	public <P> NativeQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, AllowableParameterType type) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameter( parameter, value, type );
	}

	@Override
	public NativeQueryImplementor<R> setParameter(String name, Object value, AllowableParameterType type) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameter( name, value, type );
	}

	@Override
	public NativeQueryImplementor<R> setParameter(int position, Object value, AllowableParameterType type) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameter( position, value, type );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(int position, Collection values) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( position, values );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(int position, Object[] values) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( position, values );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(String name, Collection values, Class javaType) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( name, values, javaType );
	}

	@Override
	public NativeQueryImplementor<R> setParameterList(int position, Collection values, Class javaType) {
		//noinspection unchecked
		return (NativeQueryImplementor) super.setParameterList( position, values, javaType );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hints

	@Override
	public NativeQueryImplementor<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, RootGraphImplementor entityGraph) {
		throw new HibernateException( "A native SQL query cannot use EntityGraphs" );
	}
}
