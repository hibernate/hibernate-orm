/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistryInitiator;
import org.hibernate.service.ServiceRegistry;

/**
 * Helper class for helping deal with the reflection calls relating to CDI
 * in terms of building CDI-based {@link org.hibernate.resource.beans.container.spi.BeanContainer}
 * instance
 *
 * We need to to avoid statically linking CDI classed into the ClassLoader which
 * would lead to errors if CDI is not available on the classpath.
 *
 * @author Steve Ebersole
 */
public class CdiBeanContainerBuilder {
	private static final String CONTAINER_FQN_IMMEDIATE = "org.hibernate.resource.beans.container.internal.CdiBeanContainerImmediateAccessImpl";
	private static final String CONTAINER_FQN_DELAYED = "org.hibernate.resource.beans.container.internal.CdiBeanContainerDelayedAccessImpl";
	private static final String CONTAINER_FQN_EXTENDED = "org.hibernate.resource.beans.container.internal.CdiBeanContainerExtendedAccessImpl";

	private static final String BEAN_MANAGER_EXTENSION_FQN = "org.hibernate.resource.beans.container.spi.ExtendedBeanManager";

	@SuppressWarnings("unchecked")
	public static BeanContainer fromBeanManagerReference(
			Object beanManagerRef,
			ServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		final Class beanManagerClass = ManagedBeanRegistryInitiator.cdiBeanManagerClass( classLoaderService );
		final Class extendedBeanManagerClass = getHibernateClass( BEAN_MANAGER_EXTENSION_FQN );

		final Class<? extends BeanContainer> containerClass;
		final Class ctorArgType;

		if ( extendedBeanManagerClass.isInstance( beanManagerRef ) ) {
			containerClass = getHibernateClass( CONTAINER_FQN_EXTENDED );
			ctorArgType = extendedBeanManagerClass;
		}
		else {
			ctorArgType = beanManagerClass;

			final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );
			final boolean delayAccessToCdi = cfgService.getSetting( AvailableSettings.DELAY_CDI_ACCESS, StandardConverters.BOOLEAN, false );
			if ( delayAccessToCdi ) {
				containerClass = getHibernateClass( CONTAINER_FQN_DELAYED );
			}
			else {
				containerClass = getHibernateClass( CONTAINER_FQN_IMMEDIATE );
			}
		}

		try {
			final Constructor<? extends BeanContainer> ctor = containerClass.getDeclaredConstructor( ctorArgType );
			try {
				ReflectHelper.ensureAccessibility( ctor );
				return ctor.newInstance( ctorArgType.cast( beanManagerRef ) );
			}
			catch (InvocationTargetException e) {
				throw new HibernateException( "Problem building " + containerClass.getName(), e.getCause() );
			}
			catch (Exception e) {
				throw new HibernateException( "Problem building " + containerClass.getName(), e );
			}
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"Could not locate proper %s constructor",
							containerClass.getName()
					),
					e
			);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> getHibernateClass(String fqn) {
		// we use this Class's ClassLoader...
		try {
			return  (Class<T>) Class.forName(
					fqn,
					true,
					CdiBeanContainerBuilder.class.getClassLoader()
			);
		}
		catch (ClassNotFoundException e) {
			throw new HibernateException( "Unable to locate Hibernate class by name via reflection : " + fqn, e );
		}
	}
}
