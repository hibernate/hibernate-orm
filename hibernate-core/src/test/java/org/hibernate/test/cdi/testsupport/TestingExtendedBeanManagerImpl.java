/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.testsupport;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

class TestingExtendedBeanManagerImpl
		implements TestingExtendedBeanManager, ExtendedBeanManager {

	private ExtendedBeanManager.LifecycleListener lifecycleListener;

	@Override
	public void registerLifecycleListener(ExtendedBeanManager.LifecycleListener lifecycleListener) {
		if ( this.lifecycleListener != null ) {
			throw new RuntimeException( "LifecycleListener already registered" );
		}
		this.lifecycleListener = lifecycleListener;
	}

	@Override
	public void notifyListenerReady(BeanManager beanManager) {
		lifecycleListener.beanManagerInitialized( beanManager );
	}

	@Override
	public void notifyListenerShuttingDown(BeanManager beanManager) {
		lifecycleListener.beforeBeanManagerDestroyed( beanManager );
	}
}
