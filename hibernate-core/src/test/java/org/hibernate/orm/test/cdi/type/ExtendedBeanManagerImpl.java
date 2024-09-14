/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cdi.type;

import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

import jakarta.enterprise.inject.spi.BeanManager;

/**
 * @author Steve Ebersole
 */
public class ExtendedBeanManagerImpl implements ExtendedBeanManager {
	private LifecycleListener lifecycleListener;

	@Override
	public void registerLifecycleListener(LifecycleListener lifecycleListener) {
		this.lifecycleListener = lifecycleListener;
	}

	public void injectBeanManager(BeanManager beanManager) {
		lifecycleListener.beanManagerInitialized( beanManager );
	}
}
