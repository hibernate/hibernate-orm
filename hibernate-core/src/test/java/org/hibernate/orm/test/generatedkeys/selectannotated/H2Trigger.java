/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.generatedkeys.selectannotated;

import org.h2.tools.TriggerAdapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class H2Trigger extends TriggerAdapter {
	@Override
	public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
		ResultSet resultSet = conn.createStatement().executeQuery("select coalesce(max(id), 0) from my_entity");
		resultSet.next();
		newRow.updateInt( "id", resultSet.getInt(1) + 1 );
	}
}
