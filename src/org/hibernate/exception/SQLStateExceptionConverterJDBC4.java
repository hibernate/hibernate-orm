// $Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;

/**
 * A SQLExceptionConverter implementation which performs converion based on
 * the underlying SQLState. Interpretation of a SQL error based on SQLState
 * is not nearly as accurate as using the ErrorCode (which is, however, vendor-
 * specific).  Use of a ErrorCode-based converter should be preferred approach
 * for converting/interpreting SQLExceptions.
 *
 * @author Gail Badner
 */
public class SQLStateExceptionConverterJDBC4 extends SQLStateConverter {

	public SQLStateExceptionConverterJDBC4(ViolatedConstraintNameExtracter extracter) {
		super( extracter );
	}

	/**
	 * Convert the given SQLException into Hibernate's JDBCException hierarchy.
	 *
	 * @param sqlException The SQLException to be converted.
	 * @param message      An optional error message.
	 * @param sql          Optionally, the sql being performed when the exception occurred.
	 * @return The resulting JDBCException.
	 */
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		JDBCException jdbcException = super.convert( sqlException, message, sql );
		if ( !( jdbcException instanceof GenericJDBCException) ) {
			return jdbcException;
		}

		try {
			if ( Class.forName( "java.sql.SQLIntegrityConstraintViolationException" ).isInstance( sqlException ) ) {
				String constraintName = getViolatedConstraintName( sqlException );
				return new ConstraintViolationException( message, sqlException, sql, constraintName );
			}
			else if ( Class.forName( "java.sql.SQLTransactionRollbackException" ).isInstance( sqlException ) ) {
				return new TransactionRollbackException( message, sqlException, sql );
			}
			else if ( Class.forName( "java.sql.SQLClientInfoException" ).isInstance( sqlException ) ||
					Class.forName( "java.sql.SQLInvalidAuthorizationSpecException" ).isInstance( sqlException ) ||
					Class.forName( "java.sql.SQLNonTransientConnectionException" ).isInstance( sqlException ) ||
					Class.forName( "java.sql.SQLTransientConnectionException" ).isInstance( sqlException ) ) {
				return new JDBCConnectionException( message, sqlException, sql );
			}
			else if ( Class.forName( "java.sql.SQLSyntaxErrorException" ).isInstance( sqlException ) ) {
				return new SQLGrammarException( message, sqlException, sql );
			}
			else if ( Class.forName( "java.sql.SQLDataException" ).isInstance( sqlException ) ||
					Class.forName( "javax.sql.rowset.serial.SerialException" ).isInstance( sqlException ) ) {
				return new DataException( message, sqlException, sql );
			}
		}
		catch ( ClassNotFoundException e ) {
			// log because either config is messed up or there's a typo in a class name
		}
		return jdbcException;
	}
}