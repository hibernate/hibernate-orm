/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.JDBCException;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.AbstractSQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.internal.util.JdbcExceptionHelper;

/**
 * A SQLExceptionConverter implementation which performs conversion based on the underlying SQLState.
 * Interpretation of a SQL error based on SQLState is not nearly as accurate as using the ErrorCode (which is,
 * however, vendor-specific).
 *
 * SQLState codes are defined by both ANSI SQL specs and X/Open.  Some of the "classes" are shared, others are
 * specific to one or another, yet others are custom vendor classes.  Unfortunately I have not been able to
 * find a "blessed" list of X/Open codes.  These codes are cobbled together between ANSI SQL spec and error
 * code tables from few vendors documentation.
 *
 * @author Steve Ebersole
 */
public class SQLStateConversionDelegate extends AbstractSQLExceptionConversionDelegate {

	private static final Set<String> SQL_GRAMMAR_CATEGORIES = buildGrammarCategories();
	private static Set<String> buildGrammarCategories() {
		HashSet<String> categories = new HashSet<String>(
				Arrays.asList(
						"07", 	// "dynamic SQL error"
						"20",
						"2A", 	// "direct SQL syntax error or access rule violation"
						"37",	// "dynamic SQL syntax error or access rule violation"
						"42",	// "syntax error or access rule violation"
						"65",	// Oracle specific as far as I can tell
						"S0"	// MySQL specific as far as I can tell
				)
		);
		return Collections.unmodifiableSet( categories );
	}

	private static final Set DATA_CATEGORIES = buildDataCategories();
	private static Set<String> buildDataCategories() {
		HashSet<String> categories = new HashSet<String>(
				Arrays.asList(
						"21",	// "cardinality violation"
						"22"	// "data exception"
				)
		);
		return Collections.unmodifiableSet( categories );
	}

	private static final Set INTEGRITY_VIOLATION_CATEGORIES = buildContraintCategories();
	private static Set<String> buildContraintCategories() {
		HashSet<String> categories = new HashSet<String>(
				Arrays.asList(
						"23",	// "integrity constraint violation"
						"27",	// "triggered data change violation"
						"44"	// "with check option violation"
				)
		);
		return Collections.unmodifiableSet( categories );
	}

	private static final Set CONNECTION_CATEGORIES = buildConnectionCategories();
	private static Set<String> buildConnectionCategories() {
		HashSet<String> categories = new HashSet<String>();
		categories.add(
				"08"	// "connection exception"
		);
		return Collections.unmodifiableSet( categories );
	}

	public SQLStateConversionDelegate(ConversionContext conversionContext) {
		super( conversionContext );
	}

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
		final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

		if ( sqlState != null ) {
			String sqlStateClassCode = JdbcExceptionHelper.determineSqlStateClassCode( sqlState );

			if ( sqlStateClassCode != null ) {
				if ( SQL_GRAMMAR_CATEGORIES.contains( sqlStateClassCode ) ) {
					return new SQLGrammarException( message, sqlException, sql );
				}
				else if ( INTEGRITY_VIOLATION_CATEGORIES.contains( sqlStateClassCode ) ) {
					final String constraintName = getConversionContext()
							.getViolatedConstraintNameExtracter()
							.extractConstraintName( sqlException );
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}
				else if ( CONNECTION_CATEGORIES.contains( sqlStateClassCode ) ) {
					return new JDBCConnectionException( message, sqlException, sql );
				}
				else if ( DATA_CATEGORIES.contains( sqlStateClassCode ) ) {
					return new DataException( message, sqlException, sql );
				}
			}

			if ( "40001".equals( sqlState ) ) {
				return new LockAcquisitionException( message, sqlException, sql );
			}

			if ( "40XL1".equals( sqlState ) || "40XL2".equals( sqlState )) {
				// Derby "A lock could not be obtained within the time requested."
				return new PessimisticLockException( message, sqlException, sql );
			}

			// MySQL Query execution was interrupted
			if ( "70100".equals( sqlState ) ||
					// Oracle user requested cancel of current operation
					( "72000".equals( sqlState ) && errorCode == 1013 ) ) {
				throw new QueryTimeoutException(  message, sqlException, sql );
			}
		}

		return null;
	}
}
