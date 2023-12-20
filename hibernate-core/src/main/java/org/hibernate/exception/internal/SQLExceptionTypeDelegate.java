/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.internal;

import java.sql.DataTruncation;
import java.sql.SQLClientInfoException;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;

import org.hibernate.JDBCException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.AbstractSQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ConversionContext;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link org.hibernate.exception.spi.SQLExceptionConverter} implementation
 * that does conversion based on the {@link SQLException} subtype hierarchy
 * defined by JDBC 4.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionTypeDelegate extends AbstractSQLExceptionConversionDelegate {
	public SQLExceptionTypeDelegate(ConversionContext conversionContext) {
		super( conversionContext );
	}

	@Override
	public @Nullable JDBCException convert(SQLException sqlException, String message, String sql) {
		if ( sqlException instanceof SQLClientInfoException
				|| sqlException instanceof SQLInvalidAuthorizationSpecException
				|| sqlException instanceof SQLNonTransientConnectionException
				|| sqlException instanceof SQLTransientConnectionException ) {
			return new JDBCConnectionException( message, sqlException, sql );
		}
		else if ( sqlException instanceof DataTruncation ||
				sqlException instanceof SQLDataException ) {
			return new DataException( message, sqlException, sql );
		}
		else if ( sqlException instanceof SQLIntegrityConstraintViolationException ) {
			return new ConstraintViolationException(
					message,
					sqlException,
					sql,
					getConversionContext().getViolatedConstraintNameExtractor().extractConstraintName( sqlException )
			);
		}
		else if ( sqlException instanceof SQLSyntaxErrorException ) {
			return new SQLGrammarException( message, sqlException, sql );
		}
		else if ( sqlException instanceof SQLTimeoutException ) {
			return new QueryTimeoutException( message, sqlException, sql );
		}
		else if ( sqlException instanceof SQLTransactionRollbackException ) {
			// Not 100% sure this is completely accurate.  The JavaDocs for SQLTransactionRollbackException state that
			// it indicates sql states starting with '40' and that those usually indicate that:
			//		<quote>
			//			the current statement was automatically rolled back by the database because of deadlock or
			// 			other transaction serialization failures.
			//		</quote>
			return new LockAcquisitionException( message, sqlException, sql );
		}

		return null; // allow other delegates the chance to look
	}
}
