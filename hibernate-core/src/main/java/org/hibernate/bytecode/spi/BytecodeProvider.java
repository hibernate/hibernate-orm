/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

import java.util.Map;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contract for providers of bytecode services to Hibernate.
 * <p>
 * Bytecode requirements break down into the following areas<ol>
 *     <li>proxy generation (both for runtime-lazy-loading and basic proxy generation) {@link #getProxyFactoryFactory()}</li>
 *     <li>bean reflection optimization {@link #getReflectionOptimizer}</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface BytecodeProvider extends Service {
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
	 * @deprecated Use {@link #getReflectionOptimizer(Class, Map)} insstead
	 */
	@Deprecated(forRemoval = true)
	ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames, Class[] types);

	/**
	 * Retrieve the ReflectionOptimizer delegate for this provider
	 * capable of generating reflection optimization components.
	 *
	 * @param clazz The class to be reflected upon.
	 * @param propertyAccessMap The ordered property access map
	 * @return The reflection optimization delegate.
	 */
	@Nullable ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap);

	/**
	 * Returns a byte code enhancer that implements the enhancements described in the supplied enhancement context.
	 *
	 * @param enhancementContext The enhancement context that describes the enhancements to apply.
	 *
	 * @return An enhancer to perform byte code manipulations.
	 */
	@Nullable Enhancer getEnhancer(EnhancementContext enhancementContext);

	/**
	 * Some BytecodeProvider implementations will have classloader specific caching.
	 * These caches are useful at runtime but need to be reset at least on SessionFactory shutdown
	 * to prevent leaking the deployment classloader.
	 * Since the BytecodeProvider is static these caches are potentially shared across multiple
	 * deployments; in this case we'll clear all caches which might show as a small, temporary
	 * performance degradation on the SessionFactory instances which haven't been closed.
	 * This limitation will be removed in the future, when these providers will no longer be static.
	 */
	default void resetCaches() {}

}
