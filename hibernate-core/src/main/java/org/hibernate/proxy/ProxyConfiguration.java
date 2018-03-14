/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;

import java.lang.reflect.Method;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.StubValue;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * A proxy configuration allows the definition of an interceptor object that decides on the behavior of a proxy.
 * This interface is meant for internal use but is in a public package in order to provide code generation.
 * <p>
 * While this interface depends on Byte Buddy types, this is only true for annotation types which are silently
 * suppressed by the runtime if they are not available on a class loader. This allows using this interceptor
 * and configuration with for example OSGi without any export of Byte Buddy when using Hibernate.
 */
public interface ProxyConfiguration {

	/**
	 * The canonical field name for an interceptor object stored in a proxied object.
	 */
	String INTERCEPTOR_FIELD_NAME = "$$_hibernate_interceptor";

	/**
	 * Defines an interceptor object that specifies the behavior of the proxy object.
	 *
	 * @param interceptor The interceptor object.
	 */
	void $$_hibernate_set_interceptor(Interceptor interceptor);

	/**
	 * An interceptor object that is responsible for invoking a proxy's method.
	 */
	interface Interceptor {

		/**
		 * Intercepts a method call to a proxy.
		 *
		 * @param instance The proxied instance.
		 * @param method The invoked method.
		 * @param arguments The intercepted method arguments.
		 *
		 * @return The method's return value.
		 *
		 * @throws Throwable If the intercepted method raises an exception.
		 */
		@RuntimeType
		Object intercept(@This Object instance, @Origin Method method, @AllArguments Object[] arguments) throws Throwable;
	}

	/**
	 * A static interceptor that guards against method calls before the interceptor is set.
	 */
	class InterceptorDispatcher {

		/**
		 * Intercepts a method call to a proxy.
		 *
		 * @param instance The proxied instance.
		 * @param method The invoked method.
		 * @param arguments The method arguments.
		 * @param stubValue The intercepted method's default value.
		 * @param interceptor The proxy object's interceptor instance.
		 *
		 * @return The intercepted method's return value.
		 *
		 * @throws Throwable If the intercepted method raises an exception.
		 */
		@RuntimeType
		public static Object intercept(
				@This final Object instance,
				@Origin final Method method,
				@AllArguments final Object[] arguments,
				@StubValue final Object stubValue,
				@FieldValue(INTERCEPTOR_FIELD_NAME) Interceptor interceptor
		) throws Throwable {
			if ( interceptor == null ) {
				if ( method.getName().equals( "getHibernateLazyInitializer" ) ) {
					return instance;
				}
				else {
					return stubValue;
				}
			}
			else {
				return interceptor.intercept( instance, method, arguments );
			}
		}
	}
}
