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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;

public class BasicProxyFactoryImpl implements BasicProxyFactory {

	private static final ConcurrentMap<Set<Class>, Class> CACHE = new ConcurrentHashMap<Set<Class>, Class>();

	private static final Class[] NO_INTERFACES = new Class[0];

	private final Class proxyClass;

	public BasicProxyFactoryImpl(Class superClass, Class[] interfaces) {
		if ( superClass == null && ( interfaces == null || interfaces.length < 1 ) ) {
			throw new AssertionFailure( "attempting to build proxy without any superclass or interfaces" );
		}

		Set<Class> key = new HashSet<Class>();
		if ( superClass != null ) {
			key.add( superClass );
		}
		if ( interfaces != null && interfaces.length > 0 ) {
			key.addAll( Arrays.asList( interfaces ) );
		}

		Class proxyClass = CACHE.get( key );

		if ( proxyClass == null ) {
			proxyClass = new ByteBuddy()
					.with( TypeValidation.DISABLED )
					.with( new AuxiliaryType.NamingStrategy.SuffixingRandom( "HibernateBasicProxy" ) )
					.subclass( superClass == null ? Object.class : superClass )
					.implement( interfaces == null ? NO_INTERFACES : interfaces )
					.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
					.method( ElementMatchers.isVirtual().and( ElementMatchers.not( ElementMatchers.isFinalizer() ) ) )
					.intercept( MethodDelegation.toField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME ) )
					.implement( ProxyConfiguration.class )
					.intercept( FieldAccessor.ofField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME ).withAssigner( Assigner.DEFAULT, Assigner.Typing.DYNAMIC ) )
					.make()
					.load( BasicProxyFactory.class.getClassLoader() )
					.getLoaded();
			Class previousProxy = CACHE.putIfAbsent( key, proxyClass );
			if ( previousProxy != null ) {
				proxyClass = previousProxy;
			}
		}

		this.proxyClass = proxyClass;
	}

	public Object getProxy() {
		try {
			final ProxyConfiguration proxy = (ProxyConfiguration) proxyClass.newInstance();
			proxy.$$_hibernate_set_interceptor( new PassThroughInterceptor( proxy, proxyClass.getName() ) );
			return proxy;
		}
		catch (Throwable t) {
			throw new HibernateException( "Unable to instantiate proxy instance" );
		}
	}

	public boolean isInstance(Object object) {
		return proxyClass.isInstance( object );
	}
}
