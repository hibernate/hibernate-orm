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
 * Models unary arithmetic operation (unary plus and unary minus).
 *
 * @author Steve Ebersole
 */
public class UnaryArithmeticOperation<T> 
		extends ExpressionImpl<T>
		implements UnaryOperatorExpression<T>, Serializable {

	public static enum Operation {
		UNARY_PLUS, UNARY_MINUS
	}

	private final Operation operation;
	private final Expression<T> operand;

	@SuppressWarnings({ "unchecked" })
	public UnaryArithmeticOperation(
			CriteriaBuilderImpl criteriaBuilder,
			Operation operation,
			Expression<T> operand) {
		super( criteriaBuilder, (Class)operand.getJavaType() );
		this.operation = operation;
		this.operand = operand;
	}

	public Operation getOperation() {
		return operation;
	}

	@Override
	public Expression<T> getOperand() {
		return operand;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getOperand(), registry );
	}

	@Override
	public String render(RenderingContext renderingContext) {
		return ( getOperation() == Operation.UNARY_MINUS ? '-' : '+' )
				+ ( (Renderable) getOperand() ).render( renderingContext );
	}

	@Override
	public String renderProjection(RenderingContext renderingContext) {
		return render( renderingContext );
	}
}
