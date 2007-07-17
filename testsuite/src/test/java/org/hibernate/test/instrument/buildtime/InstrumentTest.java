//$Id: InstrumentTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.instrument.buildtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.test.instrument.domain.Document;
import org.hibernate.test.instrument.cases.TestDirtyCheckExecutable;
import org.hibernate.test.instrument.cases.TestFetchAllExecutable;
import org.hibernate.test.instrument.cases.TestLazyExecutable;
import org.hibernate.test.instrument.cases.TestLazyManyToOneExecutable;
import org.hibernate.test.instrument.cases.TestInjectFieldInterceptorExecutable;
import org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable;
import org.hibernate.test.instrument.cases.TestLazyPropertyCustomTypeExecutable;
import org.hibernate.test.instrument.cases.TestManyToOneProxyExecutable;
import org.hibernate.test.instrument.cases.Executable;
import org.hibernate.junit.UnitTestCase;

/**
 * @author Gavin King
 */
public class InstrumentTest extends UnitTestCase {

	public InstrumentTest(String str) {
		super(str);
	}

	public static Test suite() {
		return new TestSuite( InstrumentTest.class );
	}

	public void testDirtyCheck() {
		execute( new TestDirtyCheckExecutable() );
	}

	public void testFetchAll() throws Exception {
		execute( new TestFetchAllExecutable() );
	}

	public void testLazy() throws Exception {
		execute( new TestLazyExecutable() );
	}

	public void testLazyManyToOne() {
		execute( new TestLazyManyToOneExecutable() );
	}

	public void testSetFieldInterceptor() {
		execute( new TestInjectFieldInterceptorExecutable() );
	}

	public void testPropertyInitialized() {
		execute( new TestIsPropertyInitializedExecutable() );
	}

	public void testManyToOneProxy() {
		execute( new TestManyToOneProxyExecutable() );
	}

	public void testLazyPropertyCustomTypeExecutable() {
		execute( new TestLazyPropertyCustomTypeExecutable() );
	}

	private void execute(Executable executable) {
		executable.prepare();
		try {
			executable.execute();
		}
		finally {
			executable.complete();
		}
	}

	protected void runTest() throws Throwable {
		if ( isRunnable() ) {
			super.runTest();
		}
		else {
			reportSkip( "domain classes not instrumented", "build-time instrumentation" );
		}
	}

	public static boolean isRunnable() {
		return FieldInterceptionHelper.isInstrumented( new Document() );
	}
}

