/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.service.Service;

/**
 * A registry for {@link ManagedBean} instances.  Responsible for managing the lifecycle.
 * <p>
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
	 * Get a bean reference by class with an explicit fallback bean instance producer.
	 */
	<T> ManagedBean<T> getBean(Class<T> beanContract, BeanInstanceProducer fallbackBeanInstanceProducer);

	/**
	 * Get a bean reference by name and contract with an explicit fallback bean instance producer.
	 */
	<T> ManagedBean<T> getBean(
			String beanName,
			Class<T> beanContract,
			BeanInstanceProducer fallbackBeanInstanceProducer);

	/**
	 * Get a reference to the underlying BeanContainer.  May return {@code null}
	 * indicating that no back-end container has been configured
	 */
	BeanContainer getBeanContainer();
}
