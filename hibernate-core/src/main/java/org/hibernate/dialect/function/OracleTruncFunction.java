/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Expression;

import static org.hibernate.dialect.function.TruncFunction.TruncArgumentsValidator.DATETIME_VALIDATOR;
import static org.hibernate.dialect.function.TruncFunction.TruncArgumentsValidator.NUMERIC_VALIDATOR;

/**
 * Oracle trunc function which uses emulation when truncating datetimes to seconds
 * or when truncating offset-aware datetimes.
 *
 * @author Marco Belladelli
 */
public class OracleTruncFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public OracleTruncFunction() {
		super(
				"trunc",
				new TruncFunction.TruncArgumentsValidator(),
				StandardFunctionReturnTypeResolvers.useArgType( 1 ),
				StandardFunctionArgumentTypeResolvers.byArgument(
						StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE,
						StandardFunctionArgumentTypeResolvers.NULL
				)
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final ArgumentsValidator argumentsValidator;
		if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit ) {
			// datetime truncation
			argumentsValidator = DATETIME_VALIDATOR;
			datetimeFormat( ( (SqmExtractUnit<?>) arguments.get( 1 ) ).getUnit() );
		}
		else {
			// numeric truncation
			argumentsValidator = NUMERIC_VALIDATOR;
		}

		return new SelfRenderingSqmFunction<>(
				this,
				this,
				arguments,
				impliedResultType,
				argumentsValidator,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		if ( sqlAstArguments.size() == 2 && sqlAstArguments.get( 1 ) instanceof ExtractUnit extractUnit ) {
			renderDatetimeTrunc( sqlAppender, sqlAstArguments, extractUnit.getUnit(), walker );
		}
		else {
			renderNumericTrunc( sqlAppender, sqlAstArguments, walker );
		}
	}

	private static void renderDatetimeTrunc(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			TemporalUnit temporalUnit,
			SqlAstTranslator<?> walker) {
		if ( isOffsetOrZonedTimestamp( sqlAstArguments.get( 0 ) ) ) {
			renderOffsetTimestampTrunc( sqlAppender, sqlAstArguments, temporalUnit, walker );
		}
		else if ( temporalUnit == TemporalUnit.SECOND ) {
			if ( isTimestamp( sqlAstArguments.get( 0 ) ) ) {
				sqlAppender.appendSql( "cast(" );
				renderTimestampTruncToSecond( sqlAppender, sqlAstArguments.get( 0 ), walker );
				sqlAppender.appendSql( " as timestamp)" );
			}
			else {
				renderTimestampTruncToSecond( sqlAppender, sqlAstArguments.get( 0 ), walker );
			}
		}
		else {
			if ( isTimestamp( sqlAstArguments.get( 0 ) ) ) {
				sqlAppender.appendSql( "cast(" );
			}
			sqlAppender.appendSql( "trunc(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ",'" );
			sqlAppender.appendSql( datetimeFormat( temporalUnit ) );
			sqlAppender.appendSql( "')" );
			if ( isTimestamp( sqlAstArguments.get( 0 ) ) ) {
				sqlAppender.appendSql( " as timestamp)" );
			}
		}
	}

	private static void renderNumericTrunc(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "trunc(" );
		sqlAstArguments.get( 0 ).accept( walker );
		if ( sqlAstArguments.size() == 2 ) {
			sqlAppender.appendSql( ',' );
			sqlAstArguments.get( 1 ).accept( walker );
		}
		sqlAppender.appendSql( ')' );
	}

	private static void renderOffsetTimestampTrunc(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			TemporalUnit temporalUnit,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "from_tz(" );
		if ( temporalUnit == TemporalUnit.SECOND ) {
			sqlAppender.appendSql( "cast(" );
			renderTimestampTruncToSecond( sqlAppender, sqlAstArguments.get( 0 ), walker );
			sqlAppender.appendSql( " as timestamp)" );
		}
		else {
			sqlAppender.appendSql( "cast(trunc(to_timestamp(to_char(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ",'YYYY-MM-DD HH24:MI:SS.FF9'),'YYYY-MM-DD HH24:MI:SS.FF9'),'" );
			sqlAppender.appendSql( datetimeFormat( temporalUnit ) );
			sqlAppender.appendSql( "') as timestamp)" );
		}
		sqlAppender.appendSql( ",to_char(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( ",'" );
		sqlAppender.appendSql( getTimezoneFormat( sqlAstArguments.get( 0 ) ) );
		sqlAppender.appendSql( "'))" );
	}

	private static void renderTimestampTruncToSecond(
			SqlAppender sqlAppender,
			SqlAstNode datetime,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "to_date(to_char(" );
		datetime.accept( walker );
		sqlAppender.appendSql( ",'YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD HH24:MI:SS')" );
	}

	private static String datetimeFormat(TemporalUnit temporalUnit) {
		return switch ( temporalUnit ) {
			case YEAR -> "YYYY";
			case MONTH -> "MM";
			case WEEK -> "IW";
			case DAY -> "DD";
			case HOUR -> "HH";
			case MINUTE -> "MI";
			case SECOND -> null;
			default -> throw new UnsupportedOperationException( "Temporal unit not supported [" + temporalUnit + "]" );
		};
	}

	private static boolean isOffsetOrZonedTimestamp(SqlAstNode datetime) {
		final var castType = getCastType( datetime );
		return castType == CastType.OFFSET_TIMESTAMP || castType == CastType.ZONE_TIMESTAMP;
	}

	private static boolean isTimestamp(SqlAstNode datetime) {
		return getCastType( datetime ) == CastType.TIMESTAMP;
	}

	private static String getTimezoneFormat(SqlAstNode datetime) {
		return getCastType( datetime ) == CastType.ZONE_TIMESTAMP ? "TZR" : "TZH:TZM";
	}

	private static CastType getCastType(SqlAstNode datetime) {
		if ( datetime instanceof Expression expression ) {
			final var expressionType = expression.getExpressionType();
			if ( expressionType != null && expressionType.getJdbcTypeCount() == 1 ) {
				return expressionType.getSingleJdbcMapping().getCastType();
			}
		}
		return null;
	}
}
