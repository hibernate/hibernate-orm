/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.resource.beans.container.spi.BeanContainer;
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
	 * Get a bean reference by class.
	 */
	<T> ManagedBean<T> getBean(Class<T> beanClass);

	/**
	 * Get a bean reference by name and contract.
	 */
	<T> ManagedBean<T> getBean(String beanName, Class<T> beanContract);

	/**
	 * Get a reference to the underlying BeanContainer.  May return {@code null}
	 * indicating that no back-end container has been configured
	 */
	BeanContainer getBeanContainer();
}
