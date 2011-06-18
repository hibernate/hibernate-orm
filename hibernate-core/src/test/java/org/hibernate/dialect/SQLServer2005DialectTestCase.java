package org.hibernate.dialect;

import junit.framework.TestCase;

/**
 * Unit test of the behavior of the SQLServerDialect utility methods
 * 
 * @author Valotasion Yoryos
 * 
 */
public class SQLServer2005DialectTestCase extends TestCase {

	public void testStripAliases() {
		String input = "some_field1 as f1, some_fild2 as f2, _field3 as f3 ";

		assertEquals( "some_field1, some_fild2, _field3", SQLServer2005Dialect.stripAliases(input) );
	}
	
	public void testGetSelectFieldsWithoutAliases() {
		StringBuilder input = new StringBuilder( "select some_field1 as f12, some_fild2 as f879, _field3 as _f24674_3 from...." );
		String output = SQLServer2005Dialect.getSelectFieldsWithoutAliases( input ).toString();

		assertEquals( " some_field1, some_fild2, _field3", output );
	}
	
	
	public void testReplaceDistinctWithGroupBy() {
		assertReplaceDistinctWithGroupBy( "select distinct f1, f2 as ff, f3 from table where f1 = 5", "select f1, f2 as ff, f3 from table where f1 = 5 group by f1, f2, f3 " );
		
		//http://opensource.atlassian.com/projects/hibernate/browse/HHH-5715 distinct in an aggragate function
		assertReplaceDistinctWithGroupBy( "select count(distinct f1) from table", "select count(distinct f1) from table" );
	}
	
	private static final void assertReplaceDistinctWithGroupBy(String input, String expectedOutput) {
		StringBuilder partialQuery = new StringBuilder( input );
		SQLServer2005Dialect.replaceDistinctWithGroupBy( partialQuery );
		
		assertEquals( expectedOutput, partialQuery.toString() );
	}
	
	public void testGetLimitString() { 
		Dialect sqlDialect = new SQLServer2005Dialect();

		//assertGetLimitString( sqlDialect, 
		//		"sql",
		//		"expected output sql" );
		
		assertGetLimitString( sqlDialect, 
				"select distinct f1 as f53245 from table849752 order by f234, f67 desc",
				"with query as (select row_number() over (order by f234, f67 desc) as __hibernate_row_nr__, f1 as f53245 from table849752  group by f1) select * from query where __hibernate_row_nr__ between ? and ?" );
		
		// http://opensource.atlassian.com/projects/hibernate/browse/HHH-5715 distinct in an aggragate function
		// this case should not happen! Is there a way to get paginated data with an aggregate function?
		// assertGetLimitString( sqlDialect,
		// "select count(distinct p.n) from table849752 p order by f234, f67 desc",
		// "the actual sql that should be used" );
		
	}
	
	private static final void assertGetLimitString(Dialect dialect, String input, String expected) {
		String limitString = dialect.getLimitString( input, 10, 15 ).toLowerCase();
		assertEquals( expected, limitString );
	}
}
