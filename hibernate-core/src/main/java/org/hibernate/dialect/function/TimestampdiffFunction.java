/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;
import org.checkerframework.checker.nullness.qual.Nullable;

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
	private final SqlAstNodeRenderingMode[] renderingModes;

	public TimestampdiffFunction(Dialect dialect, TypeConfiguration typeConfiguration) {
		this( dialect, typeConfiguration, SqlAstNodeRenderingMode.DEFAULT );
	}

	public TimestampdiffFunction(Dialect dialect, TypeConfiguration typeConfiguration, SqlAstNodeRenderingMode... renderingModes) {
		super(
				"timestampdiff",
				new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						TEMPORAL_UNIT, TEMPORAL, TEMPORAL
				),
				new TimestampdiffFunctionReturnTypeResolver(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LONG ),
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL_UNIT, TEMPORAL, TEMPORAL )
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
		final Expression from = (Expression) arguments.get( 1 );
		final Expression to = (Expression) arguments.get( 2 );

		patternRenderer( field == null ? null : field.getUnit(), from, to ).render( sqlAppender, arguments, walker );
	}

	private PatternRenderer patternRenderer(TemporalUnit unit, Expression from, Expression to) {
		TemporalType lhsTemporalType = getSqlTemporalType( from.getExpressionType() );
		TemporalType rhsTemporalType = getSqlTemporalType( to.getExpressionType() );
		return new PatternRenderer( dialect.timestampdiffPattern( unit, lhsTemporalType, rhsTemporalType ), renderingModes );
	}

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
				field != null
						? field.getExpressionType()
						: (JdbcMappingContainer) impliedResultType
		);
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL_UNIT field, TEMPORAL start, TEMPORAL end)";
	}

	/**
	 * A special resolver that resolves to DOUBLE for {@link TemporalUnit#SECOND} and otherwise to LONG.
	 */
	private static class TimestampdiffFunctionReturnTypeResolver implements FunctionReturnTypeResolver {

		private final BasicType<Long> longType;
		private final BasicType<Double> doubleType;

		public TimestampdiffFunctionReturnTypeResolver(BasicType<Long> longType, BasicType<Double> doubleType) {
			this.longType = longType;
			this.doubleType = doubleType;
		}

		@Override
		public ReturnableType<?> resolveFunctionReturnType(
				ReturnableType<?> impliedType,
				@Nullable SqmToSqlAstConverter converter,
				List<? extends SqmTypedNode<?>> arguments,
				TypeConfiguration typeConfiguration) {
			final BasicType<?> invariantType;
			if ( ( (SqmDurationUnit<?>) arguments.get( 0 ) ).getUnit() == TemporalUnit.SECOND ) {
				invariantType = doubleType;
			}
			else {
				invariantType = longType;
			}
			return StandardFunctionReturnTypeResolvers.isAssignableTo( invariantType, impliedType )
					? impliedType : invariantType;
		}

		@Override
		public BasicValuedMapping resolveFunctionReturnType(
				Supplier<BasicValuedMapping> impliedTypeAccess,
				List<? extends SqlAstNode> arguments) {
			final BasicType<?> invariantType;
			if ( ( (SqmDurationUnit<?>) arguments.get( 0 ) ).getUnit() == TemporalUnit.SECOND ) {
				invariantType = doubleType;
			}
			else {
				invariantType = longType;
			}
			return StandardFunctionReturnTypeResolvers.useImpliedTypeIfPossible( invariantType, impliedTypeAccess.get() );
		}

		@Override
		public String getReturnType() {
			return longType.getJavaType().getSimpleName()
				+ "|" + doubleType.getJavaType().getSimpleName();
		}
	}

}
