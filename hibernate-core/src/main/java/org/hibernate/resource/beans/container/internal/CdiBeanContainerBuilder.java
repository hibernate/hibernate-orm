/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.inject.spi.BeanManager;
import org.hibernate.AssertionFailure;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.cfg.ManagedBeanSettings.DELAY_CDI_ACCESS;

/**
 * Helper class for building a CDI-based {@link BeanContainer}.
 *
 * @author Steve Ebersole
 */
public class CdiBeanContainerBuilder {

	public static BeanContainer fromBeanManagerReference(Object beanManager, ServiceRegistry serviceRegistry) {
		if ( beanManager instanceof ExtendedBeanManager extendedBeanManager ) {
			return new CdiBeanContainerExtendedAccessImpl( extendedBeanManager );
		}
		else if ( beanManager instanceof BeanManager cdiBeanManager ) {
			return delayCdiAccess( serviceRegistry )
					? new CdiBeanContainerDelayedAccessImpl( cdiBeanManager )
					: new CdiBeanContainerImmediateAccessImpl( cdiBeanManager );
		}
		else {
			throw new AssertionFailure( "Unsupported bean manager: " + beanManager );
		}
	}

	private static boolean delayCdiAccess(ServiceRegistry serviceRegistry) {
		return serviceRegistry.requireService( ConfigurationService.class )
				.getSetting( DELAY_CDI_ACCESS, StandardConverters.BOOLEAN, false );
	}
}
