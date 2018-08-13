/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.NamingStrategy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;

public class BasicProxyFactoryImpl implements BasicProxyFactory {

	private static final Class[] NO_INTERFACES = new Class[0];
	private static final String PROXY_NAMING_SUFFIX = Environment.useLegacyProxyClassnames() ? "HibernateBasicProxy$" : "HibernateBasicProxy";

	private final Class proxyClass;
	private final ProxyConfiguration.Interceptor interceptor;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BasicProxyFactoryImpl(Class superClass, Class[] interfaces, ByteBuddyState byteBuddyState) {
		if ( superClass == null && ( interfaces == null || interfaces.length < 1 ) ) {
			throw new AssertionFailure( "attempting to build proxy without any superclass or interfaces" );
		}

		final Class<?> superClassOrMainInterface = superClass != null ? superClass : interfaces[0];
		final TypeCache.SimpleKey cacheKey = getCacheKey( superClass, interfaces );

		this.proxyClass = byteBuddyState.loadBasicProxy( superClassOrMainInterface, cacheKey, byteBuddy -> byteBuddy
				.with( new NamingStrategy.SuffixingRandom( PROXY_NAMING_SUFFIX, new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( superClassOrMainInterface.getName() ) ) )
				.subclass( superClass == null ? Object.class : superClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR )
				.implement( interfaces == null ? NO_INTERFACES : interfaces )
				.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
				.method( byteBuddyState.getProxyDefinitionHelpers().getVirtualNotFinalizerFilter() )
						.intercept( byteBuddyState.getProxyDefinitionHelpers().getDelegateToInterceptorDispatcherMethodDelegation() )
				.implement( ProxyConfiguration.class )
						.intercept( byteBuddyState.getProxyDefinitionHelpers().getInterceptorFieldAccessor() )
		);
		this.interceptor = new PassThroughInterceptor( proxyClass.getName() );
	}

	@Override
	public Object getProxy() {
		try {
			final ProxyConfiguration proxy = (ProxyConfiguration) proxyClass.newInstance();
			proxy.$$_hibernate_set_interceptor( this.interceptor );
			return proxy;
		}
		catch (Throwable t) {
			throw new HibernateException( "Unable to instantiate proxy instance", t );
		}
	}

	public boolean isInstance(Object object) {
		return proxyClass.isInstance( object );
	}

	private TypeCache.SimpleKey getCacheKey(Class<?> superClass, Class<?>[] interfaces) {
		Set<Class<?>> key = new HashSet<Class<?>>();
		if ( superClass != null ) {
			key.add( superClass );
		}
		if ( interfaces != null ) {
			key.addAll( Arrays.<Class<?>>asList( interfaces ) );
		}

		return new TypeCache.SimpleKey( key );
	}
}
