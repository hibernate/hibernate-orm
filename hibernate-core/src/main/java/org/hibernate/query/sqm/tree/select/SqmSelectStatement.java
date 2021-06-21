/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Selection;

import org.hibernate.query.FetchClauseType;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.ParameterCollector;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.jpa.CriteriaParameterCollectQueryWalker;

/**
 * @author Steve Ebersole
 */
public class SqmSelectStatement<T> extends AbstractSqmSelectQuery<T> implements JpaCriteriaQuery<T>, SqmStatement<T>,
		ParameterCollector {
	private final SqmQuerySource querySource;

	private Set<SqmParameter<?>> parameters;

	public SqmSelectStatement(NodeBuilder nodeBuilder) {
		this( SqmQuerySource.HQL, nodeBuilder );
	}

	public SqmSelectStatement(SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( (Class<T>) null, nodeBuilder );
		this.querySource = querySource;
	}

	public SqmSelectStatement(Class<T> resultJavaType, SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( resultJavaType, nodeBuilder );
		this.querySource = querySource;
	}

	public SqmSelectStatement(
			SqmQueryPart<T> queryPart,
			Class<T> resultType,
			SqmQuerySource querySource,
			NodeBuilder builder) {
		super( queryPart, resultType, builder );
		this.querySource = querySource;
	}

	/**
	 * @implNote This form is used from Hibernate's JPA criteria handling.
	 */
	public SqmSelectStatement(Class<T> resultJavaType, NodeBuilder nodeBuilder) {
		super( resultJavaType, nodeBuilder );

		this.querySource = SqmQuerySource.CRITERIA;

		getQuerySpec().setSelectClause( new SqmSelectClause( false, nodeBuilder ) );
		getQuerySpec().setFromClause( new SqmFromClause() );
	}

	@Override
	public SqmQuerySource getQuerySource() {
		return querySource;
	}

	@Override
	public SqmQuerySpec<T> getQuerySpec() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			final SqmQueryPart<T> queryPart = getQueryPart();
			if ( queryPart instanceof SqmQuerySpec<?> ) {
				return (SqmQuerySpec<T>) queryPart;
			}
			throw new IllegalStateException(
					"Query group can't be treated as query spec. Use JpaSelectCriteria#getQueryPart to access query group details"
			);
		}
		else {
			return super.getQuerySpec();
		}
	}

	public boolean containsCollectionFetches() {
		return containsCollectionFetches( getQueryPart() );
	}

	private boolean containsCollectionFetches(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			return ( (SqmQuerySpec<?>) queryPart ).containsCollectionFetches();
		}
		else {
			// We only have to check the first one
			final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
			return containsCollectionFetches( queryGroup.getQueryParts().get( 0 ) );
		}
	}

	public boolean usesDistinct() {
		return usesDistinct( getQueryPart() );
	}

	private boolean usesDistinct(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			return ( (SqmQuerySpec<?>) queryPart ).getSelectClause().isDistinct();
		}
		else {
			// We only have to check the first one
			final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
			return usesDistinct( queryGroup.getQueryParts().get( 0 ) );
		}
	}

	@Override
	public Set<SqmParameter<?>> getSqmParameters() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			assert parameters == null : "SqmSelectStatement (as Criteria) should not have collected parameters";

			return CriteriaParameterCollectQueryWalker.collectParameters(
					this,
					sqmParameter -> {},
					nodeBuilder().getServiceRegistry()
			);
		}

		return parameters == null ? Collections.emptySet() : Collections.unmodifiableSet( parameters );
	}

	@Override
	public ParameterResolutions resolveParameters() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			final CriteriaParameterCollector parameterCollector = new CriteriaParameterCollector();

			CriteriaParameterCollectQueryWalker.collectParameters(
					this,
					parameterCollector::process,
					nodeBuilder().getServiceRegistry()
			);

			return parameterCollector.makeResolution();
		}
		else {
			return new ParameterResolutions() {
				@Override
				public Set<SqmParameter<?>> getSqmParameters() {
					return parameters == null ? Collections.emptySet() : Collections.unmodifiableSet( parameters );
				}

				@Override
				public Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> getJpaCriteriaParamResolutions() {
					return Collections.emptyMap();
				}
			};
		}
	}

	private static class CriteriaParameterCollector {
		private Set<SqmParameter<?>> sqmParameters;
		private Map<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;

		public void process(SqmParameter<?> parameter) {
			if ( sqmParameters == null ) {
				sqmParameters = new HashSet<>();
			}

			if ( parameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				if ( jpaCriteriaParamResolutions == null ) {
					jpaCriteriaParamResolutions = new IdentityHashMap<>();
				}

				final SqmJpaCriteriaParameterWrapper<?> wrapper = (SqmJpaCriteriaParameterWrapper<?>) parameter;
				final JpaCriteriaParameter<?> criteriaParameter = wrapper.getJpaCriteriaParameter();

				final List<SqmJpaCriteriaParameterWrapper<?>> sqmParametersForCriteriaParameter = jpaCriteriaParamResolutions.computeIfAbsent(
						criteriaParameter,
						jcp -> new ArrayList<>()
				);

				sqmParametersForCriteriaParameter.add( wrapper );
				sqmParameters.add( wrapper );
			}
			else if ( parameter instanceof JpaCriteriaParameter ) {
				throw new UnsupportedOperationException();
//				final JpaCriteriaParameter<?> criteriaParameter = (JpaCriteriaParameter<?>) parameter;
//
//				if ( jpaCriteriaParamResolutions == null ) {
//					jpaCriteriaParamResolutions = new IdentityHashMap<>();
//				}
//
//				final List<SqmJpaCriteriaParameterWrapper<?>> sqmParametersForCriteriaParameter = jpaCriteriaParamResolutions.computeIfAbsent(
//						criteriaParameter,
//						jcp -> new ArrayList<>()
//				);
//
//				final SqmJpaCriteriaParameterWrapper<?> wrapper = new SqmJpaCriteriaParameterWrapper(
//						criteriaParameter.getHibernateType(),
//						criteriaParameter,
//						criteriaParameter.nodeBuilder()
//				);
//
//				sqmParametersForCriteriaParameter.add( wrapper );
//				sqmParameters.add( wrapper );
			}
			else {
				sqmParameters.add( parameter );
			}
		}

		private ParameterResolutions makeResolution() {
			return new ParameterResolutionsImpl(
					sqmParameters == null ? Collections.emptySet() : sqmParameters,
					jpaCriteriaParamResolutions == null ? Collections.emptyMap() : jpaCriteriaParamResolutions
			);
		}
	}

	private static class ParameterResolutionsImpl implements ParameterResolutions {
		private final Set<SqmParameter<?>> sqmParameters;
		private final Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;

		public ParameterResolutionsImpl(
				Set<SqmParameter<?>> sqmParameters,
				Map<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions) {
			this.sqmParameters = sqmParameters;

			if ( jpaCriteriaParamResolutions == null || jpaCriteriaParamResolutions.isEmpty() ) {
				this.jpaCriteriaParamResolutions = Collections.emptyMap();
			}
			else {
				this.jpaCriteriaParamResolutions = new IdentityHashMap<>( CollectionHelper.determineProperSizing( jpaCriteriaParamResolutions ) );
				for ( Map.Entry<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> entry : jpaCriteriaParamResolutions.entrySet() ) {
					final Iterator<SqmJpaCriteriaParameterWrapper<?>> itr = entry.getValue().iterator();
					this.jpaCriteriaParamResolutions.put(
							entry.getKey(),
							() -> {
								if ( itr.hasNext() ) {
									return itr.next();
								}
								throw new IllegalStateException( "SqmJpaCriteriaParameterWrapper references for JpaCriteriaParameter [" + entry.getKey() + "] already exhausted" );
							}
					);

				}
			}
		}

		@Override
		public Set<SqmParameter<?>> getSqmParameters() {
			return sqmParameters;
		}

		@Override
		public Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> getJpaCriteriaParamResolutions() {
			return jpaCriteriaParamResolutions;
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitSelectStatement( this );
	}

	@Override
	public void addParameter(SqmParameter parameter) {
		if ( parameters == null ) {
			parameters = new HashSet<>();
		}

		parameters.add( parameter );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmSelectStatement<T> distinct(boolean distinct) {
		return (SqmSelectStatement<T>) super.distinct( distinct );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<ParameterExpression<?>> getParameters() {
		// At this level, the number of parameters may still be growing as
		// nodes are added to the Criteria - so we re-calculate this every
		// time.
		//
		// for a "finalized" set of parameters, use `#resolveParameters` instead
		assert querySource == SqmQuerySource.CRITERIA;
		return (Set) getSqmParameters();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSelectStatement<T> select(Selection<? extends T> selection) {
		getQuerySpec().setSelection( (JpaSelection<T>) selection );
		if ( getResultType() == null ) {
			setResultType( (Class<T>) selection.getJavaType() );
		}
		return this;
	}

	@Override
	public SqmSelectStatement<T> multiselect(Selection<?>... selections) {
		for ( Selection<?> selection : selections ) {
			getQuerySpec().getSelectClause().add( (SqmExpression<?>) selection, selection.getAlias() );
		}
		if ( getResultType() == null ) {
			setResultType( (Class<T>) Object[].class );
		}
		return this;
	}

	@Override
	public SqmSelectStatement<T> multiselect(List<Selection<?>> selectionList) {
		for ( Selection<?> selection : selectionList ) {
			getQuerySpec().getSelectClause().add( (SqmExpression<?>) selection, selection.getAlias() );
		}
		if ( getResultType() == null ) {
			setResultType( (Class<T>) Object[].class );
		}
		return this;
	}

	@Override
	public SqmSelectStatement<T> orderBy(Order... orders) {
		if ( getQueryPart().getOrderByClause() == null ) {
			getQueryPart().setOrderByClause( new SqmOrderByClause() );
		}
		for ( Order order : orders ) {
			getQueryPart().getOrderByClause().addSortSpecification( (SqmSortSpecification) order );
		}
		return this;
	}

	@Override
	public SqmSelectStatement<T> orderBy(List<Order> orders) {
		if ( getQueryPart().getOrderByClause() == null ) {
			getQueryPart().setOrderByClause( new SqmOrderByClause() );
		}
		for ( Order order : orders ) {
			getQueryPart().getOrderByClause().addSortSpecification( (SqmSortSpecification) order );
		}
		return this;
	}

	@Override
	public <U> SqmSubQuery<U> subquery(Class<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}

	@Override
	public SqmSelectStatement<T> where(Expression<Boolean> restriction) {
		return (SqmSelectStatement<T>) super.where( restriction );
	}

	@Override
	public SqmSelectStatement<T> where(Predicate... restrictions) {
		return (SqmSelectStatement<T>) super.where( restrictions );
	}

	@Override
	public SqmSelectStatement<T> groupBy(Expression<?>... expressions) {
		return (SqmSelectStatement<T>) super.groupBy( expressions );
	}

	@Override
	public SqmSelectStatement<T> groupBy(List<Expression<?>> grouping) {
		return (SqmSelectStatement<T>) super.groupBy( grouping );
	}

	@Override
	public SqmSelectStatement<T> having(Expression<Boolean> booleanExpression) {
		return (SqmSelectStatement<T>) super.having( booleanExpression );
	}

	@Override
	public SqmSelectStatement<T> having(Predicate... predicates) {
		return (SqmSelectStatement<T>) super.having( predicates );
	}

	@Override
	public JpaExpression<Number> getOffset() {
		return (JpaExpression<Number>) getQueryPart().getOffset();
	}

	@Override
	public JpaCriteriaQuery<T> offset(JpaExpression<? extends Number> offset) {
		getQueryPart().setOffset( offset );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> offset(Number offset) {
		getQueryPart().setOffset( nodeBuilder().value( offset ) );
		return this;
	}

	@Override
	public JpaExpression<Number> getFetch() {
		return (JpaExpression<Number>) getQueryPart().getFetch();
	}

	@Override
	public JpaCriteriaQuery<T> fetch(JpaExpression<? extends Number> fetch) {
		getQueryPart().setFetch( fetch );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		getQueryPart().setFetch( fetch, fetchClauseType );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> fetch(Number fetch) {
		getQueryPart().setFetch( nodeBuilder().value( fetch ) );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType) {
		getQueryPart().setFetch( nodeBuilder().value( fetch ), fetchClauseType );
		return this;
	}

	@Override
	public FetchClauseType getFetchClauseType() {
		return getQueryPart().getFetchClauseType();
	}
}
