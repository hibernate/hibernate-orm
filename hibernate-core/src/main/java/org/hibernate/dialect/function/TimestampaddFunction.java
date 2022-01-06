/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.Types;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;

/**
 * @author Gavin King
 */
public class TimestampaddFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final Dialect dialect;
	private final CastFunction castFunction;
	private final BasicType<Integer> integerType;

	public TimestampaddFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super(
				"timestampadd",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						TEMPORAL_UNIT, INTEGER, TEMPORAL
				),
				StandardFunctionReturnTypeResolvers.useArgType( 3 )
		);
		this.dialect = dialect;
		this.castFunction = new CastFunction( dialect, Types.BOOLEAN );
		this.integerType = typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {

		final DurationUnit field = (DurationUnit) arguments.get( 0 );
		final Expression to = (Expression) arguments.get( 2 );
		final TemporalUnit unit;
		if ( dialect.supportsFractionalTimestampArithmetic() ) {
			unit = field.getUnit();
		}
		else {
			final Expression magnitude = (Expression) arguments.get( 1 );
			final JdbcMapping magnitudeJdbcMapping = magnitude.getExpressionType().getJdbcMappings().get( 0 );
			switch ( magnitudeJdbcMapping.getJdbcTypeDescriptor().getJdbcTypeCode() ) {
				case Types.INTEGER:
				case Types.TINYINT:
				case Types.SMALLINT:
				case Types.BIGINT:
					unit = field.getUnit();
					break;
				default:
					if ( magnitudeJdbcMapping.getMappedJavaTypeDescriptor().getJavaTypeClass() == Duration.class ) {
						// Don't scale durations
						unit = field.getUnit();
					}
					else {
						// We need to multiply the magnitude by the conversion factor and cast to int
						// Use second by default and nanosecond if we encounter fractional seconds
						unit = field.getUnit() == TemporalUnit.SECOND
								? TemporalUnit.NANOSECOND
								: TemporalUnit.SECOND;
					}
					break;
			}
		}

		final String pattern = dialect.timestampaddPattern(
				unit,
				TypeConfiguration.getSqlTemporalType( to.getExpressionType() ),
				TypeConfiguration.getSqlIntervalType(
						( (Expression) arguments.get( 1 ) ).getExpressionType().getJdbcMappings().get( 0 )
				)
		);

		final PatternRenderer renderer = new PatternRenderer( pattern );
		if ( unit != field.getUnit() ) {
			final List<SqlAstNode> castArguments = new ArrayList<>( 2 );
			final List<SqlAstNode> newArguments = new ArrayList<>( arguments );
			final Expression magnitude = (Expression) arguments.get( 1 );
			final BasicValuedMapping expressionType = (BasicValuedMapping) magnitude.getExpressionType();
			final String conversionFactor = field.getUnit().conversionFactor( unit, dialect );
			if ( conversionFactor.isEmpty() ) {
				castArguments.add( magnitude );
			}
			else {
				castArguments.add(
						new BinaryArithmeticExpression(
								magnitude,
								conversionFactor.charAt( 0 ) == '*'
										? BinaryArithmeticOperator.MULTIPLY
										: BinaryArithmeticOperator.DIVIDE,
								new QueryLiteral<>(
										expressionType.getExpressableJavaTypeDescriptor()
												.fromString( conversionFactor.substring( 1 ) ),
										expressionType
								),
								expressionType
						)
				);
			}

			castArguments.add( new CastTarget( integerType ) );
			newArguments.set( 0, new DurationUnit( unit, field.getExpressionType() ) );
			newArguments.set(
					1,
					new SelfRenderingFunctionSqlAstExpression(
							"cast",
							castFunction::render,
							castArguments,
							integerType,
							integerType
					)

			);
			renderer.render( sqlAppender, newArguments, walker );
		}
		else {
			renderer.render( sqlAppender, arguments, walker );
		}
	}

//	@Override
//	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			AllowableFunctionReturnType<T> impliedResultType,
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
			AllowableFunctionReturnType<?> impliedResultType,
			SqlAstNode... sqlAstArguments) {
		Expression to = (Expression) sqlAstArguments[2];
		return new SelfRenderingFunctionSqlAstExpression(
				getName(),
				this::render,
				asList( sqlAstArguments ),
				impliedResultType != null
						? impliedResultType
						: (AllowableFunctionReturnType<?>) to.getExpressionType().getJdbcMappings().get( 0 ),
				to.getExpressionType()
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL_UNIT field, INTEGER magnitude, TEMPORAL datetime)";
	}

}
