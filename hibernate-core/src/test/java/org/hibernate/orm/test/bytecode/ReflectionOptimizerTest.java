/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode;

import static org.hibernate.bytecode.internal.BytecodeProviderInitiator.buildDefaultBytecodeProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ReflectionOptimizer;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class ReflectionOptimizerTest {

	private static BytecodeProvider provider;

	@BeforeAll
	public static void initBytecodeProvider() {
		provider = buildDefaultBytecodeProvider();
	}

	@AfterAll
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
		assertEquals( Bean.class, instance.getClass() );
		Bean bean = (Bean) instance;

		optimizer.getAccessOptimizer().setPropertyValues( bean, BeanReflectionHelper.TEST_VALUES );
		assertEquals( BeanReflectionHelper.TEST_VALUES[0], bean.getSomeString() );
		Object[] values = optimizer.getAccessOptimizer().getPropertyValues( bean );
		assertEquivalent( values, BeanReflectionHelper.TEST_VALUES );
	}

	@Test
	@JiraKey(value = "HHH-12584")
	public void testAbstractClass() {
		ReflectionOptimizer reflectionOptimizer = provider.getReflectionOptimizer( AbstractClass.class,
				new String[] {"getProperty"},
				new String[] {"setProperty"}, new Class[] {String.class} );
		assertNotNull( reflectionOptimizer );
	}

	@Test
	@JiraKey(value = "HHH-12584")
	public void testInterface() {
		ReflectionOptimizer reflectionOptimizer = provider.getReflectionOptimizer( Interface.class,
				new String[] {"getProperty"},
				new String[] {"setProperty"}, new Class[] {String.class} );
		assertNotNull( reflectionOptimizer );
	}

	private void assertEquivalent(Object[] checkValues, Object[] values) {
		assertEquals( checkValues.length, values.length, "Different lengths" );
		for ( int i = 0; i < checkValues.length; i++ ) {
			assertEquals( checkValues[i], values[i], "different values at index [" + i + "]" );
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
