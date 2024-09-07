/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import jakarta.persistence.TemporalType;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;
import static org.hibernate.type.spi.TypeConfiguration.getSqlIntervalType;
import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

/**
 * The {@code timestampadd()} or {@code dateadd()} function has a funny
 * syntax which accepts a {@link TemporalUnit} as the first argument,
 * and the actual set of accepted units varies widely. This class uses
 * {@link Dialect#timestampaddPattern(TemporalUnit, TemporalType, IntervalType)}
 * to abstract these differences.
 *
 * @author Gavin King
 */
public class TimestampaddFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;
	private final SqlAstNodeRenderingMode[] renderingModes;

	public TimestampaddFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		this( dialect, typeConfiguration, SqlAstNodeRenderingMode.DEFAULT );
	}

	public TimestampaddFunction(Dialect dialect, TypeConfiguration typeConfiguration, SqlAstNodeRenderingMode... renderingModes) {
		super(
				"timestampadd",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						TEMPORAL_UNIT, INTEGER, TEMPORAL
				),
				StandardFunctionReturnTypeResolvers.useArgType( 3 ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL_UNIT, INTEGER, TEMPORAL )
		);
		this.dialect = dialect;
		this.renderingModes = renderingModes;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final DurationUnit field = (DurationUnit) arguments.get( 0 );
		final Expression magnitude = (Expression) arguments.get(1);
		final Expression to = (Expression) arguments.get( 2 );

		patternRenderer( field.getUnit(), magnitude, to ).render( sqlAppender, arguments, walker );
	}

	PatternRenderer patternRenderer(TemporalUnit unit, Expression interval, Expression to) {
		TemporalType temporalType = getSqlTemporalType( to.getExpressionType() );
		IntervalType intervalType = getSqlIntervalType( interval.getExpressionType().getSingleJdbcMapping() );
		return new PatternRenderer( dialect.timestampaddPattern( unit, temporalType, intervalType ), renderingModes );
	}

//	@Override
//	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			ReturnableType<T> impliedResultType,
//			QueryEngine queryEngine,
//			TypeConfiguration typeConfiguration) {
//		SqmExtractUnit<?> field = (SqmExtractUnit<?>) arguments.get(0);
//		SqmExpression<?> to = (SqmExpression<?>) arguments.get(2);
//		return queryEngine.getSqmFunctionRegistry()
//				.patternDescriptorBuilder(
//						"timestampadd",
//						dialect.timestampaddPattern(
//								field.getUnit(),
//								typeConfiguration.isSqlTimestampType( to.getNodeType() )
//						)
//				)
//				.setExactArgumentCount( 3 )
//				.setReturnTypeResolver( useArgType( 3 ) )
//				.descriptor()
//				.generateSqmExpression(
//						arguments,
//						impliedResultType,
//						queryEngine,
//						typeConfiguration
//				);
//	}

	public SelfRenderingFunctionSqlAstExpression expression(
			ReturnableType<?> impliedResultType,
			SqlAstNode... sqlAstArguments) {
		Expression to = (Expression) sqlAstArguments[2];
		return new SelfRenderingFunctionSqlAstExpression(
				getName(),
				this,
				asList( sqlAstArguments ),
				impliedResultType != null
						? impliedResultType
						: (ReturnableType<?>) to.getExpressionType().getSingleJdbcMapping(),
				to.getExpressionType()
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL_UNIT field, INTEGER magnitude, TEMPORAL datetime)";
	}

}
