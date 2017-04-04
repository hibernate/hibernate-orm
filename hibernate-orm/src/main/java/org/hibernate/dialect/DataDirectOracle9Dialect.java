/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A Dialect for accessing Oracle through DataDirect driver
 */
@SuppressWarnings("deprecation")
public class DataDirectOracle9Dialect extends Oracle9Dialect {
	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		// This assumes you will want to ignore any update counts
		while (!isResultSet && ps.getUpdateCount() != -1) { 
			isResultSet = ps.getMoreResults();
		}

		return ps.getResultSet();
	}
}
