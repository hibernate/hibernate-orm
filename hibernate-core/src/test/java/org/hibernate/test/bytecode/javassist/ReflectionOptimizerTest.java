/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.javassist;

import org.junit.Test;

import org.hibernate.bytecode.internal.javassist.BulkAccessor;
import org.hibernate.bytecode.internal.javassist.BytecodeProviderImpl;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.test.bytecode.Bean;
import org.hibernate.test.bytecode.BeanReflectionHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class ReflectionOptimizerTest extends BaseUnitTestCase {
	@Test
	public void testBulkAccessorDirectly() {
		BulkAccessor bulkAccessor = BulkAccessor.create(
				Bean.class,
				BeanReflectionHelper.getGetterNames(),
				BeanReflectionHelper.getSetterNames(),
				BeanReflectionHelper.getTypes()
		);
	}

	@Test
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
}
