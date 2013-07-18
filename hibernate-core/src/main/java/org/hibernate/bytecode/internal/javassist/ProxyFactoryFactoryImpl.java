/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.bytecode.internal.javassist;

import java.lang.reflect.Method;
import java.util.HashMap;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyObject;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.hibernate.proxy.pojo.javassist.JavassistProxyFactory;

/**
 * A factory for Javassist-based {@link ProxyFactory} instances.
 *
 * @author Steve Ebersole
 */
public class ProxyFactoryFactoryImpl implements ProxyFactoryFactory {

	/**
	 * Builds a Javassist-based proxy factory.
	 *
	 * @return a new Javassist-based proxy factory.
	 */
	public ProxyFactory buildProxyFactory() {
		return new JavassistProxyFactory();
	}

	/**
	 * Constructs a BasicProxyFactoryImpl
	 *
	 * @param superClass The abstract super class (or null if none).
	 * @param interfaces Interfaces to be proxied (or null if none).
	 *
	 * @return The constructed BasicProxyFactoryImpl
	 */
	public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
		return new BasicProxyFactoryImpl( superClass, interfaces );
	}

	private static class BasicProxyFactoryImpl implements BasicProxyFactory {
		private final Class proxyClass;

		public BasicProxyFactoryImpl(Class superClass, Class[] interfaces) {
			proxyClass = JavassistLazyInitializer.getProxyFactory( superClass, interfaces );
		}

		public Object getProxy() {
			try {
				final ProxyObject proxy = (ProxyObject) proxyClass.newInstance();
				proxy.setHandler( new PassThroughHandler( proxy, proxyClass.getName() ) );
				return proxy;
			}
			catch ( Throwable t ) {
				throw new HibernateException( "Unable to instantiated proxy instance" );
			}
		}

		public boolean isInstance(Object object) {
			return proxyClass.isInstance( object );
		}
	}


	private static class PassThroughHandler implements MethodHandler {
		private HashMap data = new HashMap();
		private final Object proxiedObject;
		private final String proxiedClassName;

		public PassThroughHandler(Object proxiedObject, String proxiedClassName) {
			this.proxiedObject = proxiedObject;
			this.proxiedClassName = proxiedClassName;
		}

		@SuppressWarnings("unchecked")
		public Object invoke(
				Object object,
				Method method,
				Method method1,
				Object[] args) throws Exception {
			final String name = method.getName();
			if ( "toString".equals( name ) ) {
				return proxiedClassName + "@" + System.identityHashCode( object );
			}
			else if ( "equals".equals( name ) ) {
				return proxiedObject == object;
			}
			else if ( "hashCode".equals( name ) ) {
				return System.identityHashCode( object );
			}

			final boolean hasGetterSignature = method.getParameterTypes().length == 0
					&& method.getReturnType() != null;
			final boolean hasSetterSignature = method.getParameterTypes().length == 1
					&& ( method.getReturnType() == null || method.getReturnType() == void.class );

			if ( name.startsWith( "get" ) && hasGetterSignature ) {
				final String propName = name.substring( 3 );
				return data.get( propName );
			}
			else if ( name.startsWith( "is" ) && hasGetterSignature ) {
				final String propName = name.substring( 2 );
				return data.get( propName );
			}
			else if ( name.startsWith( "set" ) && hasSetterSignature) {
				final String propName = name.substring( 3 );
				data.put( propName, args[0] );
				return null;
			}
			else {
				// todo : what else to do here?
				return null;
			}
		}
	}
}
