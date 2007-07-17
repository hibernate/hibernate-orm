package org.hibernate.test.bytecode.cglib;

import junit.framework.TestSuite;

import org.hibernate.bytecode.ReflectionOptimizer;
import org.hibernate.bytecode.cglib.BytecodeProviderImpl;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.test.bytecode.Bean;
import org.hibernate.test.bytecode.BeanReflectionHelper;

/**
 * @author Steve Ebersole
 */
public class ReflectionOptimizerTest extends UnitTestCase {

	public ReflectionOptimizerTest(String string) {
		super( string );
	}

	public void testReflectionOptimization() {
		BytecodeProviderImpl provider = new BytecodeProviderImpl();
		ReflectionOptimizer optimizer = provider.getReflectionOptimizer(
				Bean.class,
		        BeanReflectionHelper.getGetterNames(),
		        BeanReflectionHelper.getSetterNames(),
		        BeanReflectionHelper.getTypes()
		);
		assertNotNull( optimizer );
		assertNotNull( optimizer.getInstantiationOptimizer() );
		assertNotNull( optimizer.getAccessOptimizer() );

		Object instance = optimizer.getInstantiationOptimizer().newInstance();
		assertEquals( instance.getClass(), Bean.class );
		Bean bean = ( Bean ) instance;

		optimizer.getAccessOptimizer().setPropertyValues( bean, BeanReflectionHelper.TEST_VALUES );
		assertEquals( bean.getSomeString(), BeanReflectionHelper.TEST_VALUES[0] );
		Object[] values = optimizer.getAccessOptimizer().getPropertyValues( bean );
		assertEquivalent( values, BeanReflectionHelper.TEST_VALUES );
	}

	private void assertEquivalent(Object[] checkValues, Object[] values) {
		assertEquals( "Different lengths", checkValues.length, values.length );
		for ( int i = 0; i < checkValues.length; i++ ) {
			assertEquals( "different values at index [" + i + "]", checkValues[i], values[i] );
		}
	}

	public static TestSuite suite() {
		return new TestSuite( ReflectionOptimizerTest.class );
	}
}
