//$Id: MySQLCustomSQLTest.java 10977 2006-12-12 17:28:04 -0600 (Tue, 12 Dec 2006) steve.ebersole@jboss.com $
package org.hibernate.test.sql.hand.custom.mysql;

import junit.framework.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
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

