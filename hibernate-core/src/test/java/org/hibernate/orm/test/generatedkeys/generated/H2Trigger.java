/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.generatedkeys.generated;

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
