/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.spi;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.beans.internal.BeansMessageLogger;
import org.hibernate.resource.beans.internal.CompositeManagedBeanRegistry;
import org.hibernate.resource.beans.internal.ManagedBeanRegistryCdiBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Hibernate's standard initializer for the ManagedBeanRegistry service.
 *
 * Produces a {@link CompositeManagedBeanRegistry}
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryInitiator implements StandardServiceInitiator<ManagedBeanRegistry> {
	/**
	 * Singleton access
	 */
	public static final ManagedBeanRegistryInitiator INSTANCE = new ManagedBeanRegistryInitiator();

	@Override
	public Class<ManagedBeanRegistry> getServiceInitiated() {
		return ManagedBeanRegistry.class;
	}

	@Override
	public ManagedBeanRegistry initiateService(
			Map configurationValues,
			ServiceRegistryImplementor serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		final ConfigurationService cfgSvc = serviceRegistry.getService( ConfigurationService.class );

		ManagedBeanRegistry primaryCdiBasedRegistry = null;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// apply CDI support

		final boolean isCdiAvailable = isCdiAvailable( classLoaderService );
		final Object beanManagerRef = cfgSvc.getSettings().get( AvailableSettings.CDI_BEAN_MANAGER );
		if ( beanManagerRef != null ) {
			if ( !isCdiAvailable ) {
				BeansMessageLogger.BEANS_LOGGER.beanManagerButCdiNotAvailable( beanManagerRef );
			}

			primaryCdiBasedRegistry = ManagedBeanRegistryCdiBuilder.fromBeanManagerReference( beanManagerRef, serviceRegistry );
		}
		else {
			if ( isCdiAvailable ) {
				BeansMessageLogger.BEANS_LOGGER.noBeanManagerButCdiAvailable();
			}
		}

		return new CompositeManagedBeanRegistry( primaryCdiBasedRegistry );
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
