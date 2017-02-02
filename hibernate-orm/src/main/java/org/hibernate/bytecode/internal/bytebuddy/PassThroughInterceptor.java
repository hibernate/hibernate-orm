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

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

public class PassThroughInterceptor implements ProxyConfiguration.Interceptor {

	private HashMap data = new HashMap();
	private final Object proxiedObject;
	private final String proxiedClassName;

	public PassThroughInterceptor(Object proxiedObject, String proxiedClassName) {
		this.proxiedObject = proxiedObject;
		this.proxiedClassName = proxiedClassName;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object intercept(Object instance, Method method, Object[] arguments) throws Exception {
		final String name = method.getName();
		if ( "toString".equals( name ) ) {
			return proxiedClassName + "@" + System.identityHashCode( instance );
		}
		else if ( "equals".equals( name ) ) {
			return proxiedObject == instance;
		}
		else if ( "hashCode".equals( name ) ) {
			return System.identityHashCode( instance );
		}

		final boolean hasGetterSignature = method.getParameterCount() == 0
				&& method.getReturnType() != null;
		final boolean hasSetterSignature = method.getParameterCount() == 1
				&& ( method.getReturnType() == null || method.getReturnType() == void.class );

		if ( name.startsWith( "get" ) && hasGetterSignature ) {
			final String propName = name.substring( 3 );
			return data.get( propName );
		}
		else if ( name.startsWith( "is" ) && hasGetterSignature ) {
			final String propName = name.substring( 2 );
			return data.get( propName );
		}
		else if ( name.startsWith( "set" ) && hasSetterSignature ) {
			final String propName = name.substring( 3 );
			data.put( propName, arguments[0] );
			return null;
		}
		else {
			// todo : what else to do here?
			return null;
		}
	}
}
