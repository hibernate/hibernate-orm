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

/**
 * {@link org.hibernate.exception.spi.SQLExceptionConverter} implementation that does conversion based on the
 * JDBC 4 defined {@link SQLException} sub-type hierarchy.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionTypeDelegate extends AbstractSQLExceptionConversionDelegate {
	public SQLExceptionTypeDelegate(ConversionContext conversionContext) {
		super( conversionContext );
	}

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		if ( SQLClientInfoException.class.isInstance( sqlException )
				|| SQLInvalidAuthorizationSpecException.class.isInstance( sqlException )
				|| SQLNonTransientConnectionException.class.isInstance( sqlException )
				|| SQLTransientConnectionException.class.isInstance( sqlException ) ) {
			return new JDBCConnectionException( message, sqlException, sql );
		}
		else if ( DataTruncation.class.isInstance( sqlException ) ||
				SQLDataException.class.isInstance( sqlException ) ) {
			throw new DataException( message, sqlException, sql );
		}
		else if ( SQLIntegrityConstraintViolationException.class.isInstance( sqlException ) ) {
			return new ConstraintViolationException(
					message,
					sqlException,
					sql,
					getConversionContext().getViolatedConstraintNameExtracter().extractConstraintName( sqlException )
			);
		}
		else if ( SQLSyntaxErrorException.class.isInstance( sqlException ) ) {
			return new SQLGrammarException( message, sqlException, sql );
		}
		else if ( SQLTimeoutException.class.isInstance( sqlException ) ) {
			return new QueryTimeoutException( message, sqlException, sql );
		}
		else if ( SQLTransactionRollbackException.class.isInstance( sqlException ) ) {
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
