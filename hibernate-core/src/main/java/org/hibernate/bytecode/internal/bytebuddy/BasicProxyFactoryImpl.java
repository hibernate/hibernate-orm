/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Constructor;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.engine.spi.PrimeAmongSecondarySupertypes;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.proxy.ProxyConfiguration;

import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;

public class BasicProxyFactoryImpl implements BasicProxyFactory {

	private static final Class[] NO_INTERFACES = ArrayHelper.EMPTY_CLASS_ARRAY;
	private static final String PROXY_NAMING_SUFFIX = "HibernateBasicProxy";

	private final Class proxyClass;
	private final ProxyConfiguration.Interceptor interceptor;
	private final Constructor proxyClassConstructor;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BasicProxyFactoryImpl(final Class superClass, final Class interfaceClass, final ByteBuddyState byteBuddyState) {
		if ( superClass == null && interfaceClass == null ) {
			throw new AssertionFailure( "attempting to build proxy without any superclass or interfaces" );
		}
		if ( superClass != null && interfaceClass != null ) {
			//TODO cleanup this case
			throw new AssertionFailure( "Ambiguous call: we assume invocation with EITHER a superClass OR an interfaceClass" );
		}

		final Class<?> superClassOrMainInterface = superClass != null ? superClass : interfaceClass;
		final ByteBuddyState.ProxyDefinitionHelpers helpers = byteBuddyState.getProxyDefinitionHelpers();
		final String proxyClassName = superClassOrMainInterface.getName() + "$" + PROXY_NAMING_SUFFIX;

		this.proxyClass = byteBuddyState.loadBasicProxy( superClassOrMainInterface, proxyClassName, (byteBuddy, namingStrategy) ->
				helpers.appendIgnoreAlsoAtEnd( byteBuddy
					.with( namingStrategy )
					.subclass( superClass == null ? Object.class : superClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR )
					.implement( interfaceClass == null ? NO_INTERFACES : new Class[]{ interfaceClass } )
					.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
					.method( byteBuddyState.getProxyDefinitionHelpers().getVirtualNotFinalizerFilter() )
							.intercept( byteBuddyState.getProxyDefinitionHelpers().getDelegateToInterceptorDispatcherMethodDelegation() )
					.implement( ProxyConfiguration.class )
							.intercept( byteBuddyState.getProxyDefinitionHelpers().getInterceptorFieldAccessor() )
				)
		);
		this.interceptor = new PassThroughInterceptor( proxyClass.getName() );
		try {
			proxyClassConstructor = proxyClass.getConstructor();
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure( "Could not access default constructor from newly generated basic proxy" );
		}
	}

	@Override
	public Object getProxy() {
		final PrimeAmongSecondarySupertypes instance;
		try {
			instance = (PrimeAmongSecondarySupertypes) proxyClassConstructor.newInstance();
		}
		catch (Throwable t) {
			throw new HibernateException( "Unable to instantiate proxy instance", t );
		}
		final ProxyConfiguration proxyConfiguration = instance.asProxyConfiguration();
		if ( proxyConfiguration == null ) {
			throw new HibernateException( "Produced proxy does not correctly implement ProxyConfiguration" );
		}
		proxyConfiguration.$$_hibernate_set_interceptor( this.interceptor );
		return instance;
	}

	public boolean isInstance(Object object) {
		return proxyClass.isInstance( object );
	}

}
