package org.hibernate.dialect;

import junit.framework.TestCase;

/**
 * Unit test of the behavior of the SQLServerDialect utility methods
 * 
 * @author Valotasion Yoryos
 * 
 */
public class SQLServerDialectTestCase extends TestCase {

	public void testStripAsStatement() {
		String input = "some_field1 as f1, some_fild2 as f2, _field3 as f3 ";

		assertEquals( "some_field1, some_fild2, _field3", SQLServerDialect.stripAsStatement(input) );
	}
	
	public void testGetSelectFieldsWithoutAs() {
		StringBuilder input = new StringBuilder( "select some_field1 as f12, some_fild2 as f879, _field3 as _f24674_3 from...." );
		String output = SQLServerDialect.getSelectFieldsWithoutAs( input ).toString();

		assertEquals( " some_field1, some_fild2, _field3", output );
	}
	
	
	public void testReplaceDistinctWithGroupBy() {
		StringBuilder input = new StringBuilder( "select distinct f1, f2 as ff, f3 from table where f1 = 5" );
		SQLServerDialect.replaceDistinctWithGroupBy( input );
		
		assertEquals( "select f1, f2 as ff, f3 from table where f1 = 5 group by f1, f2, f3 ", input.toString() );
	}
	
	
	public void testGetLimitString() {
		String input = "select distinct f1 as f53245 from table849752 order by f234, f67 desc"; 
		
		SQLServerDialect sqlDialect = new SQLServerDialect();
		
		assertEquals( "with query as (select row_number() over (order by f234, f67 desc) as __hibernate_row_nr__, f1 as f53245 from table849752  group by f1) select * from query where __hibernate_row_nr__ between 11 and 15", sqlDialect.getLimitString(input, 10, 15).toLowerCase() );
	}
}
