/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Constructor;

import net.bytebuddy.description.modifier.Visibility;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;

import static org.hibernate.proxy.ProxyConfiguration.INTERCEPTOR_FIELD_NAME;

public class BasicProxyFactoryImpl implements BasicProxyFactory {

	private static final Class<?>[] NO_INTERFACES = ArrayHelper.EMPTY_CLASS_ARRAY;
	private static final String PROXY_NAMING_SUFFIX = "HibernateBasicProxy";

	private final Class<?> proxyClass;
	private final Constructor<?> proxyClassConstructor;

	public BasicProxyFactoryImpl(final Class<?> superClass, final Class<?> interfaceClass, final ByteBuddyState byteBuddyState) {
		if ( superClass == null && interfaceClass == null ) {
			throw new AssertionFailure( "attempting to build proxy without any superclass or interfaces" );
		}
		if ( superClass != null && interfaceClass != null ) {
			//TODO cleanup this case
			throw new AssertionFailure( "Ambiguous call: we assume invocation with EITHER a superClass OR an interfaceClass" );
		}

		final var superClassOrMainInterface = superClass != null ? superClass : interfaceClass;
		final var helpers = byteBuddyState.getProxyDefinitionHelpers();
		final var constants = byteBuddyState.getEnhancerConstants();
		final String proxyClassName = superClassOrMainInterface.getName() + "$" + PROXY_NAMING_SUFFIX;

		this.proxyClass = byteBuddyState.loadBasicProxy( superClassOrMainInterface, proxyClassName, (byteBuddy, namingStrategy) ->
				helpers.appendIgnoreAlsoAtEnd( byteBuddy
					.with( namingStrategy )
					.subclass( superClass == null ? Object.class : superClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR )
					.implement( interfaceClass == null ? NO_INTERFACES : new Class[]{ interfaceClass } )
					.defineField( INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
					.method( helpers.getVirtualNotFinalizerFilter() )
							.intercept( helpers.getDelegateToInterceptorDispatcherMethodDelegation() )
					.implement( constants.INTERFACES_for_ProxyConfiguration )
							.intercept( helpers.getInterceptorFieldAccessor() )
				)
		);

		try {
			proxyClassConstructor = proxyClass.getConstructor();
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure( "Could not access default constructor from newly generated basic proxy" );
		}
	}

	@Override
	public Object getProxy() {
		final var instance = instantiateProxy();
		final var proxyConfiguration = instance.asProxyConfiguration();
		if ( proxyConfiguration == null ) {
			throw new HibernateException( "Produced proxy does not correctly implement ProxyConfiguration" );
		}
		// Create a dedicated interceptor for the proxy.
		// This is required as the interceptor is stateful.
		proxyConfiguration.$$_hibernate_set_interceptor(
				new PassThroughInterceptor( proxyClass.getName() ) );
		return instance;
	}

	private PrimeAmongSecondarySupertypes instantiateProxy() {
		try {
			return (PrimeAmongSecondarySupertypes) proxyClassConstructor.newInstance();
		}
		catch (Throwable t) {
			throw new HibernateException( "Unable to instantiate proxy instance", t );
		}
	}

	public boolean isInstance(Object object) {
		return proxyClass.isInstance( object );
	}

}
