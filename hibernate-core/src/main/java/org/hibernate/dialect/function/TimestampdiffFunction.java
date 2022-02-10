/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static java.util.Arrays.asList;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;
import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;

/**
 * The {@code timestampdiff()} or {@code datediff()} function has a funny
 * syntax which accepts a {@link TemporalUnit} as the first argument, and
 * the actual set of accepted units varies widely. This class uses
 * {@link Dialect#timestampdiffPattern(TemporalUnit, TemporalType, TemporalType)}
 * to abstract these differences.
 *
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
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL_UNIT, TEMPORAL, TEMPORAL )
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

		patternRenderer( field.getUnit(), from, to ).render( sqlAppender, arguments, walker );
	}

	private PatternRenderer patternRenderer(TemporalUnit unit, Expression from, Expression to) {
		TemporalType lhsTemporalType = getSqlTemporalType( from.getExpressionType() );
		TemporalType rhsTemporalType = getSqlTemporalType( to.getExpressionType() );
		return new PatternRenderer( dialect.timestampdiffPattern( unit, lhsTemporalType, rhsTemporalType ) );
	}

//	@Override
//	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
//			List<SqmTypedNode<?>> arguments,
//			ReturnableType<T> impliedResultType,
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
			ReturnableType<?> impliedResultType,
			SqlAstNode... sqlAstArguments) {
		DurationUnit field = (DurationUnit) sqlAstArguments[0];
		return new SelfRenderingFunctionSqlAstExpression(
				getName(),
				this,
				asList( sqlAstArguments ),
				impliedResultType != null
						? impliedResultType
						: (ReturnableType<?>) field.getExpressionType().getJdbcMapping(),
				field.getExpressionType()
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL_UNIT field, TEMPORAL start, TEMPORAL end)";
	}

}
