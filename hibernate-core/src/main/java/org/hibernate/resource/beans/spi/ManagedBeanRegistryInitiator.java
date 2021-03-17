/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.spi;

import java.util.Map;

import org.hibernate.InstantiationException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerBuilder;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.BeansMessageLogger;
import org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Hibernate's standard initializer for the ManagedBeanRegistry service.
 *
 * Produces a {@link org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl}
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
		return new ManagedBeanRegistryImpl( resolveBeanContainer( configurationValues, serviceRegistry ) );
	}

	private BeanContainer resolveBeanContainer(Map configurationValues, ServiceRegistryImplementor serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		final ConfigurationService cfgSvc = serviceRegistry.getService( ConfigurationService.class );

		// was a specific container explicitly specified?
		final Object explicitBeanContainer = configurationValues.get( AvailableSettings.BEAN_CONTAINER );
		if ( explicitBeanContainer != null ) {
			return interpretExplicitBeanContainer( explicitBeanContainer, classLoaderService, serviceRegistry );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// simplified CDI support

		final boolean isCdiAvailable = isCdiAvailable( classLoaderService );
		final Object beanManagerRef = cfgSvc.getSettings().get( AvailableSettings.CDI_BEAN_MANAGER );
		if ( beanManagerRef != null ) {
			if ( !isCdiAvailable ) {
				BeansMessageLogger.BEANS_LOGGER.beanManagerButCdiNotAvailable( beanManagerRef );
			}

			return CdiBeanContainerBuilder.fromBeanManagerReference( beanManagerRef, serviceRegistry );
		}
		else {
			if ( isCdiAvailable ) {
				BeansMessageLogger.BEANS_LOGGER.noBeanManagerButCdiAvailable();
			}
		}

		return null;
	}

	private BeanContainer interpretExplicitBeanContainer(
			Object explicitSetting,
			ClassLoaderService classLoaderService, ServiceRegistryImplementor serviceRegistry) {
		if ( explicitSetting == null ) {
			return null;
		}

		if ( explicitSetting instanceof BeanContainer ) {
			return (BeanContainer) explicitSetting;
		}

		// otherwise we ultimately need to resolve this to a class
		final Class containerClass;
		if ( explicitSetting instanceof Class ) {
			containerClass = (Class) explicitSetting;
		}
		else {
			final String name = explicitSetting.toString();
			// try the StrategySelector service
			final Class selected = serviceRegistry.getService( StrategySelector.class )
					.selectStrategyImplementor( BeanContainer.class, name );
			if ( selected != null ) {
				containerClass = selected;
			}
			else {
				containerClass = classLoaderService.classForName( name );
			}
		}

		try {
			return (BeanContainer) containerClass.newInstance();
		}
		catch (Exception e) {
			throw new InstantiationException( "Unable to instantiate specified BeanContainer : " + containerClass.getName(), containerClass, e );
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
