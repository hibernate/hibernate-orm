/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

import java.util.Collection;
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
import org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.resource.beans.internal.BeansMessageLogger.BEANS_MSG_LOGGER;

/**
 * Standard initializer for the {@link ManagedBeanRegistry} service.
 * Always produces an instance of {@link ManagedBeanRegistryImpl}.
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
			Map<String, Object> configurationValues,
			ServiceRegistryImplementor serviceRegistry) {
		return new ManagedBeanRegistryImpl( resolveBeanContainer( configurationValues, serviceRegistry ) );
	}

	private BeanContainer resolveBeanContainer(Map<?,?> configurationValues, ServiceRegistry serviceRegistry) {
		// was a specific container explicitly specified?
		final Object explicitBeanContainer = configurationValues.get( AvailableSettings.BEAN_CONTAINER );
		return explicitBeanContainer == null
				? interpretImplicitBeanContainer( serviceRegistry )
				: interpretExplicitBeanContainer( explicitBeanContainer, serviceRegistry );
	}

	private static BeanContainer interpretImplicitBeanContainer(ServiceRegistry serviceRegistry) {
		final Collection<BeanContainer> beanContainers =
				serviceRegistry.requireService( ClassLoaderService.class )
						.loadJavaServices( BeanContainer.class );
		return switch ( beanContainers.size() ) {
			case 1 -> beanContainers.iterator().next();
			case 0 -> interpretImplicitCdiBeanContainer( serviceRegistry );
			default -> throw new ServiceException( "Multiple BeanContainer service implementations found"
													+ " (set '" + AvailableSettings.BEAN_CONTAINER + "' explicitly)" );
		};
	}

	// simplified CDI support
	private static BeanContainer interpretImplicitCdiBeanContainer(ServiceRegistry serviceRegistry) {
		final Map<String, Object> settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
		Object beanManager = settings.get( AvailableSettings.CDI_BEAN_MANAGER );
		if ( beanManager == null ) {
			beanManager = settings.get( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER );
		}
		final boolean isCdiAvailable = isCdiAvailable( serviceRegistry );
		if ( beanManager != null ) {
			if ( !isCdiAvailable ) {
				BEANS_MSG_LOGGER.beanManagerButCdiNotAvailable( beanManager );
			}
			return CdiBeanContainerBuilder.fromBeanManagerReference( beanManager, serviceRegistry );
		}
		else {
			if ( isCdiAvailable ) {
				BEANS_MSG_LOGGER.noBeanManagerButCdiAvailable();
			}
			return null;
		}
	}

	private BeanContainer interpretExplicitBeanContainer(Object explicitSetting, ServiceRegistry serviceRegistry) {
		if ( explicitSetting == null ) {
			return null;
		}
		else if ( explicitSetting instanceof BeanContainer beanContainer ) {
			return beanContainer;
		}
		else {
			// otherwise we ultimately need to resolve this to a class
			final Class<?> containerClass = containerClass( explicitSetting, serviceRegistry );
			try {
				return (BeanContainer) containerClass.newInstance();
			}
			catch (Exception e) {
				throw new InstantiationException( "Unable to instantiate specified BeanContainer", containerClass, e );
			}
		}
	}

	private static Class<?> containerClass(Object explicitSetting, ServiceRegistry serviceRegistry) {
		if ( explicitSetting instanceof Class<?> clazz ) {
			return clazz;
		}
		else {
			final String name = explicitSetting.toString();
			// try the StrategySelector service
			final Class<?> selected =
					serviceRegistry.requireService( StrategySelector.class )
							.selectStrategyImplementor( BeanContainer.class, name );
			return selected == null
					? serviceRegistry.requireService( ClassLoaderService.class ).classForName( name )
					: selected;
		}
	}

	// is CDI available on our ClassLoader?
	private static boolean isCdiAvailable(ServiceRegistry serviceRegistry) {
		try {
			serviceRegistry.requireService( ClassLoaderService.class )
					.classForName( "jakarta.enterprise.inject.spi.BeanManager" );
			return true;
		}
		catch (ClassLoadingException e) {
			return false;
		}
	}

}
