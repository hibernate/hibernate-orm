/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.hibernate.JDBCException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.SQLExceptionConverter;

/**
 * @author Steve Ebersole
 */
public class StandardSQLExceptionConverter implements SQLExceptionConverter {
	private ArrayList<SQLExceptionConversionDelegate> delegates = new ArrayList<SQLExceptionConversionDelegate>();

	public StandardSQLExceptionConverter() {
	}

	public StandardSQLExceptionConverter(SQLExceptionConversionDelegate... delegates) {
		if ( delegates != null ) {
			this.delegates.addAll( Arrays.asList( delegates ) );
		}
	}

	public void addDelegate(SQLExceptionConversionDelegate delegate) {
		if ( delegate != null ) {
			this.delegates.add( delegate );
		}
	}

	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		for ( SQLExceptionConversionDelegate delegate : delegates ) {
			final JDBCException jdbcException = delegate.convert( sqlException, message, sql );
			if ( jdbcException != null ) {
				return jdbcException;
			}
		}
		return new GenericJDBCException( message, sqlException, sql );
	}
}
