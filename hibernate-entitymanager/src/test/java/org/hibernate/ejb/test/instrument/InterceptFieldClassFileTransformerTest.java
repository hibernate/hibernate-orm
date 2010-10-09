//$Id$
package org.hibernate.ejb.test.instrument;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class InterceptFieldClassFileTransformerTest extends TestCase {
	/**
	 * Tests that class file enhancement works.
	 * 
	 * @throws Exception in case the test fails.
	 */
	public void testEnhancement() throws Exception {
		List<String> entities = new ArrayList<String>();
		entities.add( "org.hibernate.ejb.test.instrument.Simple" );

		// sanity check that the class is unmodified and does not contain getFieldHandler()
		try {
			org.hibernate.ejb.test.instrument.Simple.class.getDeclaredMethod( "getFieldHandler" );
			fail();
		} catch ( NoSuchMethodException nsme ) {
			// success
		}

		// use custom class loader which enhances the class
		InstrumentedClassLoader cl = new InstrumentedClassLoader( Thread.currentThread().getContextClassLoader() );
		cl.setEntities( entities );
		Class clazz = cl.loadClass( entities.get( 0 ) );
		
		// javassist is our default byte code enhancer. Enhancing will eg add the method getFieldHandler()
		// see org.hibernate.bytecode.javassist.FieldTransformer
		Method method = clazz.getDeclaredMethod( "getFieldHandler" );
		assertNotNull( method );
	}
}
