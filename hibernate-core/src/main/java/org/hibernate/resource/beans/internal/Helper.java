/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.internal.ContainerManagedLifecycleStrategy;
import org.hibernate.resource.beans.container.internal.JpaCompliantLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class Helper {
	/**
	 * Singleton access
	 */
	public static final Helper INSTANCE = new Helper();

	private Helper() {
	}

	public String determineBeanCacheKey(Class<?> beanType) {
		return beanType.getName();
	}

	public String determineBeanCacheKey(String name, Class<?> beanType) {
		return beanType.getName() + ':' + name;
	}

	public boolean shouldIgnoreBeanContainer(ServiceRegistry serviceRegistry) {
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );
		final Object beanManagerRef = configService.getSettings().get( AvailableSettings.JAKARTA_CDI_BEAN_MANAGER );

		if ( beanManagerRef instanceof ExtendedBeanManager ) {
			return true;
		}

		if ( configService.getSetting( AvailableSettings.DELAY_CDI_ACCESS, StandardConverters.BOOLEAN, false ) ) {
			return true;
		}

		return false;
	}

	@SuppressWarnings("unused")
	public BeanLifecycleStrategy getLifecycleStrategy(boolean shouldRegistryManageLifecycle) {
		return shouldRegistryManageLifecycle
				? JpaCompliantLifecycleStrategy.INSTANCE
				: ContainerManagedLifecycleStrategy.INSTANCE;
	}
}
