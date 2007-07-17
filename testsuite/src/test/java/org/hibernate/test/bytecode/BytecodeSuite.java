package org.hibernate.test.bytecode;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * todo: describe BytecodeSuite
 *
 * @author Steve Ebersole
 */
public class BytecodeSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "BytecodeProvider tests" );
		suite.addTest( org.hibernate.test.bytecode.cglib.ReflectionOptimizerTest.suite() );
		suite.addTest( org.hibernate.test.bytecode.cglib.InvocationTargetExceptionTest.suite() );
		suite.addTest( org.hibernate.test.bytecode.cglib.CGLIBThreadLocalTest.suite() );
		suite.addTest( org.hibernate.test.bytecode.javassist.ReflectionOptimizerTest.suite() );
		suite.addTest( org.hibernate.test.bytecode.javassist.InvocationTargetExceptionTest.suite() );
		return suite;
	}
}
