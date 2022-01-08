/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.AllowableFunctionReturnType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;

/**
 * @author Gavin King
 */
public class TimestampdiffFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;

	public TimestampdiffFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super(
				"timestampdiff",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						TEMPORAL_UNIT, TEMPORAL, TEMPORAL
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LONG )
				)
		);
		this.dialect = dialect;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {

		final DurationUnit field = (DurationUnit) arguments.get( 0 );
		final Expression from = (Expression) arguments.get( 1 );
		final Expression to = (Expression) arguments.get( 2 );
		final String pattern = dialect.timestampdiffPattern(
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
				getName(),
				this,
				asList( sqlAstArguments ),
				impliedResultType != null
						? impliedResultType
						: (AllowableFunctionReturnType<?>) field.getExpressionType().getJdbcMapping(),
				field.getExpressionType()
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL_UNIT field, TEMPORAL start, TEMPORAL end)";
	}

}
