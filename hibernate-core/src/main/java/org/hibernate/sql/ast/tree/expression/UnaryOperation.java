/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.expression;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.mapping.spi.ValueMapping;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.sql.ast.ValueMappingExpressable;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * @author Steve Ebersole
 */
public class UnaryOperation implements Expression, ValueMappingExpressable, DomainResultProducer {

	private final UnaryArithmeticOperator operator;

	private final Expression operand;
	private final ValueMappingExpressable type;

	public UnaryOperation(UnaryArithmeticOperator operator, Expression operand, ValueMappingExpressable type) {
		this.operator = operator;
		this.operand = operand;
		this.type = type;
	}

	public UnaryArithmeticOperator getOperator() {
		return operator;
	}

	public Expression getOperand() {
		return operand;
	}

	@Override
	public ValueMappingExpressable getExpressionType() {
		return type;
	}

	@Override
	public ValueMapping getExpressableValueMapping() {
		// calling type#getExpressableValueMapping could be recursive
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitUnaryOperationExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

}
