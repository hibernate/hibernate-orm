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
	 * Form of {@link #getBean(Class)} allowing to indicate whether
	 * it is required to use CDI if it is available
	 *
	 * @implNote Defaulted for backwards compatibility
	 */
	default <T> ManagedBean<T> getBean(Class<T> beanClass, boolean cdiRequiredIfAvailable) {
		return getBean( beanClass );
	}

	/**
	 * Get a bean reference by class with an explicit fallback bean instance producer.
	 */
	<T> ManagedBean<T> getBean(Class<T> beanContract, BeanInstanceProducer fallbackBeanInstanceProducer);

	/**
	 * Form of {@link #getBean(Class, BeanInstanceProducer)} allowing to indicate whether
	 * it is required to use CDI if it is available
	 *
	 * @implNote Defaulted for backwards compatibility
	 */
	default <T> ManagedBean<T> getBean(
			Class<T> beanContract,
			BeanInstanceProducer fallbackBeanInstanceProducer,
			boolean cdiRequiredIfAvailable) {
		return getBean( beanContract, fallbackBeanInstanceProducer );
	}

	/**
	 * Get a bean reference by name and contract.
	 */
	<T> ManagedBean<T> getBean(String beanName, Class<T> beanContract);

	/**
	 * Form of {@link #getBean(String,Class)} allowing to indicate whether
	 * it is required to use CDI if it is available
	 *
	 * @implNote Defaulted for backwards compatibility
	 */
	default <T> ManagedBean<T> getBean(String beanName, Class<T> beanContract, boolean cdiRequiredIfAvailable) {
		return getBean( beanName, beanContract );
	}

	/**
	 * Get a bean reference by name and contract with an explicit fallback bean instance producer.
	 */
	<T> ManagedBean<T> getBean(
			String beanName,
			Class<T> beanContract,
			BeanInstanceProducer fallbackBeanInstanceProducer);

	/**
	 * Form of {@link #getBean(String, Class, BeanInstanceProducer)} allowing to indicate whether
	 * it is required to use CDI if it is available
	 *
	 * @implNote Defaulted for backwards compatibility
	 */
	default <T> ManagedBean<T> getBean(
			String beanName,
			Class<T> beanContract,
			BeanInstanceProducer fallbackBeanInstanceProducer,
			boolean cdiRequiredIfAvailable) {
		return getBean( beanName, beanContract, fallbackBeanInstanceProducer );
	}

	/**
	 * Get a reference to the underlying BeanContainer.  May return {@code null}
	 * indicating that no back-end container has been configured
	 */
	BeanContainer getBeanContainer();
}
