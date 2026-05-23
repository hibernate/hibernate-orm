/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

import java.util.List;

import static java.lang.Character.isWhitespace;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard.isStandardRenderer;

/**
 * Helper for expanding native query collection-valued parameters.
 */
public final class NativeQueryParameterListHelper {
	private NativeQueryParameterListHelper() {
	}

	public static String expandParameterLists(
			String sqlString,
			List<ParameterOccurrence> parameterOccurrences,
			QueryParameterBindings parameterBindings,
			SessionFactoryImplementor factory,
			int parameterStartPosition) {
		if ( parameterOccurrences == null || parameterOccurrences.isEmpty() ) {
			return sqlString;
		}
		// HHH-1123
		// Some DBs limit the number of IN expressions. For now, warn.
		final var dialect = factory.getJdbcServices().getDialect();
		final boolean paddingEnabled = factory.getSessionFactoryOptions().inClauseParameterPaddingEnabled();
		final int inExprLimit = dialect.getInExpressionCountLimit();
		final var parameterMarkerStrategy = factory.getJdbcServices().getParameterMarkerStrategy();
		final boolean needsMarker = !isStandardRenderer( parameterMarkerStrategy );

		var sql =
				needsMarker
						? new StringBuilder( sqlString.length() + parameterOccurrences.size() * 10 )
								.append( sqlString )
						: null;

		// Handle parameter lists
		int offset = 0;
		int parameterPosition = parameterStartPosition;
		for ( var occurrence : parameterOccurrences ) {
			final var queryParameter = occurrence.parameter();
			final var binding = parameterBindings.getBinding( queryParameter );
			if ( binding.isMultiValued() ) {
				final int bindValueCount = binding.getBindValues().size();
				logTooManyExpressions( inExprLimit, bindValueCount, dialect, queryParameter );
				final int sourcePosition = occurrence.sourcePosition();
				if ( sourcePosition >= 0 ) {
					// check if placeholder is already immediately enclosed in parentheses
					// (ignoring whitespace)
					final boolean isEnclosedInParens = isEnclosedInParens( sqlString, sourcePosition );
					// short-circuit for performance when only 1 value and the
					// placeholder is already enclosed in parentheses...
					if ( bindValueCount != 1 || !isEnclosedInParens ) {
						if ( sql == null ) {
							sql = new StringBuilder( sqlString.length() + 20 )
									.append( sqlString );
						}
						final int bindValueMaxCount =
								determineBindValueMaxCount( paddingEnabled, inExprLimit, bindValueCount );
						final String expansionListAsString = expandList(
								bindValueMaxCount,
								isEnclosedInParens,
								parameterPosition,
								parameterMarkerStrategy,
								needsMarker
						);
						final int start = sourcePosition + offset;
						final int end = start + 1;
						sql.replace( start, end, expansionListAsString );
						offset += expansionListAsString.length() - 1;
						parameterPosition += bindValueMaxCount;
					}
					else if ( needsMarker ) {
						final int start = sourcePosition + offset;
						final int end = start + 1;
						final String parameterMarker = parameterMarkerStrategy.createMarker( parameterPosition, null );
						sql.replace( start, end, parameterMarker );
						offset += parameterMarker.length() - 1;
						parameterPosition++;
					}
				}
			}
			else if ( needsMarker ) {
				final int sourcePosition = occurrence.sourcePosition();
				final int start = sourcePosition + offset;
				final int end = start + 1;
				final String parameterMarker = parameterMarkerStrategy.createMarker( parameterPosition, null );
				sql.replace( start, end, parameterMarker );
				offset += parameterMarker.length() - 1;
				parameterPosition++;
			}
		}
		return sql == null ? sqlString : sql.toString();
	}

	private static void logTooManyExpressions(
			int inExprLimit, int bindValueCount,
			Dialect dialect, QueryParameterImplementor<?> queryParameter) {
		if ( inExprLimit > 0 && bindValueCount > inExprLimit ) {
			CORE_LOGGER.tooManyInExpressions(
					dialect.getClass().getName(),
					inExprLimit,
					queryParameter.getName() == null
							? queryParameter.getPosition().toString()
							: queryParameter.getName(),
					bindValueCount
			);
		}
	}

	private static String expandList(
			int bindValueMaxCount,
			boolean isEnclosedInParens,
			int parameterPosition,
			ParameterMarkerStrategy parameterMarkerStrategy,
			boolean needsMarker) {
		// HHH-8901
		if ( bindValueMaxCount == 0 ) {
			return isEnclosedInParens ? "null" : "(null)";
		}
		else if ( needsMarker ) {
			final StringBuilder sb = new StringBuilder( bindValueMaxCount * 4 );
			if ( !isEnclosedInParens ) {
				sb.append( '(' );
			}
			for ( int i = 0; i < bindValueMaxCount; i++ ) {
				sb.append( parameterMarkerStrategy.createMarker( parameterPosition + i, null ) );
				sb.append( ',' );
			}
			sb.setLength( sb.length() - 1 );
			if ( !isEnclosedInParens ) {
				sb.append( ')' );
			}
			return sb.toString();
		}
		else {
			// Shift 1 bit instead of multiplication by 2
			final char[] chars;
			if ( isEnclosedInParens ) {
				chars = new char[(bindValueMaxCount << 1) - 1];
				chars[0] = '?';
				for ( int i = 1; i < bindValueMaxCount; i++ ) {
					final int index = i << 1;
					chars[index - 1] = ',';
					chars[index] = '?';
				}
			}
			else {
				chars = new char[(bindValueMaxCount << 1) + 1];
				chars[0] = '(';
				chars[1] = '?';
				for ( int i = 1; i < bindValueMaxCount; i++ ) {
					final int index = i << 1;
					chars[index] = ',';
					chars[index + 1] = '?';
				}
				chars[chars.length - 1] = ')';
			}
			return new String( chars );
		}
	}

	private static boolean isEnclosedInParens(String sqlString, int sourcePosition) {
		boolean isEnclosedInParens = true;
		for ( int i = sourcePosition - 1; i >= 0; i-- ) {
			final char ch = sqlString.charAt( i );
			if ( !isWhitespace( ch ) ) {
				isEnclosedInParens = ch == '(';
				break;
			}
		}
		if ( isEnclosedInParens ) {
			for ( int i = sourcePosition + 1; i < sqlString.length(); i++ ) {
				final char ch = sqlString.charAt( i );
				if ( !isWhitespace( ch ) ) {
					isEnclosedInParens = ch == ')';
					break;
				}
			}
		}
		return isEnclosedInParens;
	}

	public static int determineBindValueMaxCount(boolean paddingEnabled, int inExprLimit, int bindValueCount) {
		int bindValueMaxCount = bindValueCount;

		final boolean inClauseParameterPaddingEnabled = paddingEnabled && bindValueCount > 2;

		if ( inClauseParameterPaddingEnabled ) {
			int bindValuePaddingCount = MathHelper.ceilingPowerOfTwo( bindValueCount );

			if ( inExprLimit > 0 && bindValuePaddingCount > inExprLimit ) {
				bindValuePaddingCount = inExprLimit;
			}

			if ( bindValueCount < bindValuePaddingCount ) {
				bindValueMaxCount = bindValuePaddingCount;
			}
		}
		return bindValueMaxCount;
	}
}
