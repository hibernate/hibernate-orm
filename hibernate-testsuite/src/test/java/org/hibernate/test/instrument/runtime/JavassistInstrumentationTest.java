//$Id: $
package org.hibernate.test.instrument.runtime;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.bytecode.BytecodeProvider;
import org.hibernate.bytecode.javassist.BytecodeProviderImpl;

/**
 * @author Steve Ebersole
 */
public class JavassistInstrumentationTest extends AbstractTransformingClassLoaderInstrumentTestCase {
	public JavassistInstrumentationTest(String string) {
		super( string );
	}

	protected BytecodeProvider buildBytecodeProvider() {
		return new BytecodeProviderImpl();
	}

	public static Test suite() {
		return new TestSuite( JavassistInstrumentationTest.class );
	}

	public void testSetFieldInterceptor() {
		super.testSetFieldInterceptor();
	}

	public void testDirtyCheck() {
		super.testDirtyCheck();
	}

	public void testFetchAll() throws Exception {
		super.testFetchAll();
	}

	public void testLazy() {
		super.testLazy();
	}

	public void testLazyManyToOne() {
		super.testLazyManyToOne();
	}

	public void testPropertyInitialized() {
		super.testPropertyInitialized();
	}

	public void testManyToOneProxy() {
		super.testManyToOneProxy();
	}

	public void testSharedPKOneToOne() {
		super.testSharedPKOneToOne();
	}

	public void testCustomColumnReadAndWrite() {
		super.testCustomColumnReadAndWrite();
	}	
	
}
