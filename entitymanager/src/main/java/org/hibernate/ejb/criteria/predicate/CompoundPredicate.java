/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Expression;

import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.Renderable;

/**
 * A compound {@link Predicate predicate} is a grouping of other {@link Predicate predicates} in order to convert
 * either a conjunction (logical AND) or a disjunction (logical OR).
 *
 * @author Steve Ebersole
 */
public class CompoundPredicate
		extends AbstractPredicateImpl
		implements Serializable {
	private final BooleanOperator operator;
	private final List<Expression<Boolean>> expressions = new ArrayList<Expression<Boolean>>();

	/**
	 * Constructs an empty conjunction or disjunction.
	 *
	 * @param criteriaBuilder The query builder from whcih this originates.
	 * @param operator Indicates whether this predicate will funtion
	 * as a conjunction or disjuntion.
	 */
	public CompoundPredicate(CriteriaBuilderImpl criteriaBuilder, BooleanOperator operator) {
		super( criteriaBuilder );
		this.operator = operator;
	}

	/**
	 * Constructs a conjunction or disjunction over the given expressions.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will funtion
	 * as a conjunction or disjuntion.
	 * @param expressions The expressions to be grouped.
	 */
	public CompoundPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			BooleanOperator operator,
			Expression<Boolean>... expressions) {
		this( criteriaBuilder,  operator );
		applyExpressions( expressions );
	}

	/**
	 * Constructs a conjunction or disjunction over the given expressions.
	 *
	 * @param criteriaBuilder The query builder from whcih this originates.
	 * @param operator Indicates whether this predicate will funtion
	 * as a conjunction or disjuntion.
	 * @param expressions The expressions to be grouped.
	 */
	public CompoundPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			BooleanOperator operator,
			List<Expression<Boolean>> expressions) {
		this( criteriaBuilder,  operator );
		applyExpressions( expressions );
	}

	private void applyExpressions(Expression<Boolean>... expressions) {
		applyExpressions( Arrays.asList( expressions ) );
	}

	private void applyExpressions(List<Expression<Boolean>> expressions) {
		this.expressions.clear();
		this.expressions.addAll( expressions );
	}

	public BooleanOperator getOperator() {
		return operator;
	}

	public List<Expression<Boolean>> getExpressions() {
		return expressions;
	}

	public void registerParameters(ParameterRegistry registry) {
		for ( Expression expression : getExpressions() ) {
			Helper.possibleParameter(expression, registry);
		}
	}

	public String render(CriteriaQueryCompiler.RenderingContext renderingContext) {
		if ( getExpressions().size() == 0 ) {
			boolean implicitTrue = getOperator() == BooleanOperator.AND;
			if ( isNegated() ) {
				implicitTrue = !implicitTrue;
			}
			return implicitTrue
					? "1=1" // true
					: "0=1"; // false
		}
		if ( getExpressions().size() == 1 ) {
			return ( (Renderable) getExpressions().get(0) ).render( renderingContext );
		}
		final StringBuilder buffer = new StringBuilder();
		String sep = "";
		for ( Expression expression : getExpressions() ) {
			buffer.append( sep )
					.append( "( " )
					.append( ( (Renderable) expression ).render( renderingContext ) )
					.append( " )" );
			sep = operatorTextWithSeparator();
		}
		return buffer.toString();
	}

	private String operatorTextWithSeparator() {
		return getOperator() == BooleanOperator.AND
				? " and "
				: " or ";
	}

	public String renderProjection(CriteriaQueryCompiler.RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
