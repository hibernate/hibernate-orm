/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;

import java.util.List;

import static org.hibernate.query.UnaryArithmeticOperator.UNARY_MINUS;

/**
 * Emulate timestampadd() on databases with intervals
 * by producing an expression for the duration in the
 * database-specific syntax, then adding it to the given
 * TIMESTAMP.
 *
 * @author Gavin King
 */
abstract class IntervalTimestampaddEmulation
		extends AbstractSqmFunctionTemplate implements SelfRenderingFunctionSupport {

	IntervalTimestampaddEmulation() {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.useArgType( 3 )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		ExtractUnit field = (ExtractUnit) arguments.get(0);
		Expression magnitude = (Expression) arguments.get(1);
		Expression datetime = (Expression) arguments.get(2);
		sqlAppender.appendSql("(");
		datetime.accept(walker);
		boolean subtract = false;
		if (magnitude instanceof UnaryOperation) {
			UnaryOperation operation = (UnaryOperation) magnitude;
			if (operation.getOperator() == UNARY_MINUS) {
				magnitude = operation.getOperand();
				subtract = true;
			}
		}
		sqlAppender.appendSql(subtract ? " - " : " + ");
		renderInterval(sqlAppender, walker, field, magnitude);
		sqlAppender.appendSql(")");
	}

	/**
	 * Produce an expression for the added duration in
	 * terms of a unit and an integral quantity, using
	 * syntax that is totally database-specific.
	 *
	 * @param field the unit
	 * @param magnitude the integral number of units to add
	 */
	protected abstract void renderInterval(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			ExtractUnit field,
			Expression magnitude);

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine
	) {
		return new SelfRenderingSqmFunction<T>(
				this,
				arguments,
				impliedResultType,
				queryEngine.getCriteriaBuilder(),
				"timestampadd"
		);
	}
}
