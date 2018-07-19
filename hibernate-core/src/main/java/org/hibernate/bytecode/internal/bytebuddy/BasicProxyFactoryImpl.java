/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;

public class BasicProxyFactoryImpl implements BasicProxyFactory {

	private static final Class[] NO_INTERFACES = new Class[0];
	private static final String PROXY_NAMING_SUFFIX = Environment.useLegacyProxyClassnames() ? "HibernateBasicProxy$" : "HibernateBasicProxy";

	private final Class proxyClass;
	private final ProxyConfiguration.Interceptor interceptor;

	@SuppressWarnings("unchecked")
	public BasicProxyFactoryImpl(Class superClass, Class[] interfaces, ByteBuddyState bytebuddy) {
		if ( superClass == null && ( interfaces == null || interfaces.length < 1 ) ) {
			throw new AssertionFailure( "attempting to build proxy without any superclass or interfaces" );
		}

		final Class<?> superClassOrMainInterface = superClass != null ? superClass : interfaces[0];

		this.proxyClass = bytebuddy.getCurrentyByteBuddy()
			.with( new NamingStrategy.SuffixingRandom( PROXY_NAMING_SUFFIX, new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( superClassOrMainInterface.getName() ) ) )
			.subclass( superClass == null ? Object.class : superClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR )
			.implement( interfaces == null ? NO_INTERFACES : interfaces )
			.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
			.method( ElementMatchers.isVirtual().and( ElementMatchers.not( ElementMatchers.isFinalizer() ) ) )
					.intercept( MethodDelegation.to( ProxyConfiguration.InterceptorDispatcher.class ) )
			.implement( ProxyConfiguration.class )
					.intercept( FieldAccessor.ofField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME ).withAssigner( Assigner.DEFAULT, Assigner.Typing.DYNAMIC ) )
			.make()
			.load( superClassOrMainInterface.getClassLoader(), ByteBuddyState.resolveClassLoadingStrategy( superClassOrMainInterface ) )
			.getLoaded();
		this.interceptor = new PassThroughInterceptor( proxyClass.getName() );
	}

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
}
