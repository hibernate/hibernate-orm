/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.spi;

import java.io.Serializable;
import java.sql.SQLException;

import org.hibernate.JDBCException;

/**
 * Defines a contract for implementations that know how to convert a SQLException
 * into Hibernate's JDBCException hierarchy.  Inspired by Spring's
 * SQLExceptionTranslator.
 * <p/>
 * Implementations <b>must</b> have a constructor which takes a
 * {@link ViolatedConstraintNameExtracter} parameter.
 * <p/>
 * Implementations may implement {@link org.hibernate.exception.spi.Configurable} if they need to perform
 * configuration steps prior to first use.
 *
 * @author Steve Ebersole
 * @see SQLExceptionConverterFactory
 */
public interface SQLExceptionConverter extends Serializable {
	/**
	 * Convert the given SQLException into the Hibernate {@link JDBCException} hierarchy.
	 *
	 * @param sqlException The SQLException to be converted.
	 * @param message      An optional error message.
	 * @return The resulting JDBCException.
	 * @see org.hibernate.exception.ConstraintViolationException , JDBCConnectionException, SQLGrammarException, LockAcquisitionException
	 */
	public JDBCException convert(SQLException sqlException, String message, String sql);
}
