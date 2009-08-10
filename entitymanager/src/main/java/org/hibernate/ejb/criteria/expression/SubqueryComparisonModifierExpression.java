package org.hibernate.ejb.criteria.expression;

import javax.persistence.criteria.Subquery;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Represents a {@link Modifier#ALL}, {@link Modifier#ANY}, {@link Modifier#SOME} modifier appplied to a subquery as
 * part of a comparison.
 *
 * @author Steve Ebersole
 */
public class SubqueryComparisonModifierExpression<Y> extends ExpressionImpl<Y> {
	public static enum Modifier { ALL, SOME, ANY };

	private final Subquery<Y> subquery;
	private final Modifier modifier;

	public SubqueryComparisonModifierExpression(
			QueryBuilderImpl queryBuilder,
			Class<Y> javaType,
			Subquery<Y> subquery,
			Modifier modifier) {
		super(queryBuilder, javaType);
		this.subquery = subquery;
		this.modifier = modifier;
	}

	public Modifier getModifier() {
		return modifier;
	}

	public Subquery<Y> getSubquery() {
		return subquery;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothign to do (the subquery should be handled directly, and the modified itself is not parameterized)
	}

}
