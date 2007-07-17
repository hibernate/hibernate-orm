//$Id$
package org.hibernate.test.sql.hand.custom.db2;

import junit.framework.Test;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;

/**
 * Custom SQL tests for DB2
 * 
 * @author Max Rydahl Andersen
 */
public class DB2CustomSQLTest extends CustomStoredProcTestSupport {
	
	public DB2CustomSQLTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "sql/hand/custom/db2/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( DB2CustomSQLTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		return ( dialect instanceof DB2Dialect);
	}

}

