//$Id: MySQLCustomSQLTest.java 11486 2007-05-08 21:57:24Z steve.ebersole@jboss.com $
package org.hibernate.test.sql.hand.custom.mysql;

import junit.framework.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;

/**
 * Custom SQL tests for MySQL
 *
 * @author Gavin King
 */
public class MySQLCustomSQLTest extends CustomStoredProcTestSupport {

	public MySQLCustomSQLTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "sql/hand/custom/mysql/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MySQLCustomSQLTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		return ( dialect instanceof MySQLDialect );
	}
}

