//$Id: $
package org.hibernate.annotations.common.test.reflection.java.generics.deep;

import java.util.List;

import junit.framework.TestCase;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;


/**
 * @author Paolo Perrotta
 */
public class DeepGenericsContainment extends TestCase {

	public static class Contained<T> {
	    T generic;
	}
	
	public static class Container {
		Contained<String> contained;
	}
	
	public static class ContainerWithCollection {
		List<Contained<String>> contained;
	}

	public void test2StepsGenerics() throws Exception {
		JavaReflectionManager factory = new JavaReflectionManager();
		XClass container = factory.toXClass( Container.class );
		XProperty contained = container.getDeclaredProperties( XClass.ACCESS_FIELD ).get( 0 );
		assertTrue( contained.isTypeResolved() );
		XProperty generic = contained.getType().getDeclaredProperties( XClass.ACCESS_FIELD ).get( 0 );
		assertTrue( generic.isTypeResolved() );
	}

	public void test2StepsGenericsCollection() throws Exception {
		JavaReflectionManager factory = new JavaReflectionManager();
		XClass container = factory.toXClass( ContainerWithCollection.class );
		XProperty collection = container.getDeclaredProperties( XClass.ACCESS_FIELD ).get( 0 );
		assertTrue( collection.isTypeResolved() );
		XClass elementClass = collection.getElementClass();
		XProperty generic = elementClass.getDeclaredProperties( XClass.ACCESS_FIELD ).get( 0 );
		assertTrue( generic.isTypeResolved() );
	}
}
