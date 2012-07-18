//$Id$
package org.hibernate.jpa.test.instrument;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class InterceptFieldClassFileTransformerTest {
	/**
	 * Tests that class file enhancement works.
	 * 
	 * @throws Exception in case the test fails.
	 */
    @Test
	public void testEnhancement() throws Exception {
		List<String> entities = new ArrayList<String>();
		entities.add( "org.hibernate.jpa.test.instrument.Simple" );

		// sanity check that the class is unmodified and does not contain getFieldHandler()
		try {
			Simple.class.getDeclaredMethod( "getFieldHandler" );
			Assert.fail();
		} catch ( NoSuchMethodException nsme ) {
			// success
		}

		// use custom class loader which enhances the class
		InstrumentedClassLoader cl = new InstrumentedClassLoader( Thread.currentThread().getContextClassLoader() );
		cl.setEntities( entities );
		Class clazz = cl.loadClass( entities.get( 0 ) );
		
		// javassist is our default byte code enhancer. Enhancing will eg add the method getFieldHandler()
		// see org.hibernate.bytecode.internal.javassist.FieldTransformer
		Method method = clazz.getDeclaredMethod( "getFieldHandler" );
		Assert.assertNotNull( method );
	}
}
