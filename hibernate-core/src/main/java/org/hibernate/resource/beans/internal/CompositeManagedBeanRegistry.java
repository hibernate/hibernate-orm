/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Manageable;
import org.hibernate.service.spi.OptionallyManageable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import org.jboss.logging.Logger;

/**
 * A ManagedBeanRegistry that supports a single primary ManagedBeanRegistry
 * delegate falling back to {@link ManagedBeanRegistryDirectImpl} for direct
 * instantiation (no DI).
 *
 * Note too that it supports all of the optional Service contracts and delegates
 * them to the primary if the primary also implements that particular contract.
 *
 * @author Steve Ebersole
 */
public class CompositeManagedBeanRegistry
		implements ManagedBeanRegistry, Startable, Stoppable, Configurable, ServiceRegistryAwareService, OptionallyManageable {
	private static final Logger log = Logger.getLogger( CompositeManagedBeanRegistry.class );

	private final ManagedBeanRegistry primaryRegistry;
	private final ManagedBeanRegistryDirectImpl fallback = new ManagedBeanRegistryDirectImpl();

	public CompositeManagedBeanRegistry(ManagedBeanRegistry primaryRegistry) {
		this.primaryRegistry = primaryRegistry;
	}

	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass, boolean shouldRegistryManageLifecycle) {
		if ( primaryRegistry != null ) {
			try {
				final ManagedBean<T> bean = primaryRegistry.getBean( beanClass, shouldRegistryManageLifecycle );
				if ( bean != null ) {
					return bean;
				}
			}
			catch (Exception ignore) {
				log.debugf(
						"Error obtaining ManagedBean [%s] from registry [%s] - using fallback registry",
						beanClass.getName(),
						primaryRegistry
				);
			}
		}

		return fallback.getBean( beanClass, shouldRegistryManageLifecycle );
	}

	@Override
	public <T> ManagedBean<T> getBean(String beanName, Class<T> beanContract, boolean shouldRegistryManageLifecycle) {
		if ( primaryRegistry != null ) {
			try {
				final ManagedBean<T> bean = primaryRegistry.getBean( beanName, beanContract,shouldRegistryManageLifecycle );
				if ( bean != null ) {
					return bean;
				}
			}
			catch (Exception ignore) {
				log.debugf(
						"Error obtaining ManagedBean [%s : %s] from registry [%s] - using fallback registry",
						beanName,
						beanContract.getName(),
						primaryRegistry
				);
			}
		}

		return fallback.getBean( beanName, beanContract,shouldRegistryManageLifecycle );
	}

	@Override
	public ManagedBeanRegistry getPrimaryBeanRegistry() {
		return primaryRegistry;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Optional delegation

	@Override
	public void start() {
		if ( primaryRegistry == null ) {
			return;
		}

		if ( Startable.class.isInstance( primaryRegistry ) ) {
			Startable.class.cast( primaryRegistry ).start();
		}
	}

	@Override
	public void configure(Map configurationValues) {
		if ( primaryRegistry == null ) {
			return;
		}

		if ( Configurable.class.isInstance( primaryRegistry ) ) {
			Configurable.class.cast( primaryRegistry ).configure( configurationValues );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		if ( primaryRegistry == null ) {
			return;
		}

		if ( ServiceRegistryAwareService.class.isInstance( primaryRegistry ) ) {
			ServiceRegistryAwareService.class.cast( primaryRegistry ).injectServices( serviceRegistry );
		}
	}

	@Override
	public List<Manageable> getRealManageables() {
		if ( primaryRegistry != null ) {
			if ( Manageable.class.isInstance( primaryRegistry ) ) {
				return Collections.singletonList( (Manageable) primaryRegistry );
			}
		}

		return Collections.emptyList();
	}

	@Override
	public void stop() {
		if ( primaryRegistry == null ) {
			return;
		}

		if ( Stoppable.class.isInstance( primaryRegistry ) ) {
			Stoppable.class.cast( primaryRegistry ).stop();
		}
	}
}
