/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import jakarta.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

class TestingExtendedBeanManagerImpl
		implements TestingExtendedBeanManager, ExtendedBeanManager {

	private LifecycleListener lifecycleListener;

	@Override
	public void registerLifecycleListener(LifecycleListener lifecycleListener) {
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
