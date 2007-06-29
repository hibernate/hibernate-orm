package org.hibernate.test.sql;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.sql.check.CheckSuite;
import org.hibernate.test.sql.hand.custom.datadirect.oracle.DataDirectOracleCustomSQLTest;
import org.hibernate.test.sql.hand.custom.db2.DB2CustomSQLTest;
import org.hibernate.test.sql.hand.custom.mysql.MySQLCustomSQLTest;
import org.hibernate.test.sql.hand.custom.oracle.OracleCustomSQLTest;
import org.hibernate.test.sql.hand.custom.sybase.SybaseCustomSQLTest;
import org.hibernate.test.sql.hand.query.NativeSQLQueriesTest;
import org.hibernate.test.sql.hand.identity.CustomInsertSQLWithIdentityColumnTest;

/**
 * Suite for testing aspects of native SQL support.
 *
 * @author Steve Ebersole
 */
public class NativeSqlSupportSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "Native SQL support tests" );
		suite.addTest( DB2CustomSQLTest.suite() );
		suite.addTest( DataDirectOracleCustomSQLTest.suite() );
		suite.addTest( OracleCustomSQLTest.suite() );
		suite.addTest( SybaseCustomSQLTest.suite() );
		suite.addTest( MySQLCustomSQLTest.suite() );
		suite.addTest( NativeSQLQueriesTest.suite() );
		suite.addTest( CheckSuite.suite() );
		suite.addTest( CustomInsertSQLWithIdentityColumnTest.suite() );
		return suite;
	}
}
