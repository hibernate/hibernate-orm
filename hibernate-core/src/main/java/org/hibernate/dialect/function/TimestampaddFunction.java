/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;
import static org.hibernate.type.spi.TypeConfiguration.isTimestampType;

/**
 * @author Gavin King
 */
public class TimestampaddFunction
		extends AbstractSqmFunctionTemplate {

	private Dialect dialect;

	public TimestampaddFunction(Dialect dialect) {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.useArgType( 3 )
		);
		this.dialect = dialect;
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		SqmExtractUnit<?> field = (SqmExtractUnit<?>) arguments.get(0);
		SqmExpression<?> to = (SqmExpression<?>) arguments.get(2);
		return queryEngine.getSqmFunctionRegistry()
				.patternTemplateBuilder(
						"timestampadd",
						dialect.timestampadd(
								field.getUnit(),
								typeConfiguration.isTimestampType( to.getExpressableType() )
						)
				)
				.setExactArgumentCount( 3 )
				.setReturnTypeResolver( useArgType( 3 ) )
				.template()
				.makeSqmFunctionExpression(
						arguments,
						impliedResultType,
						queryEngine,
						typeConfiguration
				);
	}

	public SelfRenderingFunctionSqlAstExpression expression(SqlAstNode... sqlAstArguments) {
		ExtractUnit field = (ExtractUnit) sqlAstArguments[0];
		Expression to = (Expression) sqlAstArguments[2];
		return new SelfRenderingFunctionSqlAstExpression(
				new PatternRenderer(
						dialect.timestampadd(
								field.getUnit(),
								isTimestampType( to.getType() )
						)
				)::render,
				asList( sqlAstArguments ),
				to.getType()
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(field, magnitude, datetime)";
	}

}
