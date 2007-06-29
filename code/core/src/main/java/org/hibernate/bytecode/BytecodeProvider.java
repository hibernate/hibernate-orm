package org.hibernate.bytecode;

import org.hibernate.bytecode.util.ClassFilter;
import org.hibernate.bytecode.util.FieldFilter;

/**
 * Contract for providers of bytecode services to Hibernate.
 * <p/>
 * Bytecode requirements break down into basically 3 areas<ol>
 * <li>proxy generation (both for runtime-lazy-loading and basic proxy generation)
 * {@link #getProxyFactoryFactory()}
 * <li>bean relection optimization {@link #getReflectionOptimizer}
 * <li>field-access instumentation {@link #getTransformer}
 * </ol>
 *
 * @author Steve Ebersole
 */
public interface BytecodeProvider {
	/**
	 * Retrieve the specific factory for this provider capable of
	 * generating run-time proxies for lazy-loading purposes.
	 *
	 * @return The provider specifc factory.
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
}
