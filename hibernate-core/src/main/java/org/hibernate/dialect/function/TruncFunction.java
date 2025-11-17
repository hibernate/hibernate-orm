/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.type.BindingContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL_UNIT;

/**
 * Custom function that manages both numeric and datetime truncation
 *
 * @author Marco Belladelli
 */
public class TruncFunction extends AbstractSqmFunctionDescriptor {
	protected final TruncRenderingSupport numericRenderingSupport;
	protected final TruncRenderingSupport datetimeRenderingSupport;
	private final DatetimeTrunc datetimeTrunc;
	private final DateTruncEmulation dateTruncEmulation;

	public enum DatetimeTrunc {
		DATE_TRUNC( "date_trunc('?2',?1)" ),
		DATETRUNC( "datetrunc(?2,?1)" ),
		TRUNC( "trunc(?1,?2)" ),
		FORMAT( null );

		private final String pattern;

		DatetimeTrunc(String pattern) {
			this.pattern = pattern;
		}

		public String getPattern() {
			return pattern;
		}
	}

	public TruncFunction(
			String truncPattern,
			String twoArgTruncPattern,
			DatetimeTrunc datetimeTrunc,
			String toDateFunction,
			TypeConfiguration typeConfiguration) {
		super(
				"trunc",
				new TruncArgumentsValidator(),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.byArgument(
						StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE,
						StandardFunctionArgumentTypeResolvers.NULL
				)
		);
		this.datetimeTrunc = datetimeTrunc;
		numericRenderingSupport =
				new TruncRenderingSupport( new PatternRenderer( truncPattern ),
						twoArgTruncPattern == null ? null : new PatternRenderer( twoArgTruncPattern ) );
		if ( datetimeTrunc == null ) {
			dateTruncEmulation = null;
			datetimeRenderingSupport = null;
		}
		else {
			if ( datetimeTrunc.getPattern() != null ) {
				datetimeRenderingSupport =
						new TruncRenderingSupport( new PatternRenderer( datetimeTrunc.getPattern() ), null );
				dateTruncEmulation = null;
			}
			else {
				dateTruncEmulation =
						new DateTruncEmulation( toDateFunction, typeConfiguration );
				datetimeRenderingSupport = null;
			}
		}
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final var nodeBuilder = queryEngine.getCriteriaBuilder();
		final List<SqmTypedNode<?>> args = new ArrayList<>( arguments );
		final FunctionRenderer renderingSupport;
		final ArgumentsValidator argumentsValidator;
		if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit<?> extractUnit ) {
			// datetime truncation
			renderingSupport = datetimeRenderingSupport;
			argumentsValidator = TruncArgumentsValidator.DATETIME_VALIDATOR;
			if ( datetimeTrunc == null ) {
				throw new UnsupportedOperationException( "Datetime truncation is not supported for this database" );
			}
			if ( datetimeTrunc.getPattern() == null ) {
				return dateTruncEmulation.generateSqmFunctionExpression(
						arguments,
						impliedResultType,
						queryEngine
				);
			}
			else if ( datetimeTrunc == DatetimeTrunc.TRUNC ) {
				// the trunc() function requires translating the temporal_unit to a format string
				final var temporalUnit = extractUnit.getUnit();
				final String pattern = switch ( temporalUnit ) {
					case YEAR -> "YYYY";
					case MONTH -> "MM";
					case WEEK -> "IW";
					case DAY -> "DD";
					case HOUR -> "HH";
					case MINUTE -> "MI";
					case SECOND -> "SS";
					default -> throw new UnsupportedOperationException(
							"Temporal unit not supported [" + temporalUnit + "]" );
				};
				// replace temporal_unit parameter with translated string format literal
				args.set( 1, new SqmLiteral<>( pattern, nodeBuilder.getStringType(), nodeBuilder ) );
			}
		}
		else {
			// numeric truncation
			renderingSupport = numericRenderingSupport;
			argumentsValidator = TruncArgumentsValidator.NUMERIC_VALIDATOR;
		}

		return new SelfRenderingSqmFunction<>(
				this,
				renderingSupport,
				args,
				impliedResultType,
				argumentsValidator,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	protected static class TruncRenderingSupport implements FunctionRenderer {
		private final PatternRenderer truncPattern;
		private final PatternRenderer twoArgTruncPattern;

		public TruncRenderingSupport(PatternRenderer truncPattern, PatternRenderer twoArgTruncPattern) {
			this.truncPattern = truncPattern;
			this.twoArgTruncPattern = twoArgTruncPattern;
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			final var pattern =
					twoArgTruncPattern != null && sqlAstArguments.size() == 2
							? twoArgTruncPattern
							: truncPattern;
			pattern.render( sqlAppender, sqlAstArguments, walker );
		}
	}

	protected static class TruncArgumentsValidator implements ArgumentsValidator {
		protected static final ArgumentTypesValidator DATETIME_VALIDATOR = new ArgumentTypesValidator(
				StandardArgumentsValidators.exactly( 2 ),
				TEMPORAL,
				TEMPORAL_UNIT
		);

		protected static final ArgumentTypesValidator NUMERIC_VALIDATOR = new ArgumentTypesValidator(
				StandardArgumentsValidators.between( 1, 2 ),
				NUMERIC,
				NUMERIC
		);

		@Override
		public void validate(
				List<? extends SqmTypedNode<?>> arguments,
				String functionName,
				BindingContext bindingContext) {
			final var validator =
					arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit
							? DATETIME_VALIDATOR
							: NUMERIC_VALIDATOR;
			validator.validate( arguments, functionName, bindingContext );
		}
	}
}
