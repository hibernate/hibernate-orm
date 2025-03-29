/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * @author Steve Ebersole
 */
public class UnaryOperation implements Expression, DomainResultProducer {

	private final UnaryArithmeticOperator operator;

	private final Expression operand;
	private final BasicValuedMapping type;

	public UnaryOperation(UnaryArithmeticOperator operator, Expression operand, BasicValuedMapping type) {
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
	public MappingModelExpressible getExpressionType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitUnaryOperationExpression( this );
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = creationState.getSqlAstCreationState().getSqlExpressionResolver().resolveSqlSelection(
				this,
				type.getJdbcMapping().getJdbcJavaType(),
				null,
				creationState.getSqlAstCreationState().getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);

		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				type.getJdbcMapping()
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		sqlExpressionResolver.resolveSqlSelection(
				this,
				type.getJdbcMapping().getJdbcJavaType(),
				null,
				sqlAstCreationState.getCreationContext().getMappingMetamodel().getTypeConfiguration()
		);
	}

}
