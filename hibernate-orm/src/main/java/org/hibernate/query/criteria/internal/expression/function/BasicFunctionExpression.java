/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression.function;

import java.io.Serializable;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.ExpressionImpl;

/**
 * Models the basic concept of a SQL function.
 *
 * @author Steve Ebersole
 */
public class BasicFunctionExpression<X>
		extends ExpressionImpl<X>
		implements FunctionExpression<X>, Serializable {

	private final String functionName;

	public BasicFunctionExpression(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			String functionName) {
		super( criteriaBuilder, javaType );
		this.functionName = functionName;
	}

	protected  static int properSize(int number) {
		return number + (int)( number*.75 ) + 1;
	}

	public String getFunctionName() {
		return functionName;
	}

	public boolean isAggregation() {
		return false;
	}

	public void registerParameters(ParameterRegistry registry) {
		// nothing to do here...
	}

	public String render(RenderingContext renderingContext) {
		return getFunctionName() + "()";
	}

	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
