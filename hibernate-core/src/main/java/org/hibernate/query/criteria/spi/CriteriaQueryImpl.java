/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Selection;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.JpaTuple;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSubQuery;

/**
 * @author Steve Ebersole
 */
public class CriteriaQueryImpl<T> extends AbstractQuerySpecification<T> implements RootQuery<T> {
	public CriteriaQueryImpl(Class<T> resultType, CriteriaNodeBuilder criteriaBuilder) {
		super( resultType, criteriaBuilder );
	}

	@Override
	public RootQuery<T> distinct(boolean distinct) {
		return (CriteriaQueryImpl<T>) super.distinct( distinct );
	}

	@Override
	@SuppressWarnings("unchecked")
	public JpaCriteriaQuery<T> select(Selection<? extends T> selection) {
		super.setSelection( (JpaSelection) selection );
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaCriteriaQuery<T> multiselect(Selection<?>... selections) {
		applyMultiSelect( (List) Arrays.asList( selections ) );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	private void applyMultiSelect(List<? extends SelectionImplementor<?>> selections) {
		final Selection<? extends T> selection;

		if ( JpaTuple.class.isAssignableFrom( getResultType() ) ) {
			selection = (Selection) nodeBuilder().tuple( selections );
		}
		else if ( getResultType().isArray() ) {
			selection = nodeBuilder().array( (List) selections );
		}
		else if ( Object.class.equals( getResultType() ) ) {
			switch ( selections.size() ) {
				case 0: {
					throw new IllegalArgumentException(
							"empty selections passed to criteria query typed as Object"
					);
				}
				case 1: {
					selection = ( Selection<? extends T> ) selections.get( 0 );
					break;
				}
				default: {
					selection = ( Selection<? extends T> ) nodeBuilder().array( selections );
				}
			}
		}
		else {
			selection = nodeBuilder().construct( getResultType(), selections );
		}

		select( selection );

	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaCriteriaQuery<T> multiselect(List<Selection<?>> selections) {
		applyMultiSelect( (List) selections );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> where(Expression<Boolean> booleanExpression) {
		setWhere( nodeBuilder().wrap( booleanExpression ) );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> where(Predicate... predicates) {
		setWhere( nodeBuilder().wrap( predicates ) );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> groupBy(Expression<?>... expressions) {
		return (JpaCriteriaQuery<T>) super.groupBy( expressions );
	}

	@Override
	public JpaCriteriaQuery<T> groupBy(List<Expression<?>> grouping) {
		return (JpaCriteriaQuery<T>) super.groupBy( grouping );
	}

	@Override
	public JpaCriteriaQuery<T> having(Expression<Boolean> booleanExpression) {
		setGroupRestriction( nodeBuilder().wrap( booleanExpression ) );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> having(Predicate... predicates) {
		setGroupRestriction( nodeBuilder().wrap( predicates ) );
		return this;
	}

	@Override
	public JpaCriteriaQuery<T> orderBy(Order... o) {
		return orderBy( Arrays.asList( o ) );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaCriteriaQuery<T> orderBy(List<Order> orderList) {
		setSortSpecifications( (List) orderList );
		return this;
	}

	@Override
	public <U> JpaSubQuery<U> subquery(Class<U> type) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<ParameterExpression<?>> getParameters() {
		return (Set) ParameterCollector.collectParameters( this );
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitRootQuery( this );
	}
}
