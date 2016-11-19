/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.cdi.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.cdi.internal.CdiMessageLogger;
import org.hibernate.resource.cdi.internal.ManagedBeanRegistryCdiBuilder;
import org.hibernate.resource.cdi.internal.ManagedBeanRegistryNoCdiImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryInitiator implements StandardServiceInitiator<ManagedBeanRegistry> {
	@Override
	public Class<ManagedBeanRegistry> getServiceInitiated() {
		return ManagedBeanRegistry.class;
	}

	@Override
	public ManagedBeanRegistry initiateService(
			Map configurationValues,
			ServiceRegistryImplementor registry) {
		final ConfigurationService cfgSvc = registry.getService( ConfigurationService.class );
		final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );

		final Object explicitBeanRegistry = cfgSvc.getSettings().get( AvailableSettings.CDI_BEAN_REGISTRY );
		final Object beanManagerRef = cfgSvc.getSettings().get( AvailableSettings.CDI_BEAN_MANAGER );

		// first see if an explicit ManagedBeanRegistry was passed
		if ( explicitBeanRegistry != null ) {
			return interpretExplicitCdiBeanRegistrySetting(
					registry,
					beanManagerRef,
					explicitBeanRegistry
			);
		}

		final boolean isCdiAvailable = isCdiAvailable( classLoaderService );

		if ( beanManagerRef != null ) {
			if ( !isCdiAvailable ) {
				CdiMessageLogger.CDI_LOGGER.beanManagerButCdiNotAvailable( beanManagerRef );
			}

			return ManagedBeanRegistryCdiBuilder.fromBeanManagerReference( beanManagerRef, registry );
		}
		else {
			if ( isCdiAvailable ) {
				CdiMessageLogger.CDI_LOGGER.noBeanManagerButCdiAvailable();
			}
			return new ManagedBeanRegistryNoCdiImpl();
		}
	}

	private static ManagedBeanRegistry interpretExplicitCdiBeanRegistrySetting(
			ServiceRegistry serviceRegistry,
			Object beanManagerRef,
			Object explicitRegistrySetting) {
		// we were passed an explicit ManagedBeanRegistry setting value and we need to interpret it...

		if ( ManagedBeanRegistry.class.isInstance( explicitRegistrySetting ) ) {
			// we were passed a ManagedBeanRegistry instance
			if ( beanManagerRef != null ) {
				CdiMessageLogger.CDI_LOGGER.explicitCdiBeanRegistryInstanceAndBeanManagerReference( explicitRegistrySetting, beanManagerRef );
			}
			return (ManagedBeanRegistry) explicitRegistrySetting;
		}

		// we were passed a Class reference, a Class FQN or a short-name...

		if ( explicitRegistrySetting instanceof Class ) {
			return instantiateExplicitRegistryClass( serviceRegistry, (Class) explicitRegistrySetting, beanManagerRef );
		}

		final String name = explicitRegistrySetting.toString();
		if ( "cdi".equals( name ) ) {
			return ManagedBeanRegistryCdiBuilder.fromBeanManagerReference( beanManagerRef, serviceRegistry );
		}
		else if ( "no-cdi".equals( name ) ) {
			return new ManagedBeanRegistryNoCdiImpl();
		}

		// otherwise assume the name is a FQN
		final Class registryClass = serviceRegistry.getService( ClassLoaderService.class ).classForName( name );
		return instantiateExplicitRegistryClass( serviceRegistry, registryClass, beanManagerRef );

	}

	private static ManagedBeanRegistry instantiateExplicitRegistryClass(
			ServiceRegistry serviceRegistry,
			Class<? extends ManagedBeanRegistry> explicitRegistrySetting,
			Object cdiBeanManagerReference) {
		try {
			// look for a ctor taking CDI's BeanManager
			final Constructor<? extends ManagedBeanRegistry> ctor = explicitRegistrySetting.getConstructor(
					cdiBeanManagerClass( serviceRegistry.getService( ClassLoaderService.class ) )
			);

			try {
				return ctor.newInstance( cdiBeanManagerReference );
			}
			catch (InvocationTargetException e) {
				throw new HibernateException(
						"Unable to instantiate explicitly named ManagedBeanRegistry : " + explicitRegistrySetting.getName(),
						e.getCause()
				);
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to instantiate explicitly named ManagedBeanRegistry : " + explicitRegistrySetting.getName(),
						e
				);
			}
		}
		catch (NoSuchMethodException e) {
			// do nothing, let's fall through to looking for a no-arg ctor
		}

		try {
			return explicitRegistrySetting.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to instantiate explicitly named ManagedBeanRegistry : " + explicitRegistrySetting.getName(),
					e
			);
		}
	}

	private static boolean isCdiAvailable(ClassLoaderService classLoaderService) {
		// is CDI available on our ClassLoader?
		try {
			cdiBeanManagerClass( classLoaderService );
			return true;
		}
		catch (ClassLoadingException e) {
			return false;
		}
	}

	public static Class cdiBeanManagerClass(ClassLoaderService classLoaderService) throws ClassLoadingException {
		return classLoaderService.classForName( "javax.enterprise.inject.spi.BeanManager" );
	}

}