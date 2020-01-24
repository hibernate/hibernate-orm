/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqlFunctionExpression;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.type.spi.TypeConfiguration.isSqlTimestampType;

/**
 * @author Gavin King
 */
public class TimestampdiffFunction
		extends AbstractSqmFunctionDescriptor {

	private Dialect dialect;

	public TimestampdiffFunction(Dialect dialect) {
		super(
				"timestampdiff",
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.LONG )
		);
		this.dialect = dialect;
	}

	@Override
	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmExtractUnit<?> field = (SqmExtractUnit<?>) arguments.get(0);
		SqmExpression<?> from = (SqmExpression<?>) arguments.get(1);
		SqmExpression<?> to = (SqmExpression<?>) arguments.get(2);
		return queryEngine.getSqmFunctionRegistry()
				.patternDescriptorBuilder(
						"timestampdiff",
						dialect.timestampdiffPattern(
								field.getUnit(),
								typeConfiguration.isTimestampType( from.getNodeType() ),
								typeConfiguration.isTimestampType( to.getNodeType() )
						)
				)
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 3 )
				.descriptor()
				.generateSqmExpression(
						arguments,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}

	public SelfRenderingFunctionSqlAstExpression expression(
			AllowableFunctionReturnType<?> impliedResultType,
			SqlAstNode... sqlAstArguments) {
		DurationUnit field = (DurationUnit) sqlAstArguments[0];
		Expression from = (Expression) sqlAstArguments[1];
		Expression to = (Expression) sqlAstArguments[2];
		return new SelfRenderingFunctionSqlAstExpression(
				new PatternRenderer(
						dialect.timestampdiffPattern(
								field.getUnit(),
								isSqlTimestampType( from.getExpressionType() ),
								isSqlTimestampType( to.getExpressionType() )
						)
				)::render,
				asList( sqlAstArguments ),
				impliedResultType,
				field.getExpressionType()
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(field, start, end)";
	}

}
