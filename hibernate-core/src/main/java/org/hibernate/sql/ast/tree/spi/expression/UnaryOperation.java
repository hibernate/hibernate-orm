/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class UnaryOperation implements Expression, SqlExpressable {
	public enum Operator {
		PLUS,
		MINUS
	}

	private final Operator operator;
	private final Expression operand;
	private final BasicValuedExpressableType type;

	public UnaryOperation(Operator operator, Expression operand, BasicValuedExpressableType type) {
		this.operator = operator;
		this.operand = operand;
		this.type = type;
	}

	public Operator getOperator() {
		return operator;
	}

	public Expression getOperand() {
		return operand;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return type;
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return new SqlSelectionImpl(
				getType().getBasicType().getSqlSelectionReader(),
				jdbcPosition
		);
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitUnaryOperationExpression( this );
	}
}
