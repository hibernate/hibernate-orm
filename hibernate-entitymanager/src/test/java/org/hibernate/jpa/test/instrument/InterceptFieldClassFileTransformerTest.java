//$Id$
package org.hibernate.jpa.test.instrument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.StackMapTable;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Dustin Schultz
 */
public class InterceptFieldClassFileTransformerTest {
	
	private List<String> entities = new ArrayList<String>();
	private InstrumentedClassLoader loader = null;
	
	@Before
	public void setup() {
		entities.add( "org.hibernate.jpa.test.instrument.Simple" );
		// use custom class loader which enhances the class
		InstrumentedClassLoader cl = new InstrumentedClassLoader( Thread.currentThread().getContextClassLoader() );
		cl.setEntities( entities );
		this.loader = cl;
	}
	
	/**
	 * Tests that class file enhancement works.
	 * 
	 * @throws Exception in case the test fails.
	 */
    @Test
	public void testEnhancement() throws Exception {
		// sanity check that the class is unmodified and does not contain getFieldHandler()
		try {
			Simple.class.getDeclaredMethod( "getFieldHandler" );
			Assert.fail();
		} catch ( NoSuchMethodException nsme ) {
			// success
		}
		
		Class clazz = loader.loadClass( entities.get( 0 ) );
		
		// javassist is our default byte code enhancer. Enhancing will eg add the method getFieldHandler()
		// see org.hibernate.bytecode.internal.javassist.FieldTransformer
		Method method = clazz.getDeclaredMethod( "getFieldHandler" );
		Assert.assertNotNull( method );
	}
    
	/**
	 * Tests that methods that were enhanced by javassist have
	 * StackMapTables for java verification. Without these,
	 * java.lang.VerifyError's occur in JDK7.
	 * 
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-7747")
	public void testStackMapTableEnhancment() throws ClassNotFoundException,
			InstantiationException, IllegalAccessException, IOException {
		byte[] classBytes = loader.loadClassBytes(entities.get(0));
		ClassPool classPool = new ClassPool();
		CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(
				classBytes));
		for (CtMethod ctMethod : ctClass.getMethods()) {
			//Only check methods that were added by javassist
			if (ctMethod.getName().startsWith("$javassist_")) {
				AttributeInfo attributeInfo = ctMethod
						.getMethodInfo().getCodeAttribute()
						.getAttribute(StackMapTable.tag);
				Assert.assertNotNull(attributeInfo);
				StackMapTable smt = (StackMapTable)attributeInfo;
				Assert.assertNotNull(smt.get());
			}
		}
	}
}
