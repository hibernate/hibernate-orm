/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.jpa.event.internal.jpa.ListenerFactoryStandardImpl;

/**
 * @author Steve Ebersole
 */
public class ListenerFactoryBuilder {

	public static ListenerFactory buildListenerFactory(SessionFactoryOptions options) {
		final Object beanManagerRef = options.getBeanManagerReference();
		if ( beanManagerRef == null ) {
			return new ListenerFactoryStandardImpl();
		}
		else if ( ExtendedBeanManager.class.isInstance( beanManagerRef ) ) {
			return buildLazyBeanManagerListenerFactory( beanManagerRef );
		}
		else {
			return buildStandardBeanManagerListenerFactory( beanManagerRef );
		}
	}


	private static final String CDI_LISTENER_FACTORY_CLASS = "org.hibernate.jpa.event.internal.jpa.ListenerFactoryBeanManagerStandardImpl";
	private static final String CDI_LISTENER_FACTORY_LAZY_CLASS = "org.hibernate.jpa.event.internal.jpa.ListenerFactoryBeanManagerLazyImpl";
	private static final String CDI_LISTENER_FACTORY_METHOD_NAME = "fromBeanManagerReference";

	private static ListenerFactory buildStandardBeanManagerListenerFactory(Object beanManagerRef) {
		return buildBeanManagerListenerFactory( beanManagerRef, CDI_LISTENER_FACTORY_CLASS );
	}

	private static ListenerFactory buildLazyBeanManagerListenerFactory(Object beanManagerRef) {
		return buildBeanManagerListenerFactory( beanManagerRef, CDI_LISTENER_FACTORY_LAZY_CLASS );
	}

	@SuppressWarnings("unchecked")
	private static ListenerFactory buildBeanManagerListenerFactory(Object beanManagerRef, String factoryClassName) {
		try {
			// specifically using our ClassLoader here...
			final Class beanManagerListenerFactoryClass = ListenerFactoryBuilder.class.getClassLoader()
					.loadClass( factoryClassName );
			final Method beanManagerListenerFactoryBuilderMethod = beanManagerListenerFactoryClass.getMethod(
					CDI_LISTENER_FACTORY_METHOD_NAME,
					Object.class
			);

			try {
				return (ListenerFactory) beanManagerListenerFactoryBuilderMethod.invoke( null, beanManagerRef );
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}
		catch (ClassNotFoundException e) {
			throw new HibernateException(
					"Could not locate BeanManager ListenerFactory class [" + factoryClassName + "] to handle CDI extensions",
					e
			);
		}
		catch (HibernateException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new HibernateException(
					"Could not access BeanManagerListenerFactory class to handle CDI extensions",
					e
			);
		}
	}
}
