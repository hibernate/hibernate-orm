/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import jakarta.persistence.TemporalType;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
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
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;
import static org.hibernate.type.spi.TypeConfiguration.getSqlIntervalType;
import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

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
		this.integerType = typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER );
		//This is kinda wrong, we're supposed to use findFunctionDescriptor("cast"), not instantiate CastFunction
		//However, since no Dialects currently override the cast() function, it's OK for now
		this.castFunction = new CastFunction( dialect, dialect.getPreferredSqlTypeCodeForBoolean() );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {

		final DurationUnit field = (DurationUnit) arguments.get( 0 );
		final Expression magnitude = (Expression) arguments.get(1);
		final Expression to = (Expression) arguments.get( 2 );

		final TemporalUnit unit = bestTemporalUnit( magnitude, field );
		if ( unit != field.getUnit() ) {
			renderWithUnitConversion( sqlAppender, arguments, walker, field, unit );
		}
		else {
			patternRenderer( to, magnitude, unit ).render( sqlAppender, arguments, walker );
		}
	}

	private PatternRenderer patternRenderer(Expression to, Expression interval, TemporalUnit unit) {
		TemporalType temporalType = getSqlTemporalType( to.getExpressionType() );
		IntervalType intervalType = getSqlIntervalType( interval.getExpressionType().getJdbcMappings().get(0) );
		return new PatternRenderer( dialect.timestampaddPattern( unit, temporalType, intervalType ) );
	}

	private void renderWithUnitConversion(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			SqlAstTranslator<?> walker,
			DurationUnit field,
			TemporalUnit unit) {
		final Expression magnitude = (Expression) arguments.get( 1 );
		final Expression to = (Expression) arguments.get( 2 );
		final Expression interval = (Expression) arguments.get(1);

		final List<SqlAstNode> castArguments = new ArrayList<>( 2 );
		final List<SqlAstNode> newArguments = new ArrayList<>( arguments );

		castArguments.add( convertedArgument( field, unit, magnitude ) );
		castArguments.add( new CastTarget( integerType ) );
		newArguments.set( 0, new DurationUnit( unit, field.getExpressionType() ) );
		newArguments.set(
				1,
				new SelfRenderingFunctionSqlAstExpression(
						"cast",
						castFunction,
						castArguments,
						integerType,
						integerType
				)

		);
		patternRenderer( to, interval, unit ).render( sqlAppender, newArguments, walker );
	}

	private Expression convertedArgument(DurationUnit field, TemporalUnit unit, Expression magnitude) {
		final BasicValuedMapping expressionType = (BasicValuedMapping) magnitude.getExpressionType();
		final String conversionFactor = field.getUnit().conversionFactor( unit, dialect );
		return conversionFactor.isEmpty()
				? magnitude
				: new BinaryArithmeticExpression(
						magnitude,
						conversionFactor.charAt(0) == '*'
								? BinaryArithmeticOperator.MULTIPLY
								: BinaryArithmeticOperator.DIVIDE,
						new QueryLiteral<>(
								expressionType.getExpressibleJavaType().fromString( conversionFactor.substring(1) ),
								expressionType
						),
						expressionType
				);
	}

	private TemporalUnit bestTemporalUnit(Expression magnitude, DurationUnit field) {
		if ( dialect.supportsFractionalTimestampArithmetic() ) {
			return field.getUnit();
		}
		else {
			final JdbcType jdbcType = magnitude.getExpressionType().getJdbcMappings().get( 0 ).getJdbcType();
			if ( jdbcType.isFloat() ) {
				// Some databases don't support fractional seconds
				// We need to multiply the magnitude by the conversion factor and cast to int
				// Use second by default and nanosecond if we encounter fractional seconds
				return field.getUnit() == TemporalUnit.SECOND
						? TemporalUnit.NANOSECOND
						: TemporalUnit.SECOND;
			}
			else {
				return field.getUnit();
			}
		}
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
						: (ReturnableType<?>) to.getExpressionType().getJdbcMappings().get( 0 ),
				to.getExpressionType()
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL_UNIT field, INTEGER magnitude, TEMPORAL datetime)";
	}

}
