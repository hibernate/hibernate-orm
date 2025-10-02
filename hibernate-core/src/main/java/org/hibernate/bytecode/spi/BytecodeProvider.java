/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
	 * @deprecated Use {@link #getReflectionOptimizer(Class, PropertyInfo[])} instead
	 */
	@Deprecated(forRemoval = true)
	@Nullable ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap);

	/**
	 * Retrieve the ReflectionOptimizer delegate for this provider
	 * capable of generating reflection optimization components.
	 *
	 * @param clazz The class to be reflected upon.
	 * @param propertyInfos The ordered property infos
	 * @return The reflection optimization delegate.
	 */
	default @Nullable ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, PropertyInfo[] propertyInfos) {
		final Map<String, PropertyAccess> map = new HashMap<>();
		for ( int i = 0; i < propertyInfos.length; i++ ) {
			map.put( propertyInfos[i].propertyName(), propertyInfos[i].propertyAccess() );
		}
		return getReflectionOptimizer( clazz, map );
	}

	/**
	 * Information about a property of a class, needed for generating reflection optimizers.
	 *
	 */
	public static final class PropertyInfo {
		private final String propertyName;
		private final PropertyAccess propertyAccess;

		/**
		 * @param propertyName The name of the property
		 * @param propertyAccess The property access
		 */
		public PropertyInfo(String propertyName, PropertyAccess propertyAccess) {
			this.propertyName = propertyName;
			this.propertyAccess = propertyAccess;
		}

		public String propertyName() {
			return propertyName;
		}

		public PropertyAccess propertyAccess() {
			return propertyAccess;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( obj == null || obj.getClass() != this.getClass() ) {
				return false;
			}
			var that = (PropertyInfo) obj;
			return Objects.equals( this.propertyName, that.propertyName ) &&
					Objects.equals( this.propertyAccess, that.propertyAccess );
		}

		@Override
		public int hashCode() {
			return Objects.hash( propertyName, propertyAccess );
		}

		@Override
		public String toString() {
			return "PropertyInfo[" +
					"propertyName=" + propertyName + ", " +
					"propertyAccess=" + propertyAccess + ']';
		}
	}

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
