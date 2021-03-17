/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import static org.junit.Assert.assertEquals;

import org.hibernate.dialect.pagination.SybaseASE157LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.junit.Test;

public class SybaseASE157LimitHandlerTest {

	@Test
	public void testLimitHandler() {
		assertEquals( "select * from entity", processSql( "select * from entity", null, null ) );
		assertEquals( "select * from entity", processSql( "select * from entity", 15, null ) );
		assertEquals( "select top 15 * from entity", processSql( "select * from entity", null, 15 ) );
		assertEquals( "select top 18 * from entity", processSql( "select * from entity", 3, 15 ) );
		assertEquals( "SELECT top 18 * FROM entity", processSql( "SELECT * FROM entity", 3, 15 ) );
		assertEquals( "		 select	 top 18 * from entity", processSql( "		 select	 * from entity", 3, 15 ) );
		assertEquals( "selectand", processSql( "selectand", 3, 15 ) );
		assertEquals( "select distinct top 15 id from entity",
				processSql( "select distinct id from entity", null, 15 ) );
		assertEquals( "select distinct top 18 id from entity", processSql( "select distinct id from entity", 3, 15 ) );
		assertEquals( "		 select 	distinct	 top 18 id from entity",
				processSql( "		 select 	distinct	 id from entity", 3, 15 ) );
		assertEquals(
				"WITH employee AS (SELECT * FROM Employees) SELECT * FROM employee WHERE ID < 20 UNION ALL SELECT * FROM employee WHERE Sex = 'M'",
				processSql(
						"WITH employee AS (SELECT * FROM Employees) SELECT * FROM employee WHERE ID < 20 UNION ALL SELECT * FROM employee WHERE Sex = 'M'",
						3, 15 ) );

		assertEquals( "select top 5 * from entity", processSql( "select top 5 * from entity", 3, 15 ) );
		assertEquals( "select distinct top 7 * from entity", processSql( "select distinct top 7 * from entity", 3, 15 ) );
		assertEquals( "select distinct top 18 top_column from entity", processSql( "select distinct top_column from entity", 3, 15 ) );
	}

	private String processSql(String sql, Integer offset, Integer limit) {
		RowSelection rowSelection = new RowSelection();
		if ( offset != null ) {
			rowSelection.setFirstRow( offset );
		}
		if (limit != null) {
			rowSelection.setMaxRows( limit );
		}

		SybaseASE157LimitHandler limitHandler = new SybaseASE157LimitHandler();
		return limitHandler.processSql( sql, rowSelection );
	}
}
