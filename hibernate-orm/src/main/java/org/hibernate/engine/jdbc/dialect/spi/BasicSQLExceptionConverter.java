/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.spi;

import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.exception.internal.SQLStateConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * A helper to centralize conversion of {@link java.sql.SQLException}s to {@link org.hibernate.JDBCException}s.
 * <p/>
 * Used while querying JDBC metadata during bootstrapping
 *
 * @author Steve Ebersole
 */
public class BasicSQLExceptionConverter {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BasicSQLExceptionConverter.class );

	/**
	 * Singleton access
	 */
	public static final BasicSQLExceptionConverter INSTANCE = new BasicSQLExceptionConverter();

	/**
	 * Message
	 */
	public static final String MSG = LOG.unableToQueryDatabaseMetadata();

	private static final SQLStateConverter CONVERTER = new SQLStateConverter( new ConstraintNameExtracter() );

	/**
	 * Perform a conversion.
	 *
	 * @param sqlException The exception to convert.
	 * @return The converted exception.
	 */
	public JDBCException convert(SQLException sqlException) {
		return CONVERTER.convert( sqlException, MSG, null );
	}

	private static class ConstraintNameExtracter implements ViolatedConstraintNameExtracter {
		@Override
		public String extractConstraintName(SQLException sqle) {
			return "???";
		}
	}
}
