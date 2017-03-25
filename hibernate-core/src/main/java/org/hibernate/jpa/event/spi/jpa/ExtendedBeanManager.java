/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

import javax.enterprise.inject.spi.BeanManager;

/**
 * This contract and the nested LifecycleListener contract represent the changes
 * we'd like to propose to the CDI spec.  The idea being simply to allow contextual
 * registration of BeanManager lifecycle callbacks
 *
 * @author Steve Ebersole
 */
public interface ExtendedBeanManager {
	/**
	 * Register a BeanManager LifecycleListener
	 *
	 * @param lifecycleListener The listener to register
	 */
	void registerLifecycleListener(LifecycleListener lifecycleListener);

	/**
	 * Contract for things interested in receiving notifications of
	 * BeanManager lifecycle events.
	 * <p/>
	 * A "beanManagerDestroyed" notifications would probably also be generally
	 * useful, although we do not need it here and not sure WildFly can really
	 * tell us that reliably.
	 */
	interface LifecycleListener {
		void beanManagerInitialized(BeanManager beanManager);
	}
}
