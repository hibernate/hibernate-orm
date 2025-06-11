/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.internal;

import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.exception.AuthException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.AbstractSQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ConversionContext;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.JdbcExceptionHelper.determineSqlStateClassCode;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractSqlState;

/**
 * A {@link org.hibernate.exception.spi.SQLExceptionConverter} implementation which performs conversion based
 * on the underlying SQLState. Interpretation of a SQL error based on SQLState is not nearly as accurate as
 * using the ErrorCode (which is, however, vendor-specific).
 *
 * @implNote
 * SQLState codes are defined by both ANSI SQL specs and X/Open. Some "classes" are shared, others are
 * specific to one or another, yet others are custom vendor classes. Unfortunately I have not been able to
 * find a "blessed" list of X/Open codes. These codes are cobbled together between ANSI SQL spec and error
 * code tables from few vendor's documentation.
 *
 * @author Steve Ebersole
 */
public class SQLStateConversionDelegate extends AbstractSQLExceptionConversionDelegate {

	public SQLStateConversionDelegate(ConversionContext conversionContext) {
		super( conversionContext );
	}

	@Override
	public @Nullable JDBCException convert(SQLException sqlException, String message, String sql) {
		final String sqlState = extractSqlState( sqlException );
		if ( sqlState != null ) {
			switch ( sqlState ) {
				case "42501":
					return new AuthException( message, sqlException, sql );
				case "40001":
					return new LockAcquisitionException( message, sqlException, sql );
			}
			switch ( determineSqlStateClassCode( sqlState ) ) {
				case
					"07", 	// "dynamic SQL error"
					"20",
					"2A", 	// "direct SQL syntax error or access rule violation"
					"37",	// "dynamic SQL syntax error or access rule violation"
					"42",	// "syntax error or access rule violation"
					"65",	// Oracle specific as far as I can tell
					"S0":	// MySQL specific as far as I can tell
					return new SQLGrammarException( message, sqlException, sql );
				case
					"23",	// "integrity constraint violation"
					"27",	// "triggered data change violation"
					"44":	// "with check option violation"
					final String constraintName =
							getConversionContext().getViolatedConstraintNameExtractor()
									.extractConstraintName( sqlException );
					if ( sqlState.length() >= 5 ) {
						final ConstraintKind constraintKind = constraintKind( sqlState.substring( 0, 5 ) );
						return new ConstraintViolationException( message, sqlException, sql, constraintKind, constraintName );
					}
					else {
						return new ConstraintViolationException( message, sqlException, sql, constraintName );
					}
				case
					"08":	// "connection exception"
					return new JDBCConnectionException( message, sqlException, sql );
				case
					"21",	// "cardinality violation"
					"22":	// "data exception" (22001 is string too long; 22003 is numeric value out of range)
					return new DataException( message, sqlException, sql );
				case
					"28":	// "authentication failure"
					return new AuthException( message, sqlException, sql );
			}
		}
		return null;
	}

	private static ConstraintKind constraintKind(String trimmedState) {
		return switch ( trimmedState ) {
			case "23502" -> ConstraintKind.NOT_NULL;
			case "23505" -> ConstraintKind.UNIQUE;
			case "23503" -> ConstraintKind.FOREIGN_KEY;
			// 23510-3 indicate CHECK on Db2,
			// 23514 indicates CHECK on Postgres,
			// 23513-4 indicate CHECK on h2
			case "23510", "23511", "23512", "23513", "23514" -> ConstraintKind.CHECK;
			default -> ConstraintKind.OTHER;
		};
	}
}
