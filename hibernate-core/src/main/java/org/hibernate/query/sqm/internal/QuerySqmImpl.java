/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TemporalType;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.TypeMismatchException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BindableType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.Query;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.criteria.internal.NamedCriteriaQueryMementoImpl;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.AbstractSelectionQuery;
import org.hibernate.query.spi.DelegatingQueryOptions;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.internal.SqmInterpretationsKey.InterpretationsKeySource;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_CACHE_STORE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_RETRIEVE_MODE;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_CACHE_STORE_MODE;
import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;
import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptionsWithUniqueSemanticFilter;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelect;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R>
		extends AbstractSelectionQuery<R>
		implements SqmQueryImplementor<R>, InterpretationsKeySource, DomainQueryExecutionContext {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QuerySqmImpl.class );

	private final String hql;
	private final SqmStatement<R> sqm;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;

	private final QueryParameterBindingsImpl parameterBindings;

	private final Class<R> resultType;
	private final TupleMetadata tupleMetadata;

	/**
	 * Creates a Query instance from a named HQL memento
	 */
	public QuerySqmImpl(
			NamedHqlQueryMementoImpl memento,
			Class<R> expectedResultType,
			SharedSessionContractImplementor session) {
		super( session );

		this.hql = memento.getHqlString();
		this.resultType = expectedResultType;

		final SessionFactoryImplementor factory = session.getFactory();
		final QueryEngine queryEngine = factory.getQueryEngine();
		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();
		final HqlInterpretation hqlInterpretation = interpretationCache.resolveHqlInterpretation(
				hql,
				expectedResultType,
				(s) -> queryEngine.getHqlTranslator().translate( hql, expectedResultType )
		);

		this.sqm = hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		validateStatement( sqm, resultType );
		setComment( hql );


		applyOptions( memento );
		this.tupleMetadata = buildTupleMetadata( sqm, resultType );
	}

	public QuerySqmImpl(
			NamedCriteriaQueryMementoImpl memento,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		this( memento.getSqmStatement(), resultType, session );

		applyOptions( memento );
	}

	/**
	 * Form used for HQL queries
	 */
	@SuppressWarnings("unchecked")
	public QuerySqmImpl(
			String hql,
			HqlInterpretation hqlInterpretation,
			Class<R> resultType,
			SharedSessionContractImplementor session) {
		super( session );
		this.hql = hql;
		this.resultType = resultType;

		this.sqm = hqlInterpretation.getSqmStatement();

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = QueryParameterBindingsImpl.from( parameterMetadata, session.getFactory() );

		validateStatement( sqm, resultType );
		setComment( hql );

		this.tupleMetadata = buildTupleMetadata( sqm, resultType );
	}

	/**
	 * Form used for criteria queries
	 */
	public QuerySqmImpl(
			SqmStatement<R> criteria,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );
		this.hql = CRITERIA_HQL_STRING;
		if ( producer.isCriteriaCopyTreeEnabled() ) {
			this.sqm = criteria.copy( SqmCopyContext.simpleContext() );
		}
		else {
			this.sqm = criteria;
		}

		setComment( hql );

		this.domainParameterXref = DomainParameterXref.from( this.sqm );
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

		if ( sqm instanceof SqmSelectStatement<?> ) {
			SqmUtil.verifyIsSelectStatement( sqm, null );
			final SqmQueryPart<R> queryPart = ( (SqmSelectStatement<R>) sqm ).getQueryPart();
			// For criteria queries, we have to validate the fetch structure here
			queryPart.validateQueryStructureAndFetchOwners();
			visitQueryReturnType(
					queryPart,
					resultType,
					producer.getFactory()
			);
		}
		else {
			if ( resultType != null ) {
				throw new IllegalQueryOperationException(
						"Result type given for a non-SELECT Query",
						hql,
						null
				);
			}
			if ( sqm instanceof SqmUpdateStatement<?> ) {
				final SqmUpdateStatement<R> updateStatement = (SqmUpdateStatement<R>) sqm;
				verifyImmutableEntityUpdate( CRITERIA_HQL_STRING, updateStatement, producer.getFactory() );
				if ( updateStatement.getSetClause() == null || updateStatement.getSetClause()
						.getAssignments()
						.isEmpty() ) {
					throw new IllegalArgumentException( "No assignments specified as part of UPDATE criteria" );
				}
			}
			else if ( sqm instanceof SqmInsertStatement<?> ) {
				verifyInsertTypesMatch( CRITERIA_HQL_STRING, (SqmInsertStatement<R>) sqm );
			}
		}

		this.resultType = resultType;
		this.tupleMetadata = buildTupleMetadata( criteria, resultType );
	}

	private void validateStatement(SqmStatement<R> sqmStatement, Class<R> resultType) {
		if ( sqmStatement instanceof SqmSelectStatement<?> ) {
			SqmUtil.verifyIsSelectStatement( sqmStatement, hql );
		}
		else {
			if ( resultType != null ) {
				throw new IllegalQueryOperationException(
						"Result type given for a non-SELECT Query",
						hql,
						null
				);
			}
			if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
				final SqmUpdateStatement<R> updateStatement = (SqmUpdateStatement<R>) sqmStatement;
				verifyImmutableEntityUpdate( hql, updateStatement, getSessionFactory() );
				verifyUpdateTypesMatch( hql, updateStatement );
			}
			else if ( sqmStatement instanceof SqmInsertStatement<?> ) {
				verifyInsertTypesMatch( hql, (SqmInsertStatement<R>) sqmStatement );
			}
		}
	}

	private void verifyImmutableEntityUpdate(
			String hqlString,
			SqmUpdateStatement<R> sqmStatement,
			SessionFactoryImplementor factory) {
		final EntityPersister entityDescriptor = factory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( sqmStatement.getTarget().getEntityName() );
		if ( entityDescriptor.isMutable() ) {
			return;
		}
		final ImmutableEntityUpdateQueryHandlingMode immutableEntityUpdateQueryHandlingMode = factory
				.getSessionFactoryOptions()
				.getImmutableEntityUpdateQueryHandlingMode();

		final String querySpaces = Arrays.toString( entityDescriptor.getQuerySpaces() );

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
						"The " + immutableEntityUpdateQueryHandlingMode + " is not supported"
				);
		}
	}

	private void verifyUpdateTypesMatch(String hqlString, SqmUpdateStatement<R> sqmStatement) {
		final List<SqmAssignment<?>> assignments = sqmStatement.getSetClause().getAssignments();
		for ( int i = 0; i < assignments.size(); i++ ) {
			final SqmAssignment<?> assignment = assignments.get( i );
			final SqmPath<?> targetPath = assignment.getTargetPath();
			final SqmExpression<?> expression = assignment.getValue();
			if ( targetPath.getNodeJavaType() == null || expression.getNodeJavaType() == null ) {
				continue;
			}
			if ( targetPath.getNodeJavaType() != expression.getNodeJavaType()
					&& !targetPath.getNodeJavaType().isWider( expression.getNodeJavaType() ) ) {
				throw new SemanticException(
						String.format(
								"The assignment expression type [%s] did not match the assignment path type [%s] for the path [%s]",
								expression.getNodeJavaType().getJavaType().getTypeName(),
								targetPath.getNodeJavaType().getJavaType().getTypeName(),
								targetPath.toHqlString()
						),
						hqlString,
						null
				);
			}
		}
	}

	private void verifyInsertTypesMatch(String hqlString, SqmInsertStatement<R> sqmStatement) {
		final List<SqmPath<?>> insertionTargetPaths = sqmStatement.getInsertionTargetPaths();
		if ( sqmStatement instanceof SqmInsertValuesStatement<?> ) {
			final SqmInsertValuesStatement<R> statement = (SqmInsertValuesStatement<R>) sqmStatement;
			for ( SqmValues sqmValues : statement.getValuesList() ) {
				verifyInsertTypesMatch( hqlString, insertionTargetPaths, sqmValues.getExpressions() );
			}
		}
		else {
			final SqmInsertSelectStatement<R> statement = (SqmInsertSelectStatement<R>) sqmStatement;
			final List<SqmSelectableNode<?>> selections = statement.getSelectQueryPart()
					.getFirstQuerySpec()
					.getSelectClause()
					.getSelectionItems();
			verifyInsertTypesMatch( hqlString, insertionTargetPaths, selections );
			statement.getSelectQueryPart().validateQueryStructureAndFetchOwners();
		}
	}

	private void verifyInsertTypesMatch(
			String hqlString,
			List<SqmPath<?>> insertionTargetPaths,
			List<? extends SqmTypedNode<?>> expressions) {
		final int size = insertionTargetPaths.size();
		final int expressionsSize = expressions.size();
		if ( size != expressionsSize ) {
			throw new SemanticException(
				String.format(
						"Expected insert attribute count [%d] did not match Query selection count [%d]",
						size,
						expressionsSize
				),
				hqlString,
				null
			);
		}
		for ( int i = 0; i < expressionsSize; i++ ) {
			final SqmTypedNode<?> expression = expressions.get( i );
			if ( expression.getNodeJavaType() == null ) {
				continue;
			}
			if ( insertionTargetPaths.get( i ).getJavaTypeDescriptor() != expression.getNodeJavaType() ) {
				throw new SemanticException(
						String.format(
								"Expected insert attribute type [%s] did not match Query selection type [%s] at selection index [%d]",
								insertionTargetPaths.get( i ).getJavaTypeDescriptor().getJavaType().getTypeName(),
								expression.getNodeJavaType().getJavaType().getTypeName(),
								i
						),
						hqlString,
						null
				);
			}
		}
	}

	public TupleMetadata getTupleMetadata() {
		return tupleMetadata;
	}

	@Override
	public String getQueryString() {
		return hql;
	}

	public SqmStatement getSqmStatement() {
		return sqm;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
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
	public QueryParameterBindings getParameterBindings() {
		return getQueryParameterBindings();
	}

	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return getSession().getLoadQueryInfluencers();
	}

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		// No need to resolve JDBC parameter types as we know them from the SQM model
		return false;
	}

	@Override
	public Supplier<Boolean> hasMultiValuedParameterBindingsChecker() {
		return this::hasMultiValuedParameterBindings;
	}

	protected boolean hasMultiValuedParameterBindings() {
		return getQueryParameterBindings().hasAnyMultiValuedBindings()
				|| getParameterMetadata().hasAnyMatching( QueryParameter::allowsMultiValuedBinding );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// select execution

	@Override
	protected void prepareForExecution() {
		// Reset the callback before every execution
		resetCallback();
	}

	protected void verifySelect() {
		try {
			SqmUtil.verifyIsSelectStatement( getSqmStatement(), hql );
		}
		catch (IllegalQueryOperationException e) {
			// per JPA
			throw new IllegalStateException( "Expecting a SELECT query : `" + hql + "`", e );
		}
	}

	protected List<R> doList() {
		verifySelect();
		getSession().prepareForQueryExecution( requiresTxn( getQueryOptions().getLockOptions().findGreatestLockMode() ) );

		final SqmSelectStatement<?> sqmStatement = (SqmSelectStatement<?>) getSqmStatement();
		final boolean containsCollectionFetches = sqmStatement.containsCollectionFetches();
		final boolean hasLimit = hasLimit( sqmStatement, getQueryOptions() );
		final boolean needsDistinct = containsCollectionFetches
				&& ( sqmStatement.usesDistinct() || hasAppliedGraph( getQueryOptions() ) || hasLimit );

		final DomainQueryExecutionContext executionContextToUse;
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
				QueryLogging.QUERY_MESSAGE_LOGGER.firstOrMaxResultsSpecifiedWithCollectionFetch();
			}

			final MutableQueryOptions originalQueryOptions = getQueryOptions();
			final QueryOptions normalizedQueryOptions;
			if ( needsDistinct ) {
				normalizedQueryOptions = omitSqlQueryOptionsWithUniqueSemanticFilter( originalQueryOptions, true, false );
			}
			else {
				normalizedQueryOptions = omitSqlQueryOptions( originalQueryOptions, true, false );
			}
			if ( originalQueryOptions == normalizedQueryOptions ) {
				executionContextToUse = this;
			}
			else {
				executionContextToUse = new DelegatingDomainQueryExecutionContext( this ) {
					@Override
					public QueryOptions getQueryOptions() {
						return normalizedQueryOptions;
					}
				};
			}
		}
		else {
			if ( needsDistinct ) {
				final MutableQueryOptions originalQueryOptions = getQueryOptions();
				final QueryOptions normalizedQueryOptions = uniqueSemanticQueryOptions( originalQueryOptions );
				if ( originalQueryOptions == normalizedQueryOptions ) {
					executionContextToUse = this;
				}
				else {
					executionContextToUse = new DelegatingDomainQueryExecutionContext( this ) {
						@Override
						public QueryOptions getQueryOptions() {
							return normalizedQueryOptions;
						}
					};
				}
			}
			else {
				executionContextToUse = this;
			}
		}

		final List<R> list = resolveSelectQueryPlan().performList( executionContextToUse );

		if ( needsDistinct ) {
			final int first = !hasLimit || getQueryOptions().getLimit().getFirstRow() == null
					? getIntegerLiteral( sqmStatement.getOffset(), 0 )
					: getQueryOptions().getLimit().getFirstRow();
			final int max = !hasLimit || getQueryOptions().getLimit().getMaxRows() == null
					? getMaxRows( sqmStatement, list.size() )
					: getQueryOptions().getLimit().getMaxRows();
			if ( first > 0 || max != -1 ) {
				final int toIndex;
				final int resultSize = list.size();
				if ( max != -1 ) {
					toIndex = first + max;
				}
				else {
					toIndex = resultSize;
				}
				return list.subList( first, toIndex > resultSize ? resultSize : toIndex );
			}
		}
		return list;

	}

	public static QueryOptions uniqueSemanticQueryOptions(QueryOptions originalOptions) {
		final ListResultsConsumer.UniqueSemantic semantic = originalOptions.getUniqueSemantic();


		if ( semantic == ListResultsConsumer.UniqueSemantic.FILTER ) {
			return originalOptions;
		}

		return new UniqueSemanticFilterQueryOption( originalOptions );
	}

	public static class UniqueSemanticFilterQueryOption extends DelegatingQueryOptions{

		public UniqueSemanticFilterQueryOption(QueryOptions queryOptions) {
			super( queryOptions );
		}

		@Override
		public ListResultsConsumer.UniqueSemantic getUniqueSemantic() {
			return ListResultsConsumer.UniqueSemantic.FILTER;
		}
	}

	@Override
	protected ScrollableResultsImplementor doScroll(ScrollMode scrollMode) {
		getSession().prepareForQueryExecution( requiresTxn( getQueryOptions().getLockOptions().findGreatestLockMode() ) );
		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select query plan

	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.createInterpretationsKey( this );
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
				getSession().getFactory()
		);

		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedSelectQueryPlan( concreteSqmStatements );
		}
		else {
			return buildConcreteSelectQueryPlan( concreteSqmStatements[0], getResultType(), getQueryOptions() );
		}
	}

	private SelectQueryPlan<R> buildAggregatedSelectQueryPlan(SqmSelectStatement<?>[] concreteSqmStatements) {
		//noinspection unchecked
		final SelectQueryPlan<R>[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];

		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteSelectQueryPlan(
					concreteSqmStatements[i],
					getResultType(),
					getQueryOptions()
			);
		}

		return new AggregatedSelectQueryPlanImpl<>( aggregatedQueryPlans );
	}

	private <T> SelectQueryPlan<T> buildConcreteSelectQueryPlan(
			SqmSelectStatement<?> concreteSqmStatement,
			Class<T> resultType,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				getQueryString(),
				getDomainParameterXref(),
				resultType,
				tupleMetadata,
				queryOptions
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update / delete / insert query execution

	@Override
	public int executeUpdate() {
		verifyUpdate();
		getSession().checkTransactionNeededForUpdateOperation( "Executing an update/delete query" );
		beforeQuery();
		boolean success = false;
		try {
			final int result = doExecuteUpdate();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (TypeMismatchException e) {
			throw new IllegalArgumentException( e );
		}
		catch (HibernateException e) {
			throw getSession().getExceptionConverter().convert( e );
		}
		finally {
			afterQuery( success );
		}
	}

	private void verifyUpdate() {
		try {
			SqmUtil.verifyIsNonSelectStatement( getSqmStatement(), hql );
		}
		catch (IllegalQueryOperationException e) {
			// per JPA
			throw new IllegalStateException( "Expecting a non-SELECT query : `" + hql + "`", e );
		}
	}

	protected int doExecuteUpdate() {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private NonSelectQueryPlan buildDeleteQueryPlan() {
		final SqmDeleteStatement[] concreteSqmStatements = QuerySplitter.split(
				(SqmDeleteStatement) getSqmStatement(),
				getSessionFactory()
		);

		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedDeleteQueryPlan( concreteSqmStatements );
		}
		else {
			return buildConcreteDeleteQueryPlan( concreteSqmStatements[0] );
		}
	}

	private NonSelectQueryPlan buildConcreteDeleteQueryPlan(@SuppressWarnings("rawtypes") SqmDeleteStatement sqmDelete) {
		final EntityDomainType<?> entityDomainType = sqmDelete.getTarget().getModel();
		final String entityNameToDelete = entityDomainType.getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityNameToDelete );
		final SqmMultiTableMutationStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableMutationStrategy();
		if ( multiTableStrategy == null ) {
			return new SimpleDeleteQueryPlan( entityDescriptor, sqmDelete, domainParameterXref );
		}
		else {
			return new MultiTableDeleteQueryPlan( sqmDelete, domainParameterXref, multiTableStrategy );
		}
	}

	private NonSelectQueryPlan buildAggregatedDeleteQueryPlan(@SuppressWarnings("rawtypes") SqmDeleteStatement[] concreteSqmStatements) {
		final NonSelectQueryPlan[] aggregatedQueryPlans = new NonSelectQueryPlan[ concreteSqmStatements.length ];

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteDeleteQueryPlan( concreteSqmStatements[i] );
		}

		return new AggregatedNonSelectQueryPlanImpl( aggregatedQueryPlans );
	}

	private NonSelectQueryPlan buildUpdateQueryPlan() {
		//noinspection rawtypes
		final SqmUpdateStatement sqmUpdate = (SqmUpdateStatement) getSqmStatement();

		final String entityNameToUpdate = sqmUpdate.getTarget().getModel().getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityNameToUpdate );

		final SqmMultiTableMutationStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableMutationStrategy();
		if ( multiTableStrategy == null ) {
			return new SimpleUpdateQueryPlan( sqmUpdate, domainParameterXref );
		}
		else {
			return new MultiTableUpdateQueryPlan( sqmUpdate, domainParameterXref, multiTableStrategy );
		}
	}

	private NonSelectQueryPlan buildInsertQueryPlan() {
		//noinspection rawtypes
		final SqmInsertStatement sqmInsert = (SqmInsertStatement) getSqmStatement();

		final String entityNameToInsert = sqmInsert.getTarget().getModel().getHibernateEntityName();
		final AbstractEntityPersister entityDescriptor = (AbstractEntityPersister) getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityNameToInsert );

		boolean useMultiTableInsert = entityDescriptor.isMultiTable();
		if ( !useMultiTableInsert && !isSimpleValuesInsert( sqmInsert, entityDescriptor ) ) {
			final IdentifierGenerator identifierGenerator = entityDescriptor.getIdentifierGenerator();
			if ( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator && identifierGenerator instanceof OptimizableGenerator ) {
				final Optimizer optimizer = ( (OptimizableGenerator) identifierGenerator ).getOptimizer();
				if ( optimizer != null && optimizer.getIncrementSize() > 1 ) {
					useMultiTableInsert = !hasIdentifierAssigned( sqmInsert, entityDescriptor );
				}
			}
		}
		if ( !useMultiTableInsert ) {
			return new SimpleInsertQueryPlan( sqmInsert, domainParameterXref );
		}
		else {
			return new MultiTableInsertQueryPlan(
					sqmInsert,
					domainParameterXref,
					entityDescriptor.getSqmMultiTableInsertStrategy()
			);
		}
	}

	private boolean hasIdentifierAssigned(SqmInsertStatement<?> sqmInsert, EntityPersister entityDescriptor) {
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final String partName;
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			partName = ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName();
		}
		else {
			partName = EntityIdentifierMapping.ROLE_LOCAL_NAME;
		}
		for ( SqmPath<?> insertionTargetPath : sqmInsert.getInsertionTargetPaths() ) {
			final SqmPath<?> lhs = insertionTargetPath.getLhs();
			if ( !( lhs instanceof SqmRoot<?> ) ) {
				continue;
			}
			final SqmPathSource<?> referencedPathSource = insertionTargetPath.getReferencedPathSource();
			if ( referencedPathSource.getPathName().equals( partName ) ) {
				return true;
			}
		}

		return false;
	}

	private boolean isSimpleValuesInsert(SqmInsertStatement<?> sqmInsert, EntityPersister entityDescriptor) {
		// Simple means that we can translate the statement to a single plain insert
		return sqmInsert instanceof SqmInsertValuesStatement
				// An insert is only simple if no SqmMultiTableMutation strategy is available,
				// as the presence of it means the entity has multiple tables involved,
				// in which case we currently need to use the MultiTableInsertQueryPlan
				&& entityDescriptor.getSqmMultiTableMutationStrategy() == null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// QueryOptions

	@Override
	public SqmQueryImplementor<R> addQueryHint(String hint) {
		getQueryOptions().addDatabaseHint( hint );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	public SqmQueryImplementor<R> setLockOptions(LockOptions lockOptions) {
		// No verifySelect call, because in Hibernate we support locking in subqueries
		getQueryOptions().getLockOptions().overlay( lockOptions );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setLockMode(String alias, LockMode lockMode) {
		// No verifySelect call, because in Hibernate we support locking in subqueries
		super.setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public <T> SqmQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer) {
		applyTupleTransformer( transformer );
		//noinspection unchecked
		return (SqmQueryImplementor<T>) this;
	}

	@Override
	public SqmQueryImplementor<R> setResultListTransformer(ResultListTransformer transformer) {
		applyResultListTransformer( transformer );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setMaxResults(int maxResult) {
		applyMaxResults( maxResult );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFirstResult(int startPosition) {
		applyFirstResult( startPosition );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFlushMode(FlushModeType flushMode) {
		applyJpaFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setLockMode(LockModeType lockMode) {
		if ( lockMode != LockModeType.NONE ) {
			// JPA requires an exception to be thrown when this is not a select statement
			verifySelect();
		}
		getSession().checkOpen( false );
		getQueryOptions().getLockOptions().setLockMode( LockMode.fromJpaLockMode( lockMode ) );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		verifySelect();
		getSession().checkOpen( false );
		return getLockOptions().getLockMode().toJpaLockMode();
	}

	@Override
	public FlushModeType getFlushMode() {
		return getJpaFlushMode();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// hints

	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( isReadOnly() ) {
			hints.put( HINT_READ_ONLY, true );
		}

		putIfNotNull( hints, HINT_FETCH_SIZE, getFetchSize() );

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );
			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );

			putIfNotNull( hints, HINT_SPEC_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_SPEC_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, HINT_JAVAEE_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		}

		final AppliedGraph appliedGraph = getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() != null ) {
			hints.put( appliedGraph.getSemantic().getJakartaHintName(), appliedGraph );
			hints.put( appliedGraph.getSemantic().getJpaHintName(), appliedGraph );
		}

		putIfNotNull( hints, HINT_FOLLOW_ON_LOCKING, getQueryOptions().getLockOptions().getFollowOnLocking() );
	}

	@Override
	public SqmQueryImplementor<R> setHint(String hintName, Object value) {
		applyHint( hintName, value );
		return this;
	}

	@Override
	protected void applyLockTimeoutHint(Integer timeout) {
		if ( isSelect( sqm ) ) {
			super.applyLockTimeoutHint( timeout );
		}
	}

	@Override
	protected void applyLockTimeoutHint(int timeout) {
		if ( isSelect( sqm ) ) {
			super.applyLockTimeoutHint( timeout );
		}
	}

	@Override
	protected void applyHibernateLockMode(LockMode value) {
		if ( isSelect( sqm ) ) {
			super.applyHibernateLockMode( value );
		}
	}

	@Override
	protected void applyLockModeType(LockModeType value) {
		if ( isSelect( sqm ) ) {
			super.applyLockModeType( value );
		}
	}

	@Override
	protected void applyAliasSpecificLockModeHint(String hintName, Object value) {
		if ( isSelect( sqm ) ) {
			super.applyAliasSpecificLockModeHint( hintName, value );
		}
	}

	@Override
	protected void applyFollowOnLockingHint(Boolean followOnLocking) {
		if ( isSelect( sqm ) ) {
			super.applyFollowOnLockingHint( followOnLocking );
		}
	}

	@Override
	public SqmQueryImplementor<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic) {
		getQueryOptions().applyGraph( (RootGraphImplementor<?>) graph, semantic );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// select execution

	@Override
	public List<R> list() {
		//noinspection unchecked
		return super.list();
	}

	@Override
	public ScrollableResultsImplementor<R> scroll() {
		//noinspection unchecked
		return super.scroll();
	}

	@Override
	public ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode) {
		//noinspection unchecked
		return super.scroll( scrollMode );
	}

	@Override
	public Stream<R> stream() {
		//noinspection unchecked
		return super.stream();
	}

	@Override
	public R uniqueResult() {
		//noinspection unchecked
		return (R) super.uniqueResult();
	}

	@Override
	public R getSingleResult() {
		//noinspection unchecked
		return (R) super.getSingleResult();
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		//noinspection unchecked
		return super.uniqueResultOptional();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named query externalization

	@Override
	public NamedQueryMemento toMemento(String name) {
		if ( CRITERIA_HQL_STRING.equals( getQueryString() ) ) {
			final SqmStatement sqmStatement ;
			if ( !getSession().isCriteriaCopyTreeEnabled() ) {
				sqmStatement = getSqmStatement().copy( SqmCopyContext.simpleContext() );
			}
			else {
				// the statement has already been copied
				sqmStatement = getSqmStatement();
			}
			return new NamedCriteriaQueryMementoImpl(
					name,
					sqmStatement,
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

		return new NamedHqlQueryMementoImpl(
				name,
				getQueryString(),
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// unwrap

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

		if ( cls.isInstance( sqm ) ) {
			return (T) sqm;
		}

		if ( cls.isInstance( getQueryOptions() ) ) {
			return (T) getQueryOptions();
		}

		if ( cls.isInstance( getQueryOptions().getAppliedGraph() ) ) {
			return (T) getQueryOptions().getAppliedGraph();
		}

		if ( EntityGraphQueryHint.class.isAssignableFrom( cls ) ) {
			return (T) new EntityGraphQueryHint( getQueryOptions().getAppliedGraph() );
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance


	@Override
	public SqmQueryImplementor<R> setComment(String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setProperties(Map bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// optional object loading

	@Override
	public void setOptionalId(Serializable id) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalEntityName(String entityName) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalObject(Object optionalObject) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}
}
