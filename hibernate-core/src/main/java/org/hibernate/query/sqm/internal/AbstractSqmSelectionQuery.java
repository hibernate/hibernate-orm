/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.type.BindableType;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Page;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.spi.AbstractSelectionQuery;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.results.internal.TupleMetadata;

import java.util.List;

import jakarta.persistence.TupleElement;
import jakarta.persistence.criteria.CompoundSelection;

import static org.hibernate.cfg.QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH;
import static org.hibernate.query.KeyedPage.KeyInterpretation.KEY_OF_FIRST_ON_NEXT_PAGE;
import static org.hibernate.query.QueryLogging.QUERY_MESSAGE_LOGGER;
import static org.hibernate.query.sqm.internal.KeyedResult.collectKeys;
import static org.hibernate.query.sqm.internal.KeyedResult.collectResults;
import static org.hibernate.query.sqm.internal.SqmUtil.isHqlTuple;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelectionAssignableToResultType;

/**
 * @author Gavin King
 */
abstract class AbstractSqmSelectionQuery<R> extends AbstractSelectionQuery<R> {

	AbstractSqmSelectionQuery(SharedSessionContractImplementor session) {
		super(session);
	}

	AbstractSqmSelectionQuery(AbstractSqmSelectionQuery<?> original) {
		super( original );
	}

	protected int max(boolean hasLimit, SqmSelectStatement<?> sqmStatement, List<R> list) {
		return !hasLimit || getQueryOptions().getLimit().getMaxRows() == null
				? getMaxRows( sqmStatement, list.size() )
				: getQueryOptions().getLimit().getMaxRows();
	}

	protected int first(boolean hasLimit, SqmSelectStatement<?> sqmStatement) {
		return !hasLimit || getQueryOptions().getLimit().getFirstRow() == null
				? getIntegerLiteral( sqmStatement.getOffset(), 0 )
				: getQueryOptions().getLimit().getFirstRow();
	}

	protected static boolean hasLimit(SqmSelectStatement<?> sqm, MutableQueryOptions queryOptions) {
		return queryOptions.hasLimit() || sqm.getFetch() != null || sqm.getOffset() != null;
	}

	protected boolean needsDistinct(boolean containsCollectionFetches, boolean hasLimit, SqmSelectStatement<?> sqmStatement) {
		return containsCollectionFetches
			&& ( hasLimit || sqmStatement.usesDistinct() || hasAppliedGraph( getQueryOptions() ) );
	}

	protected static boolean hasAppliedGraph(MutableQueryOptions queryOptions) {
		final var appliedGraph = queryOptions.getAppliedGraph();
		return appliedGraph != null && appliedGraph.getSemantic() != null;
	}

	protected void errorOrLogForPaginationWithCollectionFetch() {
		if ( getSessionFactory().getSessionFactoryOptions()
				.isFailOnPaginationOverCollectionFetchEnabled() ) {
			throw new HibernateException(
					"setFirstResult() or setMaxResults() specified with collection fetch join "
							+ "(in-memory pagination was about to be applied, but '"
							+ FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH
							+ "' is enabled)"
			);
		}
		else {
			QUERY_MESSAGE_LOGGER.firstOrMaxResultsSpecifiedWithCollectionFetch();
		}
	}

	public abstract SqmStatement<R> getSqmStatement();
	protected abstract void setSqmStatement(SqmSelectStatement<R> statement);
	public abstract DomainParameterXref getDomainParameterXref();
	public abstract TupleMetadata getTupleMetadata();

	protected void copyParameterBindings(QueryParameterBindings oldParameterBindings) {
		final var parameterBindings = getQueryParameterBindings();
		oldParameterBindings.visitBindings( (queryParameter, binding) -> {
			if ( binding.isBound() ) {
				//noinspection unchecked
				final var newBinding = (QueryParameterBinding<Object>) parameterBindings.getBinding( queryParameter );
				//noinspection unchecked
				final var bindType = (BindableType<Object>) binding.getBindType();
				final var explicitTemporalPrecision = binding.getExplicitTemporalPrecision();
				if ( binding.isMultiValued() ) {
					final var bindValues = binding.getBindValues();
					if ( explicitTemporalPrecision != null ) {
						newBinding.setBindValues( bindValues, explicitTemporalPrecision, getTypeConfiguration() );
					}
					else if ( bindType != null ) {
						newBinding.setBindValues( bindValues, bindType );
					}
					else {
						newBinding.setBindValues( bindValues );
					}
				}
				else {
					final var bindValue = binding.getBindValue();
					if ( explicitTemporalPrecision != null ) {
						newBinding.setBindValue( bindValue, explicitTemporalPrecision );
					}
					else if ( bindType != null ) {
						newBinding.setBindValue( bindValue, bindType );
					}
					else {
						newBinding.setBindValue( bindValue );
					}
				}
			}
		} );

		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		bindValueBindCriteriaParameters( getDomainParameterXref(), parameterBindings );
	}

