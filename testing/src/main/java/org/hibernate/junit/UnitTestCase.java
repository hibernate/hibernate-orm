/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.junit;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import junit.framework.AssertionFailedError;


/**
 * A basic JUnit {@link junit.framework.TestCase} subclass for
 * adding some Hibernate specific behavior and functionality.
 *
 * @author Steve Ebersole
 */
public abstract class UnitTestCase extends junit.framework.TestCase {

	private static final Logger log = LoggerFactory.getLogger( UnitTestCase.class );

	public UnitTestCase(String string) {
		super( string );
	}

	/**
	 * runBare overridden in order to apply FailureExpected validations
	 * as well as start/complete logging
	 *
	 * @throws Throwable
	 */
	public void runBare() throws Throwable {
		final boolean doValidate = getName().endsWith( "FailureExpected" ) && Boolean.getBoolean( "hibernate.test.validatefailureexpected" );
		try {
			log.info( "Starting test [" + fullTestName() + "]" );
			super.runBare();
			if ( doValidate ) {
				fail( "Test marked as FailureExpected, but did not fail!" );
			}
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
			log.info( "Completed test [" + fullTestName() + "]" );
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
		SkipLog.LOG.warn( "*** skipping [" + fullTestName() + "] - " + testDescription + " : " + reason, new Exception()  );
	}
}
