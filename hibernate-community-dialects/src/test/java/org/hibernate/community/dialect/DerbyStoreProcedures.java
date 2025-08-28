/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Andrea Boriero
 */
public class DerbyStoreProcedures {

	public static void selectAllEmployments(ResultSet[] resultSets) throws SQLException {
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );

		PreparedStatement statement = conn.prepareStatement(
				"select EMPLOYEE, EMPLOYER, STARTDATE, ENDDATE," +
						" REGIONCODE, EMPID, \"VALUE\", CURRENCY" +
						" FROM EMPLOYMENT"
		);
		resultSets[0] = statement.executeQuery();

		conn.close();
	}

	public static void paramHandling(short j, short i, ResultSet[] resultSets) throws SQLException {
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );

		PreparedStatement statement = conn.prepareStatement( "SELECT " + j + " as value, " + i + "  as value2 from sysibm.sysdummy1" );
		resultSets[0] = statement.executeQuery();

		conn.close();
	}

	public static void simpleScalar(short i, ResultSet[] resultSets) throws SQLException {
		Connection conn = DriverManager.getConnection( "jdbc:default:connection" );

		PreparedStatement statement = conn.prepareStatement(
				"SELECT " + i + " as value, 'getAll' as name from sysibm.sysdummy1"
		);
		resultSets[0] = statement.executeQuery();

		conn.close();
	}
}
