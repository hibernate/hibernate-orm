/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
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

import java.util.Arrays;
import java.util.List;

import static org.hibernate.query.sqm.BinaryArithmeticOperator.DIVIDE;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.MULTIPLY;

/**
 * Used in place of {@link TimestampaddFunction} for databases which don't
 * support fractional seconds in the {@code timestampadd()} function.
 *
 * @author Christian Beikov
 */
public class IntegralTimestampaddFunction
		extends TimestampaddFunction {

	private final Dialect dialect;
	private final CastFunction castFunction;
	private final BasicType<Integer> integerType;

	public IntegralTimestampaddFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		super( dialect, typeConfiguration );
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
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final DurationUnit field = (DurationUnit) arguments.get( 0 );
		final Expression magnitude = (Expression) arguments.get(1);
		final Expression to = (Expression) arguments.get( 2 );

		final TemporalUnit unit = bestTemporalUnit( magnitude, field );
		if ( unit != field.getUnit() ) {
			renderWithUnitConversion( sqlAppender, magnitude, to, walker, field, unit );
		}
		else {
			super.render( sqlAppender, arguments, returnType, walker );
		}
	}

	private void renderWithUnitConversion(
			SqlAppender sqlAppender,
			Expression magnitude,
			Expression to,
			SqlAstTranslator<?> walker,
			DurationUnit field,
			TemporalUnit unit) {
		patternRenderer( unit, magnitude, to )
				.render( sqlAppender, convertedArguments( field, unit, magnitude, to ), walker );
	}

	private List<SqlAstNode> convertedArguments(
			DurationUnit field,
			TemporalUnit unit,
			Expression magnitude,
			Expression to) {

		return Arrays.asList(
				new DurationUnit( unit, field.getExpressionType() ),
				new SelfRenderingFunctionSqlAstExpression(
						"cast",
						castFunction,
						Arrays.asList(
								convertedArgument(field, unit, magnitude),
								new CastTarget( integerType )
						),
						integerType,
						integerType
				),
				to
		);
	}

	private Expression convertedArgument(DurationUnit field, TemporalUnit unit, Expression magnitude) {
		final BasicValuedMapping expressionType = (BasicValuedMapping) magnitude.getExpressionType();
		final String conversionFactor = field.getUnit().conversionFactorFull( unit, dialect );
		return conversionFactor.isEmpty()
				? magnitude
				: new BinaryArithmeticExpression(
						magnitude,
						conversionFactor.charAt(0) == '*' ? MULTIPLY : DIVIDE,
						new QueryLiteral<>(
								expressionType.getExpressibleJavaType()
										.fromString( conversionFactor.substring(1) ),
								expressionType
						),
						expressionType
				);
	}

	private TemporalUnit bestTemporalUnit(Expression magnitude, DurationUnit field) {
		final JdbcType jdbcType = magnitude.getExpressionType().getSingleJdbcMapping().getJdbcType();
		if ( jdbcType.isFloat() ) {
			// We need to multiply the magnitude by the conversion factor and cast to int
			// Use SECOND by default and NATIVE if we encounter fractional seconds
			return field.getUnit() == TemporalUnit.SECOND
					? TemporalUnit.NATIVE
					: TemporalUnit.SECOND;
		}
		else {
			return field.getUnit();
		}
	}
}
