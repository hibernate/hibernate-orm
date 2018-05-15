/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.annotations.Remove;

/**
 * Performs parameter value binding to a JDBC PreparedStatement.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
public interface JdbcParameterBinder {
	// todo (6.0) : remove the return value here.  Like below, is always 1
	int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			ExecutionContext executionContext) throws SQLException;

	/**
	 * @deprecated A parameter at the JDBC level is always just a "span of 1"
	 */
	@Remove
	@Deprecated
	default int getNumberOfJdbcParametersNeeded() {
		return 1;
	}
}
