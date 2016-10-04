/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * Contract for providers of bytecode services to Hibernate.
 * <p/>
 * Bytecode requirements break down into the following areas<ol>
 *     <li>proxy generation (both for runtime-lazy-loading and basic proxy generation) {@link #getProxyFactoryFactory()}</li>
 *     <li>bean reflection optimization {@link #getReflectionOptimizer}</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public interface BytecodeProvider {
	/**
	 * Retrieve the specific factory for this provider capable of
	 * generating run-time proxies for lazy-loading purposes.
	 *
	 * @return The provider specific factory.
	 */
	ProxyFactoryFactory getProxyFactoryFactory();

	/**
	 * Retrieve the ReflectionOptimizer delegate for this provider
	 * capable of generating reflection optimization components.
	 *
	 * @param clazz The class to be reflected upon.
	 * @param getterNames Names of all property getters to be accessed via reflection.
	 * @param setterNames Names of all property setters to be accessed via reflection.
	 * @param types The types of all properties to be accessed.
	 * @return The reflection optimization delegate.
	 */
	ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames, Class[] types);

	/**
	 * Returns a byte code enhancer that implements the enhancements described in the supplied enhancement context.
	 *
	 * @param enhancementContext The enhancement context that describes the enhancements to apply.
	 *
	 * @return An enhancer to perform byte code manipulations.
	 */
	Enhancer getEnhancer(EnhancementContext enhancementContext);
}