	protected static void bindValueBindCriteriaParameters(
			DomainParameterXref domainParameterXref,
			QueryParameterBindings bindings) {
		for ( var entry : domainParameterXref.getQueryParameters().entrySet() ) {
			final var sqmParameter = entry.getValue().get( 0 );
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> wrapper ) {
				@SuppressWarnings("unchecked")
				final var criteriaParameter = (JpaCriteriaParameter<Object>) wrapper.getJpaCriteriaParameter();
				final var value = criteriaParameter.getValue();
				// We don't set a null value, unless the type is also null which
				// is the case when using HibernateCriteriaBuilder.value
				if ( value != null || criteriaParameter.getNodeType() == null ) {
					// Use the anticipated type for binding the value if possible
					//noinspection unchecked
					final var parameter = (QueryParameterImplementor<Object>) entry.getKey();
					bindings.getBinding( parameter )
							.setBindValue( value, criteriaParameter.getAnticipatedType() );
				}
			}
		}
	}

	@Override
	protected <P> QueryParameterImplementor<P> getQueryParameter(QueryParameterImplementor<P> parameter) {
		if ( parameter instanceof JpaCriteriaParameter<?> criteriaParameter ) {
			final var parameterWrapper = getDomainParameterXref().getParameterResolutions()
					.getJpaCriteriaParamResolutions()
					.get( criteriaParameter );
			//noinspection unchecked
			return (QueryParameterImplementor<P>) getDomainParameterXref().getQueryParameter( parameterWrapper );
		}
		else {
			return parameter;
		}
	}

	public int @Nullable [] unnamedParameterIndices() {
		final var domainParameterXref = getDomainParameterXref();
		final var jpaCriteriaParamResolutions =
				domainParameterXref.getParameterResolutions().getJpaCriteriaParamResolutions();
		if ( jpaCriteriaParamResolutions.isEmpty() ) {
			return null;
		}
		int maxId = 0;
		for ( var criteriaWrapper : jpaCriteriaParamResolutions.values() ) {
			maxId = Math.max( maxId, criteriaWrapper.getCriteriaParameterId() );
		}
		final var unnamedParameterIndices = new int[maxId + 1];
		for ( var entry : jpaCriteriaParamResolutions.entrySet() ) {
			unnamedParameterIndices[entry.getValue().getCriteriaParameterId()] = entry.getValue().getUnnamedParameterId();
		}
		return unnamedParameterIndices;
	}

	@Override
	public SelectionQuery<R> setPage(Page page) {
		setMaxResults( page.getMaxResults() );
		setFirstResult( page.getFirstResult() );
		return this;
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> keyedPage) {
		if ( keyedPage == null ) {
			throw new IllegalArgumentException( "KeyedPage was null" );
		}
		final var results =
				new SqmSelectionQueryImpl<KeyedResult<R>>( this, keyedPage )
						.getResultList();
		final int pageSize = keyedPage.getPage().getSize();
		return new KeyedResultList<>(
				collectResults( results, pageSize, keyedPage.getKeyInterpretation() ),
				collectKeys( results, pageSize ),
				keyedPage,
				nextPage( keyedPage, results ),
				previousPage( keyedPage, results )
		);
	}

	private static <R> KeyedPage<R> nextPage(KeyedPage<R> keyedPage, List<KeyedResult<R>> results) {
		if ( keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE ) {
			// the results come in reverse order
			return !results.isEmpty()
					? keyedPage.nextPage( results.get(0).getKey() )
					: null;
		}
		else {
			final int pageSize = keyedPage.getPage().getSize();
			return results.size() == pageSize + 1
					? keyedPage.nextPage( results.get(pageSize - 1).getKey() )
					: null;
		}
	}

	private static <R> KeyedPage<R> previousPage(KeyedPage<R> keyedPage, List<KeyedResult<R>> results) {
		if ( keyedPage.getKeyInterpretation() == KEY_OF_FIRST_ON_NEXT_PAGE ) {
			// the results come in reverse order
			final int pageSize = keyedPage.getPage().getSize();
			return results.size() == pageSize + 1
					? keyedPage.previousPage( results.get(pageSize - 1).getKey() )
					: null;
		}
		else {
			return !results.isEmpty()
					? keyedPage.previousPage( results.get(0).getKey() )
					: null;
		}
	}

	public abstract Class<R> getExpectedResultType();

	protected SelectQueryPlan<R> buildSelectQueryPlan() {
		final var statement = (SqmSelectStatement<R>) getSqmStatement();
		final var concreteSqmStatements = QuerySplitter.split( statement );
		return concreteSqmStatements.length > 1
				? buildAggregatedQueryPlan( concreteSqmStatements )
				: buildConcreteQueryPlan( concreteSqmStatements[0] );
	}

	private SelectQueryPlan<R> buildAggregatedQueryPlan(SqmSelectStatement<R>[] concreteSqmStatements) {
		@SuppressWarnings("unchecked")
		final SelectQueryPlan<R>[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];
		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level
		for ( int i = 0, length = concreteSqmStatements.length; i < length; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteQueryPlan( concreteSqmStatements[i] );
		}
		return new AggregatedSelectQueryPlanImpl<>( aggregatedQueryPlans );
	}

	protected SelectQueryPlan<R> buildConcreteQueryPlan(SqmSelectStatement<R> concreteSqmStatement) {
		return buildConcreteQueryPlan(
				concreteSqmStatement,
				getExpectedResultType(),
				getTupleMetadata(),
				getQueryOptions()
		);
	}

	protected <T> ConcreteSqmSelectQueryPlan<T> buildConcreteQueryPlan(
			SqmSelectStatement<T> concreteSqmStatement,
			Class<T> expectedResultType,
			TupleMetadata tupleMetadata,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				getQueryString(),
				getDomainParameterXref(),
				expectedResultType,
				tupleMetadata,
				queryOptions
		);
	}

	protected void applySqmOptions(NamedSqmQueryMemento<?> memento) {
		applyOptions( memento );

		if ( memento.getFirstResult() != null ) {
			setFirstResult( memento.getFirstResult() );
		}

		if ( memento.getMaxResults() != null ) {
			setMaxResults( memento.getMaxResults() );
		}

		if ( memento.getParameterTypes() != null ) {
			final var basicTypeRegistry = getTypeConfiguration().getBasicTypeRegistry();
			final var parameterMetadata = getParameterMetadata();
			memento.getParameterTypes().forEach( (key, value) ->
					parameterMetadata.getQueryParameter( key )
							.applyAnticipatedType( basicTypeRegistry.getRegisteredType( value ) ) );
		}
	}

	protected TupleMetadata buildTupleMetadata(SqmStatement<?> statement, Class<R> resultType) {
		if ( statement instanceof SqmSelectStatement<?> select ) {
			final var selections =
					select.getQueryPart().getFirstQuerySpec().getSelectClause().getSelections();
			return isTupleMetadataRequired( resultType, selections.get(0) )
					? getTupleMetadata( selections )
					: null;
		}
		else {
			return null;
		}
	}

	private static <R> boolean isTupleMetadataRequired(Class<R> resultType, SqmSelection<?> selection) {
		return isHqlTuple( selection )
			|| !isInstantiableWithoutMetadata( resultType )
				&& !isSelectionAssignableToResultType( selection, resultType );
	}

	private static boolean isInstantiableWithoutMetadata(Class<?> resultType) {
		return resultType == null
			|| resultType.isArray()
			|| Object.class == resultType
			|| List.class == resultType;
	}

	private TupleMetadata getTupleMetadata(List<SqmSelection<?>> selections) {
		final var tupleTransformer = getQueryOptions().getTupleTransformer();
		if ( tupleTransformer == null ) {
			return new TupleMetadata( buildTupleElementArray( selections ), buildTupleAliasArray( selections ) );
		}
		else {
			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer: "
							+ tupleTransformer
			);
		}
	}

	private static TupleElement<?>[] buildTupleElementArray(List<SqmSelection<?>> selections) {
		if ( selections.size() == 1 ) {
			final var selectableNode = selections.get( 0 ).getSelectableNode();
			if ( selectableNode instanceof CompoundSelection<?> ) {
				final var selectionItems = selectableNode.getSelectionItems();
				final var elements = new TupleElement<?>[ selectionItems.size() ];
				for ( int i = 0; i < selectionItems.size(); i++ ) {
					elements[i] = selectionItems.get( i );
				}
				return elements;
			}
			else {
				return new TupleElement<?>[] { selectableNode };
			}
		}
		else {
			final var elements = new TupleElement<?>[ selections.size() ];
			for ( int i = 0; i < selections.size(); i++ ) {
				elements[i] = selections.get( i ).getSelectableNode();
			}
			return elements;
		}
	}

	private static String[] buildTupleAliasArray(List<SqmSelection<?>> selections) {
		if ( selections.size() == 1 ) {
			final var selectableNode = selections.get(0).getSelectableNode();
			if ( selectableNode instanceof CompoundSelection<?> ) {
				final var selectionItems = selectableNode.getSelectionItems();
				final String[] elements = new String[ selectionItems.size() ];
				for ( int i = 0; i < selectionItems.size(); i++ ) {
					elements[i] = selectionItems.get( i ).getAlias();
				}
				return elements;
			}
			else {
				return new String[] { selectableNode.getAlias() };
			}
		}
		else {
			final String[] elements = new String[ selections.size() ];
			for ( int i = 0; i < selections.size(); i++ ) {
				elements[i] = selections.get( i ).getAlias();
			}
			return elements;
		}
	}

	protected static <T> HqlInterpretation<T> interpretation(
			NamedSqmQueryMemento<?> memento,
			Class<T> expectedResultType,
			SharedSessionContractImplementor session) {
		final var queryEngine = session.getFactory().getQueryEngine();
		return queryEngine.getInterpretationCache()
				.resolveHqlInterpretation( memento.getHqlString(), expectedResultType,
						queryEngine.getHqlTranslator() );
	}
}
