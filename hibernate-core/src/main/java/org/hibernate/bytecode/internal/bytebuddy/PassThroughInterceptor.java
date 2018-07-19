/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.hibernate.proxy.ProxyConfiguration;

public class PassThroughInterceptor implements ProxyConfiguration.Interceptor {

	private HashMap<Object, Object> data = new HashMap<>();
	private final String proxiedClassName;

	public PassThroughInterceptor(String proxiedClassName) {
		this.proxiedClassName = proxiedClassName;
	}

	@Override
	public Object intercept(Object instance, Method method, Object[] arguments) throws Exception {
		final String name = method.getName();

		if ( "toString".equals( name ) && arguments.length == 0 ) {
			return proxiedClassName + "@" + System.identityHashCode( instance );
		}

		if ( "equals".equals( name ) && arguments.length == 1 ) {
			return instance == arguments[0];
		}

		if ( "hashCode".equals( name ) && arguments.length == 0 ) {
			return System.identityHashCode( instance );
		}

		if ( name.startsWith( "get" ) && hasGetterSignature( method ) ) {
			final String propName = name.substring( 3 );
			return data.get( propName );
		}

		if ( name.startsWith( "is" ) && hasGetterSignature( method ) ) {
			final String propName = name.substring( 2 );
			return data.get( propName );
		}

		if ( name.startsWith( "set" ) && hasSetterSignature( method ) ) {
			final String propName = name.substring( 3 );
			data.put( propName, arguments[0] );
			return null;
		}

		// todo : what else to do here?
		return null;
	}

	private boolean hasGetterSignature(Method method) {
		return method.getParameterCount() == 0
				&& method.getReturnType() != null;
	}

	private boolean hasSetterSignature(Method method) {
		return method.getParameterCount() == 1
				&& ( method.getReturnType() == null || method.getReturnType() == void.class );
	}
}
