/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

import javax.enterprise.inject.spi.BeanManager;

/**
 * @deprecated Use {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager} instead
 */
@Deprecated
public interface ExtendedBeanManager extends org.hibernate.resource.beans.container.spi.ExtendedBeanManager {
	void registerLifecycleListener(LifecycleListener lifecycleListener);

	@Override
	default void registerLifecycleListener(org.hibernate.resource.beans.container.spi.ExtendedBeanManager.LifecycleListener lifecycleListener) {
		/*
		 * Casting the argument to our own LifecycleListener interface won't work here,
		 * since we would be down-casting and the argument may not implement the correct interface.
		 * Just use an adaptor.
		 */
		registerLifecycleListener( new LifecycleListener() {
			@Override
			public void beanManagerInitialized(BeanManager beanManager) {
				lifecycleListener.beanManagerInitialized( beanManager );
			}

			@Override
			public void beforeBeanManagerDestroyed(BeanManager beanManager) {
				lifecycleListener.beforeBeanManagerDestroyed( beanManager );
			}
		} );
	}

	/**
	 * @deprecated Use {@link org.hibernate.resource.beans.container.spi.ExtendedBeanManager.LifecycleListener} instead
	 */
	@Deprecated
	interface LifecycleListener extends org.hibernate.resource.beans.container.spi.ExtendedBeanManager.LifecycleListener {
	}
}
