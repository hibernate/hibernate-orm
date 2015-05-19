/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.predicate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * A compound {@link Predicate predicate} is a grouping of other {@link Predicate predicates} in order to convert
 * either a conjunction (logical AND) or a disjunction (logical OR).
 *
 * @author Steve Ebersole
 */
public class CompoundPredicate
		extends AbstractPredicateImpl
		implements Serializable {
	private BooleanOperator operator;
	private final List<Expression<Boolean>> expressions = new ArrayList<Expression<Boolean>>();

	/**
	 * Constructs an empty conjunction or disjunction.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 */
	public CompoundPredicate(CriteriaBuilderImpl criteriaBuilder, BooleanOperator operator) {
		super( criteriaBuilder );
		this.operator = operator;
	}

	/**
	 * Constructs a conjunction or disjunction over the given expressions.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 * @param expressions The expressions to be grouped.
	 */
	public CompoundPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			BooleanOperator operator,
			Expression<Boolean>... expressions) {
		this( criteriaBuilder, operator );
		applyExpressions( expressions );
	}

	/**
	 * Constructs a conjunction or disjunction over the given expressions.
	 *
	 * @param criteriaBuilder The query builder from which this originates.
	 * @param operator Indicates whether this predicate will function
	 * as a conjunction or disjunction.
	 * @param expressions The expressions to be grouped.
	 */
	public CompoundPredicate(
			CriteriaBuilderImpl criteriaBuilder,
			BooleanOperator operator,
			List<Expression<Boolean>> expressions) {
		this( criteriaBuilder, operator );
		applyExpressions( expressions );
	}

	private void applyExpressions(Expression<Boolean>... expressions) {
		applyExpressions( Arrays.asList( expressions ) );
	}

	private void applyExpressions(List<Expression<Boolean>> expressions) {
		this.expressions.clear();
		this.expressions.addAll( expressions );
	}

	@Override
	public BooleanOperator getOperator() {
		return operator;
	}

	@Override
	public List<Expression<Boolean>> getExpressions() {
		return expressions;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		for ( Expression expression : getExpressions() ) {
			Helper.possibleParameter( expression, registry );
		}
	}

	@Override
	public String render(RenderingContext renderingContext) {
		return render( isNegated(), renderingContext );
	}

	@Override
	public boolean isJunction() {
		return true;
	}

	@Override
	public String render(boolean isNegated, RenderingContext renderingContext) {
		return render( this, renderingContext );
	}

	private String operatorTextWithSeparator() {
		return operatorTextWithSeparator( this.getOperator() );
	}

	@Override
	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}

	/**
	 * Create negation of compound predicate by using logic rules:
	 * 1. not (x || y) is (not x && not y)
	 * 2. not (x && y) is (not x || not y)
	 */
	@Override
	public Predicate not() {
		return new NegatedPredicateWrapper( this );
	}

	private void toggleOperator() {
		this.operator = reverseOperator( this.operator );
	}

	public static BooleanOperator reverseOperator(BooleanOperator operator) {
		return operator == BooleanOperator.AND
				? BooleanOperator.OR
				: BooleanOperator.AND;
	}

	public static String render(PredicateImplementor predicate, RenderingContext renderingContext) {
		if ( ! predicate.isJunction() ) {
			throw new IllegalStateException( "CompoundPredicate.render should only be used to render junctions" );
		}

		// for junctions, the negation is already cooked into the expressions and operator; we just need to render
		// them as is

		if ( predicate.getExpressions().isEmpty() ) {
			boolean implicitTrue = predicate.getOperator() == BooleanOperator.AND;
			// AND is always true for empty; OR is always false
			return implicitTrue ? "1=1" : "0=1";
		}

		// single valued junction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		if ( predicate.getExpressions().size() == 1 ) {
			return ( (Renderable) predicate.getExpressions().get( 0 ) ).render( renderingContext );
		}

		final StringBuilder buffer = new StringBuilder();
		String sep = "";
		for ( Expression expression : predicate.getExpressions() ) {
			buffer.append( sep )
					.append( "( " )
					.append( ( (Renderable) expression ).render( renderingContext ) )
					.append( " )" );
			sep = operatorTextWithSeparator( predicate.getOperator() );
		}
		return buffer.toString();
	}

	private static String operatorTextWithSeparator(BooleanOperator operator) {
		return operator == BooleanOperator.AND
				? " and "
				: " or ";
	}
}
