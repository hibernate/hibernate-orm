/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.JpaCompoundPredicate;
import org.hibernate.query.criteria.JpaPredicateImplementor;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterContainer;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.expression.AbstractExpression;

/**
 * @author Steve Ebersole
 */
public class NegatedPredicateWrapper extends AbstractExpression<Boolean> implements JpaPredicateImplementor, Serializable {
	private final JpaPredicateImplementor predicate;
	private final BooleanOperator negatedOperator;
	private final List<Expression<Boolean>> negatedExpressions;

	@SuppressWarnings("unchecked")
	public NegatedPredicateWrapper(JpaPredicateImplementor predicate) {
		super( predicate.criteriaBuilder(), Boolean.class );
		this.predicate = predicate;
		this.negatedOperator = predicate.isJunction()
				? JpaCompoundPredicate.reverseOperator( predicate.getOperator() )
				: predicate.getOperator();
		this.negatedExpressions = negateCompoundExpressions( predicate.getExpressions(), predicate.criteriaBuilder() );
	}

	private static List<Expression<Boolean>> negateCompoundExpressions(
			List<Expression<Boolean>> expressions,
			CriteriaBuilderImpl criteriaBuilder) {
		if ( expressions == null || expressions.isEmpty() ) {
			return Collections.emptyList();
		}

		final List<Expression<Boolean>> negatedExpressions = new ArrayList<Expression<Boolean>>();
		for ( Expression<Boolean> expression : expressions ) {
			if ( Predicate.class.isInstance( expression ) ) {
				negatedExpressions.add( ( (Predicate) expression ).not() );
			}
			else {
				negatedExpressions.add( criteriaBuilder.not( expression ) );
			}
		}
		return negatedExpressions;
	}

	@Override
	public BooleanOperator getOperator() {
		return negatedOperator;
	}

	@Override
	public boolean isJunction() {
		return predicate.isJunction();
	}

	@Override
	public boolean isNegated() {
		return ! predicate.isNegated();
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return negatedExpressions;
	}

	@Override
	public Predicate not() {
		return new NegatedPredicateWrapper( this );
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		if ( ParameterContainer.class.isInstance( predicate ) ) {
			( (ParameterContainer) predicate ).registerParameters( registry );
		}
	}
}
