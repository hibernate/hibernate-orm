// $Id: TriggerGeneratedValuesWithCachingTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.generated;

import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

import junit.framework.Test;

/**
 * Implementation of TriggerGeneratedValuesWithoutCachingTest.
 *
 * @author Steve Ebersole
 */
public class TriggerGeneratedValuesWithCachingTest extends AbstractGeneratedPropertyTest {

	public TriggerGeneratedValuesWithCachingTest(String x) {
		super( x );
	}

	public final String[] getMappings() {
		return new String[] { "generated/GeneratedPropertyEntity.hbm.xml" };
	}

	public boolean appliesTo(Dialect dialect) {
		// currently have only defined triggers for oracle...
		// TODO : add more triggers for dialects which allow mods in triggers...
		return ( dialect instanceof Oracle9iDialect );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( TriggerGeneratedValuesWithCachingTest.class );
	}
}
