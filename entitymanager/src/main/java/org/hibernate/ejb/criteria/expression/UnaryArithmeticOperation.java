package org.hibernate.ejb.criteria.expression;

import javax.persistence.criteria.Expression;
import org.hibernate.ejb.criteria.ParameterContainer;
import org.hibernate.ejb.criteria.ParameterRegistry;
import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * Models arithmetc operations with two operands.
 *
 * @author Steve Ebersole
 */
public class UnaryArithmeticOperation<T> 
		extends ExpressionImpl<T>
		implements UnaryOperatorExpression<T> {

	public static enum Operation {
		UNARY_PLUS, UNARY_MINUS
	}

	private final Operation operation;
	private final Expression<T> operand;

	public UnaryArithmeticOperation(
			QueryBuilderImpl queryBuilder,
			Operation operation,
			Expression<T> operand) {
		super( queryBuilder, operand.getJavaType() );
		this.operation = operation;
		this.operand = operand;
	}

	public Expression<T> getOperand() {
		return operand;
	}

	public Operation getOperation() {
		return operation;
	}

	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getOperand(), registry );
	}

}
