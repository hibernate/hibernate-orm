/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;


import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * @author Réda Housni Alaoui
 */
public class EnvironmentTest {

	@Test
	public void testCustomBytecodeProviderBuild() {
		Properties properties = new Properties();
		properties.put(AvailableSettings.BYTECODE_PROVIDER, MyBytecodeProvider.class.getName());

		BytecodeProvider bytecodeProvider = Environment.buildBytecodeProvider(properties);

		Assert.assertTrue(bytecodeProvider instanceof MyBytecodeProvider);
	}

	public static class MyBytecodeProvider implements BytecodeProvider {

		@Override
		public ProxyFactoryFactory getProxyFactoryFactory() {
			return null;
		}

		@Override
		public ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames, Class[] types) {
			return null;
		}

		@Override
		public Enhancer getEnhancer(EnhancementContext enhancementContext) {
			return null;
		}
	}
}