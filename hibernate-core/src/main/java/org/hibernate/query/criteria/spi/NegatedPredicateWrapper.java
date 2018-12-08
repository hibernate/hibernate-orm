/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class NegatedPredicateWrapper extends AbstractPredicate {
	private final PredicateImplementor wrappedPredicate;
	private final Predicate.BooleanOperator negatedOperator;
	private final List<ExpressionImplementor<Boolean>> negatedExpressions;

	public NegatedPredicateWrapper(PredicateImplementor wrappedPredicate) {
		super( wrappedPredicate.nodeBuilder() );

		this.wrappedPredicate = wrappedPredicate;
		this.negatedOperator = wrappedPredicate instanceof Junction
				? PredicateImplementor.reverseOperator( wrappedPredicate.getOperator() )
				: wrappedPredicate.getOperator();
		this.negatedExpressions = negateCompoundExpressions( wrappedPredicate.getExpressions(), wrappedPredicate.nodeBuilder() );
	}

	private static List<ExpressionImplementor<Boolean>> negateCompoundExpressions(
			List<Expression<Boolean>> expressions,
			CriteriaNodeBuilder criteriaBuilder) {
		if ( expressions == null || expressions.isEmpty() ) {
			return Collections.emptyList();
		}

		final List<ExpressionImplementor<Boolean>> negatedExpressions = new ArrayList<>();
		for ( Expression<Boolean> expression : expressions ) {
			if ( expression instanceof PredicateImplementor ) {
				negatedExpressions.add( ( (PredicateImplementor) expression ).not() );
			}
			else {
				negatedExpressions.add( criteriaBuilder.not( expression ) );
			}
		}

		return negatedExpressions;
	}

	@Override
	public Predicate.BooleanOperator getOperator() {
		return negatedOperator;
	}

	@Override
	public boolean isNegated() {
		return ! wrappedPredicate.isNegated();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Expression<Boolean>> getExpressions() {
		return (List) negatedExpressions;
	}

	@Override
	public PredicateImplementor not() {
		return new NegatedPredicateWrapper( this );
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitNegatedPredicate( this );
	}
}
