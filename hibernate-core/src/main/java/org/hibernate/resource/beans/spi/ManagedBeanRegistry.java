/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.service.Service;

/**
 * A registry for ManagedBean instances.  Responsible for managing the lifecycle.
 * <p/>
 * Access to the beans and usage of them are only valid between the time
 * the registry is initialized and released (however those events are recognized).
 *
 * @author Steve Ebersole
 */
public interface ManagedBeanRegistry extends Service {
	/**
	 * Get a bean reference, by class.
	 *
	 * @apiNote `shouldRegistryManageLifecycle` has multiple implications that are
	 * important to understand.  First it indicates whether the registry should
	 * handle releasing (end lifecycle) the bean or whether the caller will handle
	 * that.  It also indicates whether cached references will be returned, or whether
	 * new references will be returned each time.  `true` means that cached references
	 * can be returned and the registry will handle releasing when the registry is itself
	 * released (generally when the SessionFactory is closed).  `false` means that
	 * new references should be returned for every call and the caller will handle
	 * the release calls itself.
	 */
	<T> ManagedBean<T> getBean(Class<T> beanClass, boolean shouldRegistryManageLifecycle);

	/**
	 * Get a bean reference by name and contract.
	 *
	 * @apiNote `shouldRegistryManageLifecycle` has multiple implications that are
	 * important to understand.  First it indicates whether the registry should
	 * handle releasing (end lifecycle) the bean or whether the caller will handle
	 * that.  It also indicates whether cached references will be returned, or whether
	 * new references will be returned each time.  `true` means that cached references
	 * can be returned and the registry will handle releasing when the registry is itself
	 * released (generally when the SessionFactory is closed).  `false` means that
	 * new references should be returned for every call and the caller will handle
	 * the release calls itself.
	 */
	<T> ManagedBean<T> getBean(String beanName, Class<T> beanContract, boolean shouldRegistryManageLifecycle);

	default ManagedBeanRegistry getPrimaryBeanRegistry() {
		return this;
	}
}
