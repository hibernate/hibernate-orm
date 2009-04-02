/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.bytecode.cglib;

import org.hibernate.bytecode.ProxyFactoryFactory;
import org.hibernate.bytecode.BasicProxyFactory;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.proxy.pojo.cglib.CGLIBProxyFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Factory;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * A factory for CGLIB-based {@link ProxyFactory} instances.
 *
 * @author Steve Ebersole
 */
public class ProxyFactoryFactoryImpl implements ProxyFactoryFactory {

	/**
	 * Builds a CGLIB-based proxy factory.
	 *
	 * @return a new CGLIB-based proxy factory.
	 */
	public ProxyFactory buildProxyFactory() {
		return new CGLIBProxyFactory();
	}

	public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
		return new BasicProxyFactoryImpl( superClass, interfaces );
	}

	public static class BasicProxyFactoryImpl implements BasicProxyFactory {
		private final Class proxyClass;
		private final Factory factory;

		public BasicProxyFactoryImpl(Class superClass, Class[] interfaces) {
			if ( superClass == null && ( interfaces == null || interfaces.length < 1 ) ) {
				throw new AssertionFailure( "attempting to build proxy without any superclass or interfaces" );
			}

			Enhancer en = new Enhancer();
			en.setUseCache( false );
			en.setInterceptDuringConstruction( false );
			en.setUseFactory( true );
			en.setCallbackTypes( CALLBACK_TYPES );
			en.setCallbackFilter( FINALIZE_FILTER );
			if ( superClass != null ) {
				en.setSuperclass( superClass );
			}
			if ( interfaces != null && interfaces.length > 0 ) {
				en.setInterfaces( interfaces );
			}
			proxyClass = en.createClass();
			try {
				factory = ( Factory ) proxyClass.newInstance();
			}
			catch ( Throwable t ) {
				throw new HibernateException( "Unable to build CGLIB Factory instance" );
			}
		}

		public Object getProxy() {
			try {
				return factory.newInstance(
						new Callback[] { new PassThroughInterceptor( proxyClass.getName() ), NoOp.INSTANCE }
				);
			}
			catch ( Throwable t ) {
				throw new HibernateException( "Unable to instantiate proxy instance" );
			}
		}
	}

	private static final CallbackFilter FINALIZE_FILTER = new CallbackFilter() {
		public int accept(Method method) {
			if ( method.getParameterTypes().length == 0 && method.getName().equals("finalize") ){
				return 1;
			}
			else {
				return 0;
			}
		}
	};

	private static final Class[] CALLBACK_TYPES = new Class[] { MethodInterceptor.class, NoOp.class };

	private static class PassThroughInterceptor implements MethodInterceptor {
		private HashMap data = new HashMap();
		private final String proxiedClassName;

		public PassThroughInterceptor(String proxiedClassName) {
			this.proxiedClassName = proxiedClassName;
		}

		public Object intercept(
				Object obj,
		        Method method,
		        Object[] args,
		        MethodProxy proxy) throws Throwable {
			String name = method.getName();
			if ( "toString".equals( name ) ) {
				return proxiedClassName + "@" + System.identityHashCode( obj );
			}
			else if ( "equals".equals( name ) ) {
				return args[0] instanceof Factory && ( ( Factory ) args[0] ).getCallback( 0 ) == this
						? Boolean.TRUE
			            : Boolean.FALSE;
			}
			else if ( "hashCode".equals( name ) ) {
				return new Integer( System.identityHashCode( obj ) );
			}
			boolean hasGetterSignature = method.getParameterTypes().length == 0 && method.getReturnType() != null;
			boolean hasSetterSignature = method.getParameterTypes().length == 1 && ( method.getReturnType() == null || method.getReturnType() == void.class );
			if ( name.startsWith( "get" ) && hasGetterSignature ) {
				String propName = name.substring( 3 );
				return data.get( propName );
			}
			else if ( name.startsWith( "is" ) && hasGetterSignature ) {
				String propName = name.substring( 2 );
				return data.get( propName );
			}
			else if ( name.startsWith( "set" ) && hasSetterSignature) {
				String propName = name.substring( 3 );
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
