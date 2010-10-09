package org.hibernate.test.instrument.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.bytecode.cglib.BytecodeProviderImpl;

/**
 * @author Steve Ebersole
 */
public class CGLIBInstrumentationTest extends AbstractTransformingClassLoaderInstrumentTestCase {
	public CGLIBInstrumentationTest(String string) {
		super( string );
	}

	protected BytecodeProvider buildBytecodeProvider() {
		return new BytecodeProviderImpl();
	}

	public static Test suite() {
		return new TestSuite( CGLIBInstrumentationTest.class );
	}

	public void testSetFieldInterceptor() {
		super.testSetFieldInterceptor();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public void testDirtyCheck() {
		super.testDirtyCheck();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public void testFetchAll() throws Exception {
		super.testFetchAll();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public void testLazy() {
		super.testLazy();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public void testLazyManyToOne() {
		super.testLazyManyToOne();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public void testPropertyInitialized() {
		super.testPropertyInitialized();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public void testManyToOneProxy() {
		super.testManyToOneProxy();    //To change body of overridden methods use File | Settings | File Templates.
	}

	public void testSharedPKOneToOne() {
		super.testSharedPKOneToOne();
	}
	
	public void testCustomColumnReadAndWrite() {
		super.testCustomColumnReadAndWrite();
	}	

}
