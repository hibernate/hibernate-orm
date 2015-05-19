/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.predicate;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * Defines a {@link javax.persistence.criteria.Predicate} used to wrap an {@link Expression Expression&lt;Boolean&gt;}.
 * 
 * @author Steve Ebersole
 */
public class BooleanExpressionPredicate
		extends AbstractSimplePredicate
		implements Serializable {
	private final Expression<Boolean> expression;

	public BooleanExpressionPredicate(CriteriaBuilderImpl criteriaBuilder, Expression<Boolean> expression) {
		super( criteriaBuilder );
		this.expression = expression;
	}

	/**
	 * Get the boolean expression defining the predicate.
	 * 
	 * @return The underlying boolean expression.
	 */
	public Expression<Boolean> getExpression() {
		return expression;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter(expression, registry);
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		return ( (Renderable) getExpression() ).render( renderingContext );
	}
}
