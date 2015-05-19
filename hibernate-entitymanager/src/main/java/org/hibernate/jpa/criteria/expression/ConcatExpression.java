/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ParameterRegistry;
import org.hibernate.jpa.criteria.Renderable;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * A string concatenation.
 *
 * @author Steve Ebersole
 */
public class ConcatExpression extends ExpressionImpl<String> implements Serializable {
	private Expression<String> string1;
	private Expression<String> string2;

	public ConcatExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> expression1,
			Expression<String> expression2) {
		super( criteriaBuilder, String.class );
		this.string1 = expression1;
		this.string2 = expression2;
	}

	public ConcatExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> string1, 
			String string2) {
		this( criteriaBuilder, string1, wrap( criteriaBuilder, string2) );
	}

	private static Expression<String> wrap(CriteriaBuilderImpl criteriaBuilder, String string) {
		return new LiteralExpression<String>( criteriaBuilder, string );
	}

	public ConcatExpression(
			CriteriaBuilderImpl criteriaBuilder,
			String string1,
			Expression<String> string2) {
		this( criteriaBuilder, wrap( criteriaBuilder, string1), string2 );
	}

	public Expression<String> getString1() {
		return string1;
	}

	public Expression<String> getString2() {
		return string2;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getString1(), registry );
		Helper.possibleParameter( getString2(), registry );
	}

	public String render(RenderingContext renderingContext) {
		return ( (Renderable) getString1() ).render( renderingContext )
				+ " || "
				+ ( (Renderable) getString2() ).render( renderingContext );
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
