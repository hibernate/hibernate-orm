//$Id: InstrumentTest.java 10976 2006-12-12 23:22:26Z steve.ebersole@jboss.com $
package org.hibernate.test.instrument.buildtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.test.instrument.cases.Executable;
import org.hibernate.test.instrument.cases.TestCustomColumnReadAndWrite;
import org.hibernate.test.instrument.cases.TestDirtyCheckExecutable;
import org.hibernate.test.instrument.cases.TestFetchAllExecutable;
import org.hibernate.test.instrument.cases.TestInjectFieldInterceptorExecutable;
import org.hibernate.test.instrument.cases.TestIsPropertyInitializedExecutable;
import org.hibernate.test.instrument.cases.TestLazyExecutable;
import org.hibernate.test.instrument.cases.TestLazyManyToOneExecutable;
import org.hibernate.test.instrument.cases.TestLazyPropertyCustomTypeExecutable;
import org.hibernate.test.instrument.cases.TestManyToOneProxyExecutable;
import org.hibernate.test.instrument.cases.TestSharedPKOneToOneExecutable;
import org.hibernate.test.instrument.domain.Document;

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

	public void testDirtyCheck() throws Exception {
		execute( new TestDirtyCheckExecutable() );
	}

	public void testFetchAll() throws Exception {
		execute( new TestFetchAllExecutable() );
	}

	public void testLazy() throws Exception {
		execute( new TestLazyExecutable() );
	}

	public void testLazyManyToOne() throws Exception {
		execute( new TestLazyManyToOneExecutable() );
	}

	public void testSetFieldInterceptor() throws Exception {
		execute( new TestInjectFieldInterceptorExecutable() );
	}

	public void testPropertyInitialized() throws Exception {
		execute( new TestIsPropertyInitializedExecutable() );
	}

	public void testManyToOneProxy() throws Exception {
		execute( new TestManyToOneProxyExecutable() );
	}

	public void testLazyPropertyCustomTypeExecutable() throws Exception {
		execute( new TestLazyPropertyCustomTypeExecutable() );
	}

	public void testSharedPKOneToOne() throws Exception {
		execute( new TestSharedPKOneToOneExecutable() );
	}

	public void testCustomColumnReadAndWrite() throws Exception {
		execute( new TestCustomColumnReadAndWrite() );
	}	
	
	private void execute(Executable executable) throws Exception {
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

