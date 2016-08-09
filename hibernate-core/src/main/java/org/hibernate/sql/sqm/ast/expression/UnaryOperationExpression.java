/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class UnaryOperationExpression extends SelfReadingExpressionSupport {
	public enum Operation {
		PLUS,
		MINUS
	}

	private final Operation operation;
	private final Expression operand;
	private final BasicType type;

	public UnaryOperationExpression(Operation operation, Expression operand, BasicType type) {
		this.operation = operation;
		this.operand = operand;
		this.type = type;
	}

	@Override
	public BasicType getType() {
		return type;
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitUnaryOperationExpression( this );
	}

	public Expression getOperand() {
		return operand;
	}

	public Operation getOperation() {
		return operation;
	}
}
