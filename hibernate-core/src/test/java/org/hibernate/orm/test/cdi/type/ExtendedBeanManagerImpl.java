/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
