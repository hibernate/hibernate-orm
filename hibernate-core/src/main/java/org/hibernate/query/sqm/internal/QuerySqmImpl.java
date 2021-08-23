/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.Tuple;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.Query;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.spi.SqlOmittingQueryOptions;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.BasicType;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R>
		extends AbstractQuery<R>
		implements HqlQueryImplementor<R>, ExecutionContext {

	/**
	 * The value used for {@link #getQueryString} for Criteria-based queries
	 */
	public static final String CRITERIA_HQL_STRING = "<criteria>";
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QuerySqmImpl.class );

	private final String hqlString;
	private final SqmStatement<R> sqmStatement;
	private final Class<R> resultType;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;

	private final QueryParameterBindingsImpl parameterBindings;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
	private Callback callback;

	/**
	 * Creates a Query instance from a named HQL memento
	 */
	public QuerySqmImpl(
			NamedHqlQueryMemento memento,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		this.hqlString = memento.getHqlString();

		final SessionFactoryImplementor factory = producer.getFactory();
		final QueryEngine queryEngine = factory.getQueryEngine();
		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();
		final HqlInterpretation hqlInterpretation = interpretationCache.resolveHqlInterpretation(
				hqlString,
				s -> queryEngine.getHqlTranslator().translate( hqlString )
		);

		this.sqmStatement = hqlInterpretation.getSqmStatement();

		if ( resultType != null ) {
			if ( sqmStatement instanceof SqmDmlStatement ) {
				throw new IllegalArgumentException( "Non-select queries cannot be typed" );
			}
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			verifyImmutableEntityUpdate( hqlString, (SqmUpdateStatement<R>) sqmStatement, producer.getFactory() );
		}
		this.resultType = resultType;
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();
		this.parameterMetadata = hqlInterpretation.getParameterMetadata();

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );

		applyOptions( memento );
	}

	protected void applyOptions(NamedHqlQueryMemento memento) {
		super.applyOptions( memento );

		if ( memento.getFirstResult() != null ) {
			setFirstResult( memento.getFirstResult() );
		}

		if ( memento.getMaxResults() != null ) {
			setMaxResults( memento.getMaxResults() );
		}

		if ( memento.getParameterTypes() != null ) {
			for ( Map.Entry<String, String> entry : memento.getParameterTypes().entrySet() ) {
				final QueryParameterImplementor<?> parameter = parameterMetadata.getQueryParameter( entry.getKey() );
				final BasicType type = getSessionFactory().getTypeConfiguration()
						.getBasicTypeRegistry()
						.getRegisteredType( entry.getValue() );
				parameter.applyAnticipatedType( type );
			}
		}
	}

	/**
	 * Form used for HQL queries
	 */
	public QuerySqmImpl(
			String hqlString,
			HqlInterpretation hqlInterpretation,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		this.hqlString = hqlString;
		this.resultType = resultType;

		this.sqmStatement = hqlInterpretation.getSqmStatement();

		if ( resultType != null ) {
			SqmUtil.verifyIsSelectStatement( sqmStatement );
			visitQueryReturnType(
					( (SqmSelectStatement<R>) sqmStatement ).getQueryPart(),
					resultType,
					producer.getFactory()
			);
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			verifyImmutableEntityUpdate( hqlString, (SqmUpdateStatement<R>) sqmStatement, producer.getFactory() );
		}

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );
	}

	/**
	 * Form used for criteria queries
	 */
	public QuerySqmImpl(
			SqmStatement<R> sqmStatement,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		if ( resultType != null ) {
			SqmUtil.verifyIsSelectStatement( sqmStatement );
			final SqmQueryPart<R> queryPart = ( (SqmSelectStatement<R>) sqmStatement ).getQueryPart();
			// For criteria queries, we have to validate the fetch structure here
			queryPart.validateQueryGroupFetchStructure();
			visitQueryReturnType(
					queryPart,
					resultType,
					producer.getFactory()
			);
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			verifyImmutableEntityUpdate( CRITERIA_HQL_STRING, (SqmUpdateStatement<R>) sqmStatement, producer.getFactory() );
		}

		this.hqlString = CRITERIA_HQL_STRING;
		this.sqmStatement = sqmStatement;
		this.resultType = resultType;

		this.domainParameterXref = DomainParameterXref.from( sqmStatement );
		if ( ! domainParameterXref.hasParameters() ) {
			this.parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			this.parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, producer.getFactory() );
		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : this.domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				final JpaCriteriaParameter<Object> jpaCriteriaParameter = ( (SqmJpaCriteriaParameterWrapper<Object>) sqmParameter ).getJpaCriteriaParameter();
				final Object value = jpaCriteriaParameter.getValue();
				// We don't set a null value, unless the type is also null which is the case when using HibernateCriteriaBuilder.value
				if ( value != null || jpaCriteriaParameter.getNodeType() == null ) {
					// Use the anticipated type for binding the value if possible
					getQueryParameterBindings().getBinding( jpaCriteriaParameter )
							.setBindValue( value, jpaCriteriaParameter.getAnticipatedType() );
				}
			}
		}
	}

	private void visitQueryReturnType(
			SqmQueryPart<R> queryPart,
			Class<R> resultType,
			SessionFactoryImplementor factory) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			final SqmQuerySpec<R> sqmQuerySpec = (SqmQuerySpec<R>) queryPart;
			final List<SqmSelection<?>> sqmSelections = sqmQuerySpec.getSelectClause().getSelections();

			if ( sqmSelections == null || sqmSelections.isEmpty() ) {
				// make sure there is at least one root
				final List<SqmRoot<?>> sqmRoots = sqmQuerySpec.getFromClause().getRoots();
				if ( sqmRoots == null || sqmRoots.isEmpty() ) {
					throw new IllegalArgumentException( "Criteria did not define any query roots" );
				}
				// if there is a single root, use that as the selection
				if ( sqmRoots.size() == 1 ) {
					final SqmRoot<?> sqmRoot = sqmRoots.get( 0 );
					sqmQuerySpec.getSelectClause().add( sqmRoot, null );
				}
				else {
					throw new IllegalArgumentException(  );
				}
			}

			if ( resultType != null ) {
				checkQueryReturnType( sqmQuerySpec, resultType, factory );
			}
		}
		else {
			final SqmQueryGroup<R> queryGroup = (SqmQueryGroup<R>) queryPart;
			for ( SqmQueryPart<R> sqmQueryPart : queryGroup.getQueryParts() ) {
				visitQueryReturnType( sqmQueryPart, resultType, factory );
			}
		}
	}

	private static <T> void checkQueryReturnType(SqmQuerySpec<T> querySpec, Class<T> resultClass, SessionFactoryImplementor sessionFactory) {
		if ( resultClass == null ) {
			// nothing to check
			return;
		}

		final List<SqmSelection<?>> selections = querySpec.getSelectClause().getSelections();

		if ( resultClass.isArray() ) {
			// todo (6.0) : implement
		}
		else if ( Tuple.class.isAssignableFrom( resultClass ) ) {
			// todo (6.0) : implement
		}
		else {
			if ( selections.size() != 1 ) {
				final String errorMessage = "Query result-type error - multiple selections: use Tuple or array";

				if ( sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
					throw new IllegalArgumentException( errorMessage );
				}
				else {
					throw new QueryTypeMismatchException( errorMessage );
				}
			}

			final SqmSelection sqmSelection = selections.get( 0 );

			if ( sqmSelection.getSelectableNode() instanceof SqmParameter ) {
				final SqmParameter sqmParameter = (SqmParameter) sqmSelection.getSelectableNode();

				// we may not yet know a selection type
				if ( sqmParameter.getNodeType() == null || sqmParameter.getNodeType().getExpressableJavaTypeDescriptor() == null ) {
					// we can't verify the result type up front
					return;
				}
			}

			verifyResultType( resultClass, sqmSelection.getNodeType(), sessionFactory );
		}
	}

	private static <T> void verifyResultType(
			Class<T> resultClass,
			SqmExpressable<?> sqmExpressable,
			SessionFactoryImplementor sessionFactory) {
		assert sqmExpressable != null;
		assert sqmExpressable.getExpressableJavaTypeDescriptor() != null;

		if ( ! resultClass.isAssignableFrom( sqmExpressable.getExpressableJavaTypeDescriptor().getJavaTypeClass() ) ) {
			final String errorMessage = String.format(
					"Specified result type [%s] did not match Query selection type [%s] - multiple selections: use Tuple or array",
					resultClass.getName(),
					sqmExpressable.getExpressableJavaTypeDescriptor().getJavaType().getTypeName()
			);

			if ( sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
				throw new IllegalArgumentException( errorMessage );
			}
			else {
				throw new QueryTypeMismatchException( errorMessage );
			}
		}
	}

	private void verifyImmutableEntityUpdate(
			String hqlString,
			SqmUpdateStatement<R> sqmStatement,
			SessionFactoryImplementor factory) {
		final EntityPersister entityDescriptor = factory.getDomainModel()
				.getEntityDescriptor( sqmStatement.getTarget().getEntityName() );
		if ( entityDescriptor.isMutable() ) {
			return;
		}
		final ImmutableEntityUpdateQueryHandlingMode immutableEntityUpdateQueryHandlingMode = factory
				.getSessionFactoryOptions()
				.getImmutableEntityUpdateQueryHandlingMode();

		String querySpaces = Arrays.toString( entityDescriptor.getQuerySpaces() );

		switch ( immutableEntityUpdateQueryHandlingMode ) {
			case WARNING:
				LOG.immutableEntityUpdateQuery( hqlString, querySpaces );
				break;
			case EXCEPTION:
				throw new HibernateException(
						"The query: [" + hqlString + "] attempts to update an immutable entity: " + querySpaces
				);
			default:
				throw new UnsupportedOperationException(
						"The " + immutableEntityUpdateQueryHandlingMode + " is not supported!"
				);

		}
	}

	public SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}

	protected Boolean isSelectQuery() {
		return sqmStatement instanceof SqmSelectStatement;
	}

	@Override
	public String getQueryString() {
		return hqlString;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public HqlQueryImplementor<R> applyGraph(RootGraph graph, GraphSemantic semantic) {
		queryOptions.applyGraph( (RootGraphImplementor<?>) graph, semantic );
		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, RootGraphImplementor entityGraph) {
		final GraphSemantic graphSemantic = GraphSemantic.fromJpaHintName( hintName );

		applyGraph( entityGraph, graphSemantic );
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return sqmStatement;
	}

	@SuppressWarnings("unchecked")
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		getSession().checkOpen( false );
		Set<Parameter<?>> parameters = new HashSet<>();
		parameterMetadata.collectAllParameters( parameters::add );
		return parameters;
	}

	@Override
	public LockModeType getLockMode() {
		if ( ! isSelectQuery() ) {
			throw new IllegalStateException( "Illegal attempt to access lock-mode for non-select query" );
		}

		return super.getLockMode();
	}

	@Override
	public HqlQueryImplementor<R> setLockMode(LockModeType lockModeType) {
		if ( ! LockModeType.NONE.equals( lockModeType ) ) {
			if ( ! isSelectQuery() ) {
				throw new IllegalStateException( "Illegal attempt to access lock-mode for non-select query" );
			}
		}

		return (HqlQueryImplementor<R>) super.setLockMode( lockModeType );
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

		if ( cls.isInstance( sqmStatement ) ) {
			return (T) sqmStatement;
		}

		if ( cls.isInstance( queryOptions ) ) {
			return (T) queryOptions;
		}

		if ( cls.isInstance( queryOptions.getAppliedGraph() ) ) {
			return (T) queryOptions.getAppliedGraph();
		}

		if ( EntityGraphQueryHint.class.isAssignableFrom( cls ) ) {
			return (T) new EntityGraphQueryHint( queryOptions.getAppliedGraph() );
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
	}

	protected boolean applyNativeQueryLockMode(Object value) {
		throw new IllegalStateException(
				"Illegal attempt to set lock mode on non-native query via hint; use Query#setLockMode instead"
		);
	}

	protected boolean applySynchronizeSpacesHint(Object value) {
		throw new IllegalStateException(
				"Illegal attempt to set synchronized spaces on non-native query via hint"
		);
	}

	@Override
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( queryOptions.getAppliedGraph() != null && queryOptions.getAppliedGraph().getSemantic() != null ) {
			hints.put(
					queryOptions.getAppliedGraph().getSemantic().getJpaHintName(),
					queryOptions.getAppliedGraph().getGraph()
			);
		}
	}

	@Override
	protected List<R> doList() {
		SqmUtil.verifyIsSelectStatement( getSqmStatement() );
		final SqmSelectStatement<?> selectStatement = (SqmSelectStatement<?>) getSqmStatement();

		getSession().prepareForQueryExecution( requiresTxn( getLockOptions().findGreatestLockMode() ) );
		final boolean containsCollectionFetches = selectStatement.containsCollectionFetches();
		final boolean hasLimit = queryOptions.hasLimit();
		final boolean needsDistincting = containsCollectionFetches && (
				selectStatement.usesDistinct() ||
						queryOptions.getGraph() != null ||
						hasLimit
		);
		final ExecutionContext executionContextToUse;
		if ( hasLimit && containsCollectionFetches ) {
			boolean fail = getSessionFactory().getSessionFactoryOptions().isFailOnPaginationOverCollectionFetchEnabled();
			if (fail) {
				throw new HibernateException(
						"firstResult/maxResults specified with collection fetch. " +
								"In memory pagination was about to be applied. " +
								"Failing because 'Fail on pagination over collection fetch' is enabled."
				);
			}
			else {
				LOG.firstOrMaxResultsSpecifiedWithCollectionFetch();
			}
			executionContextToUse = SqlOmittingQueryOptions.omitSqlQueryOptions( this, true, false );
		}
		else {
			executionContextToUse = this;
		}

		final List<R> list = resolveSelectQueryPlan().performList( executionContextToUse );

		if ( needsDistincting ) {
			int includedCount = -1;
			// NOTE : firstRow is zero-based
			final int first = !hasLimit || queryOptions.getLimit().getFirstRow() == null
					? 0
					: queryOptions.getLimit().getFirstRow();
			final int max = !hasLimit || queryOptions.getLimit().getMaxRows() == null
					? -1
					: queryOptions.getLimit().getMaxRows();
			final List<R> tmp = new ArrayList<>( list.size() );
			final IdentitySet<R> distinction = new IdentitySet<>( list.size() );
			for ( final R result : list ) {
				if ( !distinction.add( result ) ) {
					continue;
				}
				includedCount++;
				if ( includedCount < first ) {
					continue;
				}
				tmp.add( result );
				// NOTE : ( max - 1 ) because first is zero-based while max is not...
				if ( max >= 0 && ( includedCount - first ) >= ( max - 1 ) ) {
					break;
				}
			}
			return tmp;
		}
		return list;
	}

	private boolean requiresTxn(LockMode lockMode) {
		return lockMode != null && lockMode.greaterThan( LockMode.READ );
	}

	@SuppressWarnings("unchecked")
	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		// resolve (or make) the QueryPlan.  This QueryPlan might be an aggregation of multiple plans.
		//
		// QueryPlans can be cached, except for in certain circumstances
		// 		- the determination of these circumstances occurs in SqmInterpretationsKey#generateFrom.
		//		If SqmInterpretationsKey#generateFrom returns null the query is not cacheable

		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.generateFrom( this );
		if ( cacheKey != null ) {
			return getSession().getFactory().getQueryEngine().getInterpretationCache().resolveSelectQueryPlan(
					cacheKey,
					this::buildSelectQueryPlan
			);
		}
		else {
			return buildSelectQueryPlan();
		}
	}

	private SelectQueryPlan<R> buildSelectQueryPlan() {
		final SqmSelectStatement<R>[] concreteSqmStatements = QuerySplitter.split(
				(SqmSelectStatement<R>) getSqmStatement(),
				getSessionFactory()
		);

		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedSelectQueryPlan( concreteSqmStatements );
		}
		else {
			return buildConcreteSelectQueryPlan(
					concreteSqmStatements[0],
					getResultType(),
					getQueryOptions()
			);
		}
	}

	@SuppressWarnings("unchecked")
	private SelectQueryPlan<R> buildAggregatedSelectQueryPlan(SqmSelectStatement<R>[] concreteSqmStatements) {
		final SelectQueryPlan<R>[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];

		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteSelectQueryPlan(
					concreteSqmStatements[i],
					getResultType(),
					getQueryOptions()
			);
		}

		return new AggregatedSelectQueryPlanImpl( aggregatedQueryPlans );
	}

	private SelectQueryPlan<R> buildConcreteSelectQueryPlan(
			SqmSelectStatement<R> concreteSqmStatement,
			Class<R> resultType,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				domainParameterXref,
				resultType,
				queryOptions
		);
	}

	@Override
	public ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode) {
		SqmUtil.verifyIsSelectStatement( getSqmStatement() );
		getSession().prepareForQueryExecution( requiresTxn( getLockOptions().findGreatestLockMode() ) );

		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}

	@Override
	protected int doExecuteUpdate() {
		SqmUtil.verifyIsNonSelectStatement( getSqmStatement() );
		getSession().prepareForQueryExecution( true );

		return resolveNonSelectQueryPlan().executeUpdate( this );
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		// resolve (or make) the QueryPlan.

		NonSelectQueryPlan queryPlan = null;

		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.generateNonSelectKey( this );
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryEngine().getInterpretationCache().getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = buildNonSelectQueryPlan();
			if ( cacheKey != null ) {
				getSession().getFactory().getQueryEngine().getInterpretationCache().cacheNonSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	private NonSelectQueryPlan buildNonSelectQueryPlan() {
		// to get here the SQM statement has already been validated to be
		// a non-select variety...
		if ( getSqmStatement() instanceof SqmDeleteStatement<?> ) {
			return buildDeleteQueryPlan();
		}

		if ( getSqmStatement() instanceof SqmUpdateStatement<?> ) {
			return buildUpdateQueryPlan();
		}

		if ( getSqmStatement() instanceof SqmInsertStatement<?> ) {
			return buildInsertQueryPlan();
		}

		throw new NotYetImplementedException( "Query#executeUpdate for Statements of type [" + getSqmStatement() + "not yet supported" );
	}

	private NonSelectQueryPlan buildDeleteQueryPlan() {
		final SqmDeleteStatement<R> sqmDelete = (SqmDeleteStatement<R>) getSqmStatement();

		final String entityNameToDelete = sqmDelete.getTarget().getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToDelete );

		final SqmMultiTableMutationStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableMutationStrategy();
		if ( multiTableStrategy == null ) {
			return new SimpleDeleteQueryPlan( entityDescriptor, sqmDelete, domainParameterXref );
		}
		else {
			return new MultiTableDeleteQueryPlan( sqmDelete, domainParameterXref, multiTableStrategy );
		}
	}

	private NonSelectQueryPlan buildUpdateQueryPlan() {
		final SqmUpdateStatement<R> sqmUpdate = (SqmUpdateStatement<R>) getSqmStatement();

		final String entityNameToUpdate = sqmUpdate.getTarget().getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToUpdate );

		final SqmMultiTableMutationStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableMutationStrategy();
		if ( multiTableStrategy == null ) {
			return new SimpleUpdateQueryPlan( sqmUpdate, domainParameterXref );
		}
		else {
			return new MultiTableUpdateQueryPlan( sqmUpdate, domainParameterXref, multiTableStrategy );
		}
	}

	private NonSelectQueryPlan buildInsertQueryPlan() {
		final SqmInsertStatement<R> sqmInsert = (SqmInsertStatement<R>) getSqmStatement();

//		final String entityNameToUpdate = sqmInsert.getTarget().getReferencedPathSource().getHibernateEntityName();
//		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToUpdate );

//		final SqmMultiTableMutationStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableMutationStrategy();
//		if ( multiTableStrategy == null ) {
			return new SimpleInsertQueryPlan( sqmInsert, domainParameterXref );
//		}
//		else {
			//TODO:
//			return new MultiTableUpdateQueryPlan( sqmInsert, domainParameterXref, multiTableStrategy );
//		}
	}

	@Override
	protected void prepareForExecution() {
		super.prepareForExecution();
		// Reset the callback before every execution
		callback = null;
	}

	@Override
	public Callback getCallback() {
		if ( callback == null ) {
			callback = new CallbackImpl();
		}
		return callback;
	}

	@Override
	public void invokeAfterLoadActions(SharedSessionContractImplementor session, Object entity, Loadable persister) {
		if ( callback != null ) {
			callback.invokeAfterLoadActions( session, entity, persister );
		}
	}

	@Override
	public String getQueryIdentifier(String sql) {
		if ( CRITERIA_HQL_STRING.equals( hqlString ) ) {
			return "[CRITERIA] " + sql;
		}
		return hqlString;
	}

	@Override
	public NamedHqlQueryMemento toMemento(String name) {
		return new NamedHqlQueryMementoImpl(
				name,
				hqlString,
				getFirstResult(),
				getMaxResults(),
				isCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getHibernateFlushMode(),
				isReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				Collections.emptyMap(),
				getHints()
		);
	}
}
