//$Id$
package org.hibernate.test.sql.hand.custom.sybase;

import junit.framework.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;

/**
 * Custom SQL tests for Sybase dialects
 * 
 * @author Gavin King
 */
public class SybaseCustomSQLTest extends CustomStoredProcTestSupport {

	public SybaseCustomSQLTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] { "sql/hand/custom/sybase/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( SybaseCustomSQLTest.class );
	}

	public boolean appliesTo(Dialect dialect) {
		return ( dialect instanceof SybaseDialect ||
				dialect instanceof SybaseASE15Dialect ||
				dialect instanceof Sybase11Dialect ||
				dialect instanceof SybaseAnywhereDialect );
	}
}

