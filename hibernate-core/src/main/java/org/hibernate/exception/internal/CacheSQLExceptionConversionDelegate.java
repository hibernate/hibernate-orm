/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.exception.internal;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.JDBCException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.spi.AbstractSQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ConversionContext;
import org.hibernate.internal.util.JdbcExceptionHelper;

/**
 * A {@link org.hibernate.exception.spi.SQLExceptionConversionDelegate}
 * implementation specific to Cach&eacute; SQL, accounting for its custom
 * integrity constraint violation error codes.
 *
 * @author Jonathan Levinson
 */
public class CacheSQLExceptionConversionDelegate extends AbstractSQLExceptionConversionDelegate {

	private static final Set<String> DATA_CATEGORIES = new HashSet<String>();
	private static final Set<Integer> INTEGRITY_VIOLATION_CATEGORIES = new HashSet<Integer>();

	static {
		DATA_CATEGORIES.add( "22" );
		DATA_CATEGORIES.add( "21" );
		DATA_CATEGORIES.add( "02" );

		INTEGRITY_VIOLATION_CATEGORIES.add( 119 );
		INTEGRITY_VIOLATION_CATEGORIES.add( 120 );
		INTEGRITY_VIOLATION_CATEGORIES.add( 121 );
		INTEGRITY_VIOLATION_CATEGORIES.add( 122 );
		INTEGRITY_VIOLATION_CATEGORIES.add( 123 );
		INTEGRITY_VIOLATION_CATEGORIES.add( 124 );
		INTEGRITY_VIOLATION_CATEGORIES.add( 125 );
		INTEGRITY_VIOLATION_CATEGORIES.add( 127 );
	}

	public CacheSQLExceptionConversionDelegate(ConversionContext conversionContext) {
		super( conversionContext );
	}

	/**
	 * Convert the given SQLException into Hibernate's JDBCException hierarchy.
	 *
	 * @param sqlException The SQLException to be converted.
	 * @param message	  An optional error message.
	 * @param sql		  Optionally, the sql being performed when the exception occurred.
	 * @return The resulting JDBCException; returns null if it could not be converted.
	 */
	@Override
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		String sqlStateClassCode = JdbcExceptionHelper.extractSqlStateClassCode( sqlException );
		if ( sqlStateClassCode != null ) {
			Integer errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			if ( INTEGRITY_VIOLATION_CATEGORIES.contains( errorCode ) ) {
				String constraintName =
						getConversionContext()
								.getViolatedConstraintNameExtracter()
								.extractConstraintName( sqlException );
				return new ConstraintViolationException( message, sqlException, sql, constraintName );
			}
			else if ( DATA_CATEGORIES.contains( sqlStateClassCode ) ) {
				return new DataException( message, sqlException, sql );
			}
		}
		return null; // allow other delegates the chance to look
	}
}
