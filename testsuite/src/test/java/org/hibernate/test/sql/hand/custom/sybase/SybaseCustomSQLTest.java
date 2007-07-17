//$Id$
package org.hibernate.test.sql.hand.custom.sybase;

import junit.framework.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.sql.hand.custom.CustomStoredProcTestSupport;

/**
 * Custom SQL tests for Sybase/SQLServer (Transact-SQL)
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
		return ( dialect instanceof SybaseDialect );
	}
}

