/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Gavin King
 */
public class TimestampdiffFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

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
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {

		DurationUnit field = (DurationUnit) arguments.get(0);
		Expression from = (Expression) arguments.get(1);
		Expression to = (Expression) arguments.get(2);

		String pattern = dialect.timestampdiffPattern(
				field.getUnit(),
				TypeConfiguration.getSqlTemporalType( from.getExpressionType() ),
				TypeConfiguration.getSqlTemporalType( to.getExpressionType() )
		);

		new PatternRenderer( pattern ).render( sqlAppender, arguments, walker );
	}

//	@Override
//	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			AllowableFunctionReturnType<T> impliedResultType,
//			QueryEngine queryEngine,
//			TypeConfiguration typeConfiguration) {
//		SqmExtractUnit<?> field = (SqmExtractUnit<?>) arguments.get(0);
//		SqmExpression<?> from = (SqmExpression<?>) arguments.get(1);
//		SqmExpression<?> to = (SqmExpression<?>) arguments.get(2);
//		return queryEngine.getSqmFunctionRegistry()
//				.patternDescriptorBuilder(
//						"timestampdiff",
//						dialect.timestampdiffPattern(
//								field.getUnit(),
//								typeConfiguration.isSqlTimestampType( from.getNodeType() ),
//								typeConfiguration.isSqlTimestampType( to.getNodeType() )
//						)
//				)
//				.setInvariantType( StandardBasicTypes.LONG )
//				.setExactArgumentCount( 3 )
//				.descriptor()
//				.generateSqmExpression(
//						arguments,
//						impliedResultType,
//						queryEngine,
//						typeConfiguration
//				);
//	}

	public SelfRenderingFunctionSqlAstExpression expression(
			AllowableFunctionReturnType<?> impliedResultType,
			SqlAstNode... sqlAstArguments) {
		DurationUnit field = (DurationUnit) sqlAstArguments[0];
		return new SelfRenderingFunctionSqlAstExpression(
				this::render,
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
