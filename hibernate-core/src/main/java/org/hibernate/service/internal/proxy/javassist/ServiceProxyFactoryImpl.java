/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.internal.proxy.javassist;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import org.hibernate.service.internal.ServiceProxy;
import org.hibernate.service.internal.ServiceProxyGenerationException;
import org.hibernate.service.spi.Service;
import org.hibernate.service.spi.proxy.ServiceProxyFactory;
import org.hibernate.service.spi.proxy.ServiceProxyTargetSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Steve Ebersole
 */
public class ServiceProxyFactoryImpl implements ServiceProxyFactory {
	private final ServiceProxyTargetSource serviceRegistry;

	public ServiceProxyFactoryImpl(ServiceProxyTargetSource serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	private static final MethodFilter FINALIZE_FILTER = new MethodFilter() {
		public boolean isHandled(Method m) {
			// skip finalize methods
			return !( m.getParameterTypes().length == 0 && m.getName().equals( "finalize" ) );
		}
	};

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T extends Service> T makeProxy(Class<T> serviceRole) {
		try {
			ProxyFactory factory = new ProxyFactory();
			factory.setFilter( FINALIZE_FILTER );

			Class[] interfaces = new Class[2];
			interfaces[0] = serviceRole;
			interfaces[1] = ServiceProxy.class;
			factory.setInterfaces( interfaces );

			Class proxyClass = factory.createClass();
			ProxyObject proxyObject = (ProxyObject) proxyClass.newInstance();
			proxyObject.setHandler( new ServiceProxyMethodInterceptor<T>( (T)proxyObject, serviceRole, serviceRegistry ) );
			return (T) proxyObject;
		}
		catch (Exception e) {
			throw new ServiceProxyGenerationException( "Unable to make service proxy", e );
		}
	}

	private static class ServiceProxyMethodInterceptor<T extends Service> implements MethodHandler {
		private final T proxy;
		private final Class<T> serviceRole;
		private final ServiceProxyTargetSource serviceRegistry;

		private ServiceProxyMethodInterceptor(T proxy, Class<T> serviceRole, ServiceProxyTargetSource serviceRegistry) {
			this.proxy = proxy;
			this.serviceRole = serviceRole;
			this.serviceRegistry = serviceRegistry;
		}

		@Override
		@SuppressWarnings( {"UnnecessaryBoxing"} )
		public Object invoke(
				Object object,
		        Method method,
		        Method method1,
		        Object[] args) throws Exception {
			String name = method.getName();
			if ( "toString".equals( name ) ) {
				return serviceRole.getName() + "_$$_Proxy@" + System.identityHashCode( object );
			}
			else if ( "equals".equals( name ) ) {
				return proxy == object ? Boolean.TRUE : Boolean.FALSE;
			}
			else if ( "hashCode".equals( name ) ) {
				return Integer.valueOf( System.identityHashCode( object ) );
			}
			else if ( "getTargetInstance".equals( name ) && ServiceProxy.class.equals( method.getDeclaringClass() ) ) {
				return serviceRegistry.getServiceInternal( serviceRole );
			}
			else {
				try {
					T target = serviceRegistry.getServiceInternal( serviceRole );
					return method.invoke( target, args );
				}
				catch (InvocationTargetException e) {
					throw (Exception) e.getTargetException();
				}
			}
		}
	}

}
