// $Id: TimestampGeneratedValuesWithCachingTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.generated;

import junit.framework.Test;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Implementation of TimestampGeneratedValuesWithCachingTest.
 *
 * @author Steve Ebersole
 */
public class TimestampGeneratedValuesWithCachingTest extends AbstractGeneratedPropertyTest {

	public TimestampGeneratedValuesWithCachingTest(String x) {
		super( x );
	}

	public final String[] getMappings() {
		return new String[] { "generated/MSSQLGeneratedPropertyEntity.hbm.xml" };
	}

	public boolean appliesTo(Dialect dialect) {
		// this test is specific to Sybase/SQLServer as it is testing support
		// for their TIMESTAMP datatype...
		return ( dialect instanceof SybaseDialect || dialect instanceof Sybase11Dialect ||
			dialect instanceof SybaseAnywhereDialect || dialect instanceof SybaseASE15Dialect ||
			dialect instanceof SQLServerDialect);
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( TimestampGeneratedValuesWithCachingTest.class );
	}
}
