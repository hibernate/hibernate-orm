/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.configuration.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.jandex.AnnotationInstance;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AnnotationProxyBuilder {
	private final Map<AnnotationInstance, Object> proxyObjectMap = new HashMap<AnnotationInstance, Object>();
	private final Map<Class, ProxyFactory> proxyFactoryMap = new HashMap<Class, ProxyFactory>();

	public <T> T getAnnotationProxy(
			final AnnotationInstance annotationInstance,
			final Class<T> annotationClass,
			final ClassLoaderService classLoaderService) {
		T annotationProxy = (T) proxyObjectMap.get( annotationInstance );
		if ( annotationProxy == null ) {
			annotationProxy = buildAnnotationProxy( annotationInstance, annotationClass, classLoaderService );
			proxyObjectMap.put( annotationInstance, annotationProxy );
		}
		return annotationProxy;
	}

	private <T> T buildAnnotationProxy(
			final AnnotationInstance annotationInstance,
			final Class<T> annotationClass,
			final ClassLoaderService classLoaderService) {
		try {
			final Class annotation = annotationClass.getClassLoader().loadClass( annotationClass.getName() );
			final Class proxyClass = getProxyFactory( annotation ).createClass();
			final ProxyObject proxyObject = (ProxyObject) proxyClass.newInstance();
			proxyObject.setHandler( new MethodHandler() {
				@Override
				public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
					String executedMethodName = thisMethod.getName();
					if ( "toString".equals( executedMethodName ) ) {
						return proxyClass.getName() + "@" + System.identityHashCode( self );
					}
					final Class<?> returnType = thisMethod.getReturnType();
					if ( returnType.isArray() && returnType.getComponentType().isAnnotation() ) {
						final AnnotationInstance[] returnValues = JandexHelper.getValue(
								annotationInstance,
								executedMethodName,
								AnnotationInstance[].class,
								classLoaderService
						);
						return buildAnnotationProxyArray(
								returnValues,
								returnType.getComponentType(),
								classLoaderService
						);
					}
					return JandexHelper.getValue(
							annotationInstance,
							executedMethodName,
							thisMethod.getReturnType(),
							classLoaderService
					);
				}
			} );
			return (T) proxyObject;
		}
		catch ( Exception e ) {
			throw new HibernateException( e );
		}
	}

	@SuppressWarnings( {"unchecked"})
	private <T> T[] buildAnnotationProxyArray(
			final AnnotationInstance[] annotationInstances,
			final Class<T> annotationClass,
			final ClassLoaderService classLoaderService) {
		final T[] annotationProxyArray = (T[]) Array.newInstance( annotationClass, annotationInstances.length );
		for ( int i = 0 ; i < annotationInstances.length ; i++ ) {
			annotationProxyArray[i] = buildAnnotationProxy( annotationInstances[i], annotationClass, classLoaderService );
		}
		return annotationProxyArray;
	}

	private ProxyFactory getProxyFactory(final Class annotation) {
		ProxyFactory proxyFactory = proxyFactoryMap.get( annotation );
		if ( proxyFactory == null ) {
			proxyFactory = new ProxyFactory();
			proxyFactoryMap.put( annotation, proxyFactory );
		}
		proxyFactory.setInterfaces( new Class[] { annotation } );
		return proxyFactory;
	}
}
