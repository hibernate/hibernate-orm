/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.function.CastFunction;

/**
 * Represents a <tt>NULL</tt>literal expression.
 *
 * @author Steve Ebersole
 */
public class NullLiteralExpression<T> extends ExpressionImpl<T> implements Serializable {
	public NullLiteralExpression(CriteriaBuilderImpl criteriaBuilder, Class<T> type) {
		super( criteriaBuilder, type );
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do
	}

	public String render(RenderingContext renderingContext) {
		return "null";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return CastFunction.CAST_NAME + "( 	null  as " + renderingContext.getCastType( getJavaType() ) + ')';
	}
}
