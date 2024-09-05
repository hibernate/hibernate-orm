/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.exception.GenericJDBCException;

/**
 * @author Andrea Boriero
 */
public class SequenceValueExtractor {

	private final Dialect dialect;
	private final String queryString;

	public SequenceValueExtractor(Dialect dialect, String sequenceName) {
		this.dialect = dialect;
		this.queryString = dialect.getSequenceSupport().getSequencePreviousValString( sequenceName );
	}

	public long extractSequenceValue(final SessionImplementor sessionImpl) {
		final PreparedStatement query = sessionImpl.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( queryString );
		try ( final ResultSet resultSet = sessionImpl.getJdbcCoordinator().getResultSetReturn().extract( query, queryString ) ) {
			resultSet.next();
			long value = resultSet.getLong( 1 );
			if ( dialect instanceof DerbyDialect ) {
				value--;
			}
			return value;
		}
		catch (SQLException e) {
			throw new GenericJDBCException( "Couldn't extract value", e );
		}
	}
}
