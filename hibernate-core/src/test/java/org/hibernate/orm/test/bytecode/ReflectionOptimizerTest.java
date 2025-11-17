/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode;

import static org.hibernate.bytecode.internal.BytecodeProviderInitiator.buildDefaultBytecodeProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class ReflectionOptimizerTest extends BaseUnitTestCase {

	private static BytecodeProvider provider;

	@BeforeClass
	public static void initBytecodeProvider() {
		provider = buildDefaultBytecodeProvider();
	}

	@AfterClass
	public static void clearBytecodeProvider() {
		if ( provider != null ) {
			provider.resetCaches();
			provider = null;
		}
	}

	@Test
	public void testReflectionOptimization() {
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

	@Test
	@JiraKey(value = "HHH-12584")
	public void testAbstractClass() {
		ReflectionOptimizer reflectionOptimizer = provider.getReflectionOptimizer( AbstractClass.class, new String[]{ "getProperty" },
			new String[]{ "setProperty" }, new Class[]{ String.class } );
		assertNotNull( reflectionOptimizer );
	}

	@Test
	@JiraKey(value = "HHH-12584")
	public void testInterface() {
		ReflectionOptimizer reflectionOptimizer = provider.getReflectionOptimizer( Interface.class, new String[]{ "getProperty" },
			new String[]{ "setProperty" }, new Class[]{ String.class } );
		assertNotNull( reflectionOptimizer );
	}

	private void assertEquivalent(Object[] checkValues, Object[] values) {
		assertEquals( "Different lengths", checkValues.length, values.length );
		for ( int i = 0; i < checkValues.length; i++ ) {
			assertEquals( "different values at index [" + i + "]", checkValues[i], values[i] );
		}
	}

	public static abstract class AbstractClass {

		private String property;

		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}
	}

	public interface Interface {

		String getProperty();

		void setProperty(String property);
	}

}
