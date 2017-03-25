/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Models an ANSI SQL <tt>NULLIF</tt> expression.  <tt>NULLIF</tt> is a specialized <tt>CASE</tt> statement.
 *
 * @author Steve Ebersole
 */
public class NullifExpression<T> extends ExpressionImpl<T> implements Serializable {
	private final Expression<? extends T> primaryExpression;
	private final Expression<?> secondaryExpression;

	public NullifExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Expression<? extends T> primaryExpression,
			Expression<?> secondaryExpression) {
		super( criteriaBuilder, (Class<T>)determineType(javaType, primaryExpression) );
		this.primaryExpression = primaryExpression;
		this.secondaryExpression = secondaryExpression;
	}

	public NullifExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<T> javaType,
			Expression<? extends T> primaryExpression,
			Object secondaryExpression) {
		super( criteriaBuilder, (Class<T>)determineType(javaType, primaryExpression) );
		this.primaryExpression = primaryExpression;
		this.secondaryExpression = new LiteralExpression( criteriaBuilder, secondaryExpression );
	}

	private static Class determineType(Class javaType, Expression primaryExpression) {
		return javaType != null ? javaType : primaryExpression.getJavaType();
	}

	public Expression<? extends T> getPrimaryExpression() {
		return primaryExpression;
	}

	public Expression<?> getSecondaryExpression() {
		return secondaryExpression;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getPrimaryExpression(), registry );
		Helper.possibleParameter( getSecondaryExpression(), registry );
	}

	public String render(RenderingContext renderingContext) {
		return "nullif("
				+ ( (Renderable) getPrimaryExpression() ).render( renderingContext )
				+ ','
				+ ( (Renderable) getSecondaryExpression() ).render( renderingContext )
				+ ")";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
