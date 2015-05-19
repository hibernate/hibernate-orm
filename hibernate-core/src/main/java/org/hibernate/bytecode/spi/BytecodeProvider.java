/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import org.hibernate.bytecode.buildtime.spi.ClassFilter;
import org.hibernate.bytecode.buildtime.spi.FieldFilter;

/**
 * Contract for providers of bytecode services to Hibernate.
 * <p/>
 * Bytecode requirements break down into basically 3 areas<ol>
 *     <li>proxy generation (both for runtime-lazy-loading and basic proxy generation) {@link #getProxyFactoryFactory()}</li>
 *     <li>bean reflection optimization {@link #getReflectionOptimizer}</li>
 *     <li>field-access instrumentation {@link #getTransformer}</li>
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
	public ProxyFactoryFactory getProxyFactoryFactory();

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
	public ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames, Class[] types);

	/**
	 * Generate a ClassTransformer capable of performing bytecode manipulation.
	 *
	 * @param classFilter filter used to limit which classes are to be instrumented
	 * via this ClassTransformer.
	 * @param fieldFilter filter used to limit which fields are to be instrumented
	 * via this ClassTransformer.
	 * @return The appropriate ClassTransformer.
	 */
	public ClassTransformer getTransformer(ClassFilter classFilter, FieldFilter fieldFilter);

	/**
	 * Retrieve the interception metadata for the particular entity type.
	 *
	 * @param entityClass The entity class.  Note: we pass class here instead of the usual "entity name" because
	 * only real classes can be instrumented.
	 *
	 * @return The metadata
	 */
	public EntityInstrumentationMetadata getEntityInstrumentationMetadata(Class entityClass);
}
