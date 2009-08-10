/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria.predicate;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Defines a {@link Predicate} used to wrap an {@link Expression Expression&lt;Boolean&gt;}.
 * 
 * @author Steve Ebersole
 */
public class BooleanExpressionPredicate extends AbstractSimplePredicate {
	private final Expression<Boolean> expression;

	public BooleanExpressionPredicate(QueryBuilderImpl queryBuilder, Expression<Boolean> expression) {
		super( queryBuilder );
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

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter(expression, registry);
	}
}
