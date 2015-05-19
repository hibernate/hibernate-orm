/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.spi;

import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * Allow a {@link SQLExceptionConverter} to work by chaining together multiple such delegates.  The main
 * difference between a delegate and a full-fledged converter is that a delegate may return {@code null}.
 *
 * @author Steve Ebersole
 */
public interface SQLExceptionConversionDelegate {
	/**
	 * Convert the given SQLException into the Hibernate {@link org.hibernate.JDBCException} hierarchy.
	 *
	 * @param sqlException The SQLException to be converted.
	 * @param message An (optional) error message.
	 * @param sql The {@literal SQL} statement, if one, being performed when the exception occurred.
	 *
	 * @return The resulting JDBCException, can be {@code null}
	 */
	public JDBCException convert(SQLException sqlException, String message, String sql);

}
