package org.hibernate.annotations.common.test.annotationfactory;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationProxy;

/**
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
public class AnnotationProxyTest extends TestCase {

	private AnnotationProxy ann;
	private AnnotationDescriptor descriptor;

	public void setUp() {
		descriptor = new AnnotationDescriptor( TestAnnotation.class );
		descriptor.setValue( "stringElement", "x" );
		descriptor.setValue( "booleanElement", false );
		descriptor.setValue( "someOtherElement", "y" );
		ann = new AnnotationProxy( descriptor );
	}

	public void testConstructionFailsIfYouDoNotAssignValuesToAllTheElementsWithoutADefault() {
		try {
			AnnotationDescriptor desc = new AnnotationDescriptor( TestAnnotation.class );
			desc.setValue( "stringElement", "x" );
			desc.setValue( "booleanElement", false );
			new AnnotationProxy( desc );
			fail();
		}
		catch (Exception e) {
			assertEquals( "No value provided for someOtherElement", e.getMessage() );
		}
	}

	public void testConstructionFailsIfYouDefineElementsThatAreNotInTheAnnotationInterface() {
		try {
			AnnotationDescriptor desc = new AnnotationDescriptor( Deprecated.class );
			desc.setValue( "wrongElement", "xxx" );
			new AnnotationProxy( desc );
			fail();
		}
		catch (Exception e) {
			assertTrue( e.getMessage().contains( "unknown elements" ) );
		}
	}

	public void testSupportsGenericCallsToAllElements() throws Throwable {
		assertEquals( "x", invoke( ann, "stringElement" ) );
		assertFalse( (Boolean) invoke( ann, "booleanElement" ) );
	}

	public void testPretendsThatItHasTheGivenType() {
		assertSame( TestAnnotation.class, ann.annotationType() );
	}

	public void testItsToStringConformsToTheJavaAnnotationDocumentation() throws Throwable {
		String expectedString = "@org.hibernate.annotations.common.test.annotationfactory.TestAnnotation(booleanElement=false, elementWithDefault=abc, someOtherElement=y, stringElement=x)";
		assertEquals( expectedString, invoke( ann, "toString" ) );
	}

	public void testSupportsGenericCallsToMethods() throws Throwable {
		assertEquals( ann.annotationType(), invoke( ann, "annotationType" ) );
		assertEquals( ann.toString(), invoke( ann, "toString" ) );
	}

	public void testThrowsARuntimeExceptionIfYouUseAnElementWhichIsNotInTheAnnotationInterface() {
		AnnotationDescriptor elements = new AnnotationDescriptor( TestAnnotation.class );
		elements.setValue( "anOddElement", "x" );
		try {
			new AnnotationProxy( elements );
			fail();
		}
		catch (RuntimeException e) {
		}
	}

	public void testUsesTheDefaultValueForUndefinedElementsWhenAvailable() throws Throwable {
		assertEquals( "abc", invoke( ann, "elementWithDefault" ) );
	}

	public void testThrowsANoSuchMethodExceptionWhenAccessingAnUndefinedMethod() throws Throwable {
		try {
			invoke( ann, "anElementThatDoesNotExist" );
			fail();
		}
		catch (NoSuchMethodException e) {
		}
		try {
			invoke( ann, "anOddElement", "arg1", "arg2" );
			fail();
		}
		catch (NoSuchMethodException e) {
		}
	}

	private Object invoke(AnnotationProxy proxy, String methodName, Object... args) throws Throwable {
		Class[] parameterTypes = new Class[args.length];
		for ( int i = 0; i < args.length ; i++ ) {
			parameterTypes[i] = args[i].getClass();
		}
		Method method = TestAnnotation.class.getMethod( methodName, parameterTypes );
		return proxy.invoke( proxy, method, parameterTypes );
	}
}
