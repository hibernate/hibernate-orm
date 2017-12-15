/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.spi.jpa;

/**
 * @deprecated Use {@link org.hibernate.resource.beans.spi.ExtendedBeanManager} instead
 */
@Deprecated
public interface ExtendedBeanManager extends org.hibernate.resource.beans.spi.ExtendedBeanManager {

	void registerLifecycleListener(LifecycleListener lifecycleListener);

	@Override
	default void registerLifecycleListener(org.hibernate.resource.beans.spi.ExtendedBeanManager.LifecycleListener lifecycleListener) {
		registerLifecycleListener( lifecycleListener::beanManagerInitialized );
	}

	/**
	 * @deprecated Use {@link org.hibernate.resource.beans.spi.ExtendedBeanManager.LifecycleListener} instead
	 */
	@Deprecated
	interface LifecycleListener extends org.hibernate.resource.beans.spi.ExtendedBeanManager.LifecycleListener {
	}
}
