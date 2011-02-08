/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.junit;
import static org.hibernate.TestLogger.LOG;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.ServicesRegistry;
import org.hibernate.test.common.ServiceRegistryHolder;

/**
 * A basic JUnit {@link junit.framework.TestCase} subclass for
 * adding some Hibernate specific behavior and functionality.
 *
 * @author Steve Ebersole
 */
public abstract class UnitTestCase extends junit.framework.TestCase {

	private ServiceRegistryHolder serviceRegistryHolder;

	public UnitTestCase(String string) {
		super( string );
	}

	/**
	 * runBare overridden in order to apply FailureExpected validations
	 * as well as start/complete logging
	 *
	 * @throws Throwable
	 */
	@Override
    public void runBare() throws Throwable {
		final boolean doValidate = getName().endsWith( "FailureExpected" ) && Boolean.getBoolean( "hibernate.test.validatefailureexpected" );
		try {
            LOG.info("Starting test [" + fullTestName() + "]");
			super.runBare();
			if ( doValidate ) {
				throw new FailureExpectedTestPassedException();
			}
		}
		catch ( FailureExpectedTestPassedException t ) {
			throw t;
		}
		catch( Throwable t ) {
			if ( doValidate ) {
				skipExpectedFailure( t );
			}
			else {
				throw t;
			}
		}
		finally {
            LOG.info("Completed test [" + fullTestName() + "]");
		}
	}

	@Override
	protected void tearDown() throws Exception {
		if ( serviceRegistryHolder != null ) {
				serviceRegistryHolder.destroy();
				serviceRegistryHolder = null;
		}
	}

	protected ServicesRegistry getServiceRegistry() {
		if ( serviceRegistryHolder == null ) {
			serviceRegistryHolder = new ServiceRegistryHolder( Environment.getProperties() );
		}
		return serviceRegistryHolder.getServiceRegistry();
 	}

	protected JdbcServices getJdbcServices() {
		return getServiceRegistry().getService( JdbcServices.class );
	}

	protected ConnectionProvider getConnectionProvider() {
		return getJdbcServices().getConnectionProvider();
	}

	private static class FailureExpectedTestPassedException extends Exception {
		public FailureExpectedTestPassedException() {
			super( "Test marked as FailureExpected, but did not fail!" );
		}
	}

	protected void skipExpectedFailure(Throwable error) {
		reportSkip( "ignoring *FailuredExpected methods", "Failed with: " + error.toString() );
	}

	// additional assertions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static void assertElementTypeAssignability(java.util.Collection collection, Class clazz) throws AssertionFailedError {
		Iterator itr = collection.iterator();
		while ( itr.hasNext() ) {
			assertClassAssignability( itr.next().getClass(), clazz );
		}
	}

	public static void assertClassAssignability(Class source, Class target) throws AssertionFailedError {
		if ( !target.isAssignableFrom( source ) ) {
			throw new AssertionFailedError(
			        "Classes were not assignment-compatible : source<" + source.getName() +
			        "> target<" + target.getName() + ">"
			);
		}
	}


	// test skipping ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public String fullTestName() {
		return this.getClass().getName() + "#" + this.getName();
	}

	protected void reportSkip(String reason, String testDescription) {
        LOG.warn("*** skipping [" + fullTestName() + "] - " + testDescription + " : " + reason, new Exception());
	}

	// testsuite utitities ---------------------------------------------------

	/**
	 * Supports easy creation of TestSuites where a subclass' "FailureExpected"
	 * version of a base test is included in the suite, while the base test
	 * is excluded.  E.g. test class FooTestCase includes method testBar(), while test
	 * class SubFooTestCase extends FooTestCase includes method testBarFailureExcluded().
	 * Passing SubFooTestCase.class to this method will return a suite that
	 * does not include testBar().
	 */
	public static TestSuite createFailureExpectedSuite(Class testClass) {

	   TestSuite allTests = new TestSuite(testClass);
       Set failureExpected = new HashSet();
	   Enumeration tests = allTests.tests();
	   while (tests.hasMoreElements()) {
	      Test t = (Test) tests.nextElement();
	      if (t instanceof TestCase) {
	         String name = ((TestCase) t).getName();
	         if (name.endsWith("FailureExpected"))
	            failureExpected.add(name);
	      }
	   }

	   TestSuite result = new TestSuite();
       tests = allTests.tests();
       while (tests.hasMoreElements()) {
          Test t = (Test) tests.nextElement();
          if (t instanceof TestCase) {
             String name = ((TestCase) t).getName();
             if (!failureExpected.contains(name + "FailureExpected")) {
                result.addTest(t);
             }
          }
       }

	   return result;
	}
}
