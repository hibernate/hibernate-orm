/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.cdi.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.resource.cdi.spi.ManagedBeanRegistry;
import org.hibernate.resource.cdi.spi.ManagedBeanRegistryInitiator;
import org.hibernate.service.ServiceRegistry;

/**
 * Utility class for helping deal with the reflection calls relating to CDI.
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryCdiBuilder {
	private static final String EXTENDED_BEAN_MANAGER_FQN = "org.hibernate.resource.cdi.spi.ExtendedBeanManager";

	private static final String REGISTRY_FQN_STANDARD = "org.hibernate.resource.cdi.internal.ManagedBeanRegistryCdiStandardImpl";
	private static final String REGISTRY_FQN_DELAYED = "org.hibernate.resource.cdi.internal.ManagedBeanRegistryCdiDelayedImpl";
	private static final String REGISTRY_FQN_EXTENDED = "org.hibernate.resource.cdi.internal.ManagedBeanRegistryCdiExtendedImpl";

	@SuppressWarnings("unchecked")
	public static ManagedBeanRegistry fromBeanManagerReference(
			Object beanManagerRef,
			ServiceRegistry serviceRegistry) {
		final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
		final Class beanManagerClass = ManagedBeanRegistryInitiator.cdiBeanManagerClass( cls );
		final Class extendedBeanManagerClass = getHibernateClass( EXTENDED_BEAN_MANAGER_FQN );

		final Class<? extends ManagedBeanRegistry> registryClass;
		final Class ctorArgType;
		if ( extendedBeanManagerClass.isInstance( beanManagerRef ) ) {
			registryClass = getHibernateClass( REGISTRY_FQN_EXTENDED );
			ctorArgType = extendedBeanManagerClass;
		}
		else {
			ctorArgType = beanManagerClass;

			final boolean delayAccessToCdi = serviceRegistry.getService( ConfigurationService.class )
					.getSetting( AvailableSettings.DELAY_CDI_ACCESS, StandardConverters.BOOLEAN, false );
			if ( delayAccessToCdi ) {
				registryClass = getHibernateClass( REGISTRY_FQN_DELAYED );
			}
			else {
				registryClass = getHibernateClass( REGISTRY_FQN_STANDARD );
			}
		}

		try {
			final Constructor<? extends ManagedBeanRegistry> ctor = registryClass.getConstructor( ctorArgType );
			try {
				ctor.setAccessible( true );
				return ctor.newInstance( ctorArgType.cast( beanManagerRef ) );
			}
			catch (InvocationTargetException e) {
				throw new HibernateException( "Problem building " + registryClass.getName(), e.getCause() );
			}
			catch (Exception e) {
				throw new HibernateException( "Problem building " + registryClass.getName(), e );
			}
		}
		catch (NoSuchMethodException e) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"Could not locate proper %s constructor",
							registryClass.getName()
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
					ManagedBeanRegistryCdiBuilder.class.getClassLoader()
			);
		}
		catch (ClassNotFoundException e) {
			throw new HibernateException( "Unable to locate Hibernate class by name via reflection : " + fqn, e );
		}
	}
}