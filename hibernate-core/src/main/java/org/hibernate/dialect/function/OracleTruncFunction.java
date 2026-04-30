/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Custom {@link TruncFunction} for Oracle which uses emulation when truncating datetimes to seconds
 * or when truncating offset-aware datetimes.
 *
 * @author Marco Belladelli
 */
public class OracleTruncFunction extends TruncFunction {

	public OracleTruncFunction(TypeConfiguration typeConfiguration) {
		super(
				"trunc(?1)",
				"trunc(?1,?2)",
				DatetimeTrunc.TRUNC,
				null,
				typeConfiguration
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<? extends SqmTypedNode<?>> arguments,
			ReturnableType<T> impliedResultType,
			QueryEngine queryEngine) {
		final List<SqmTypedNode<?>> args = new ArrayList<>( arguments );
		final FunctionRenderer renderer;
		final ArgumentsValidator argumentsValidator;
		if ( arguments.size() == 2 && arguments.get( 1 ) instanceof SqmExtractUnit ) {
			// datetime truncation
			argumentsValidator = TruncArgumentsValidator.DATETIME_VALIDATOR;
			// the trunc() function requires translating the temporal_unit to a format string
			final TemporalUnit temporalUnit = ( (SqmExtractUnit<?>) arguments.get( 1 ) ).getUnit();
			renderer = new OracleTruncRenderingSupport( temporalUnit );
			final String pattern;
			switch ( temporalUnit ) {
				case YEAR:
					pattern = "YYYY";
					break;
				case MONTH:
					pattern = "MM";
					break;
				case WEEK:
					pattern = "IW";
					break;
				case DAY:
					pattern = "DD";
					break;
				case HOUR:
					pattern = "HH";
					break;
				case MINUTE:
					pattern = "MI";
					break;
				case SECOND:
					pattern = null;
					break;
				default:
					throw new UnsupportedOperationException( "Temporal unit not supported [" + temporalUnit + "]" );
			}
			// replace temporal_unit parameter with translated string format literal
			if ( pattern != null ) {
				args.set( 1, new SqmLiteral<>(
						pattern,
						queryEngine.getTypeConfiguration().getBasicTypeForJavaType( String.class ),
						queryEngine.getCriteriaBuilder()
				) );
			}
		}
		else {
			// numeric truncation
			renderer = numericRenderingSupport;
			argumentsValidator = TruncArgumentsValidator.NUMERIC_VALIDATOR;
		}

		return new SelfRenderingSqmFunction<>(
				this,
				renderer,
				args,
				impliedResultType,
				argumentsValidator,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

	private static class OracleTruncRenderingSupport implements FunctionRenderer {
		private final TemporalUnit temporalUnit;

		private OracleTruncRenderingSupport(TemporalUnit temporalUnit) {
			this.temporalUnit = temporalUnit;
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
				ReturnableType<?> returnType,
				SqlAstTranslator<?> walker) {
			if ( isOffsetOrZonedTimestamp( sqlAstArguments.get( 0 ) ) ) {
				renderOffsetTimestampTrunc( sqlAppender, sqlAstArguments, walker );
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
				sqlAppender.appendSql( ',' );
				sqlAstArguments.get( 1 ).accept( walker );
				sqlAppender.appendSql( ')' );
				if ( isTimestamp( sqlAstArguments.get( 0 ) ) ) {
					sqlAppender.appendSql( " as timestamp)" );
				}
			}
		}

		private void renderOffsetTimestampTrunc(
				SqlAppender sqlAppender,
				List<? extends SqlAstNode> sqlAstArguments,
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
				sqlAppender.appendSql( ",'YYYY-MM-DD HH24:MI:SS.FF9'),'YYYY-MM-DD HH24:MI:SS.FF9')," );
				sqlAstArguments.get( 1 ).accept( walker );
				sqlAppender.appendSql( ") as timestamp)" );
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

		private static boolean isOffsetOrZonedTimestamp(SqlAstNode datetime) {
			final CastType castType = getCastType( datetime );
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
				final JdbcMappingContainer expressionType = expression.getExpressionType();
				if ( expressionType != null && expressionType.getJdbcTypeCount() == 1 ) {
					return expressionType.getSingleJdbcMapping().getCastType();
				}
			}
			return null;
		}
	}
}
