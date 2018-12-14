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
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Selection;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.JpaTuple;

/**
 * @author Steve Ebersole
 */
public class CriteriaQueryImpl<T> extends AbstractSelectCriteria<T,RootQuery<T>> implements RootQuery<T> {
	public CriteriaQueryImpl(Class<T> resultType, CriteriaNodeBuilder criteriaBuilder) {
		super( resultType, criteriaBuilder );
	}

	@Override
	@SuppressWarnings("unchecked")
	public RootQuery<T> select(Selection<? extends T> selection) {
		getQueryStructure().setSelection( (SelectionImplementor) selection );
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public RootQuery<T> multiselect(Selection<?>... selections) {
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
	public RootQuery<T> multiselect(List<Selection<?>> selections) {
		applyMultiSelect( (List) selections );
		return this;
	}

	@Override
	public RootQuery<T> orderBy(Order... o) {
		return orderBy( Arrays.asList( o ) );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public RootQuery<T> orderBy(List<Order> orderList) {
		getQueryStructure().setSortSpecifications( (List) orderList );
		return this;
	}

	@Override
	public <U> SubQuery<U> subquery(Class<U> type) {
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
