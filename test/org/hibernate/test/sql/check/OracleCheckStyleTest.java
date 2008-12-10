package org.hibernate.test.sql.check;

import junit.framework.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * todo: describe OracleCheckStyleTest
 *
 * @author Steve Ebersole
 */
public class OracleCheckStyleTest extends ResultCheckStyleTest {
	public OracleCheckStyleTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "sql/check/oracle-mappings.hbm.xml" };
	}

	public boolean appliesTo(Dialect dialect) {
		return dialect instanceof Oracle9iDialect;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OracleCheckStyleTest.class );
	}

}
