/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Selection;

import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.jpa.ParameterCollector;

/**
 * @author Steve Ebersole
 */
public class SqmSelectStatement<T> extends AbstractSqmSelectQuery<T> implements JpaCriteriaQuery<T>, SqmStatement<T>, org.hibernate.query.sqm.tree.internal.ParameterCollector {
	private final SqmQuerySource querySource;

	private Set<SqmParameter<?>> parameters;

	public SqmSelectStatement(NodeBuilder nodeBuilder) {
		this( SqmQuerySource.HQL, nodeBuilder );
	}

	public SqmSelectStatement(SqmQuerySource querySource, NodeBuilder nodeBuilder) {
		super( (Class<T>) null, nodeBuilder );
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
	public Set<SqmParameter<?>> getSqmParameters() {
		if ( querySource == SqmQuerySource.CRITERIA ) {
			assert parameters == null : "SqmSelectStatement (as Criteria) should not have collected parameters";
			return ParameterCollector.collectParameters(
					this,
					nodeBuilder().getServiceRegistry()
			);
		}
		return parameters == null ? Collections.emptySet() : Collections.unmodifiableSet( parameters );
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
		// time
		assert querySource == SqmQuerySource.CRITERIA;
		return (Set) getSqmParameters();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmSelectStatement<T> select(Selection<? extends T> selection) {
		getQuerySpec().setSelection( (JpaSelection<T>) selection );
		setResultType( selection.getJavaType() );
		return this;
	}

	@Override
	public SqmSelectStatement<T> multiselect(Selection<?>... selections) {
		for ( Selection<?> selection : selections ) {
			getQuerySpec().getSelectClause().add( (SqmExpression) selection, selection.getAlias() );
		}
		setResultType( Object[].class );
		return this;
	}

	@Override
	public SqmSelectStatement<T> multiselect(List<Selection<?>> selectionList) {
		for ( Selection<?> selection : selectionList ) {
			getQuerySpec().getSelectClause().add( (SqmExpression) selection, selection.getAlias() );
		}
		setResultType( Object[].class );
		return this;
	}

	@Override
	public SqmSelectStatement<T> orderBy(Order... orders) {
		for ( Order order : orders ) {
			getQuerySpec().getOrderByClause().addSortSpecification( (SqmSortSpecification) order );
		}
		return this;
	}

	@Override
	public SqmSelectStatement<T> orderBy(List<Order> orders) {
		for ( Order order : orders ) {
			getQuerySpec().getOrderByClause().addSortSpecification( (SqmSortSpecification) order );
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
}
