/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.spi.AbstractManagedBeanRegistry;
import org.hibernate.resource.beans.spi.ManagedBean;

import org.jboss.logging.Logger;

/**
 * A CDI-based ManagedBeanRegistry for Hibernate following the JPA standard
 * prescribed manner... namely assuming immediate access to the BeanManager is
 * allowed.
 *
 * @see ManagedBeanRegistryCdiExtendedImpl
 * @see ManagedBeanRegistryCdiDelayedImpl
 *
 * @author Steve Ebersole
 */
class ManagedBeanRegistryCdiStandardImpl extends AbstractManagedBeanRegistry {
	private static final Logger log = Logger.getLogger( ManagedBeanRegistryCdiStandardImpl.class );

	private final BeanManager beanManager;

	private ManagedBeanRegistryCdiStandardImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
		log.debugf( "Standard access requested to CDI BeanManager : " + beanManager );
	}

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass, boolean shouldRegistryManageLifecycle) {
		return Helper.INSTANCE.getLifecycleManagementStrategy( shouldRegistryManageLifecycle )
				.createBean( beanManager, beanClass );
	}

	@Override
	protected <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract, boolean shouldRegistryManageLifecycle) {
		return Helper.INSTANCE.getLifecycleManagementStrategy( shouldRegistryManageLifecycle )
				.createBean( beanManager, beanName, beanContract );
	}
}
