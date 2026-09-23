/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.SPI;
import org.hibernate.Incubating;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.service.Service;

import static org.hibernate.SPI.Role.USE;

/**
 * A registry for {@link ManagedBean} instances. Responsible for managing the lifecycle.
 * Integrations obtain and use this service to acquire and release bean handles.
 * <p>
 * Access to the beans and usage of them are only valid between the time the registry is
 * initialized and released (however those events are recognized).
 *
 * @author Steve Ebersole
 */
@SPI(USE)
public interface ManagedBeanRegistry extends Service {
	/**
	 * Get a reference to the underlying {@link BeanContainer}.
	 */
	@Nonnull
	BeanContainer getBeanContainer();

	/**
	 * Get a bean reference by class.
	 */
	<T> ManagedBean<T> getBean(Class<T> beanClass);

	/**
	 * Get a bean reference by name and contract.
	 */
	<T> ManagedBean<? extends T> getBean(String beanName, Class<T> beanContract);

	/**
	 * Get a bean reference by class with an explicit fallback bean instance producer.
	 */
	<T> ManagedBean<T> getBean(Class<T> beanClass, BeanInstanceProducer fallbackBeanInstanceProducer);

	/**
	 * Get a bean reference by name and contract with an explicit fallback bean instance producer.
	 */
	<T> ManagedBean<? extends T> getBean(
			String beanName,
			Class<T> beanContract,
			BeanInstanceProducer fallbackBeanInstanceProducer);

	/**
	 * Get a bean reference that is safe to acquire during bootstrap.
	 *
	 * @since 8.0
	 */
	@Incubating(since = "8.0", group = "bootstrap-safe-beans")
	<T> ManagedBean<T> getBootstrapSafeBean(Class<T> beanClass);

	/**
	 * Get a bean reference that is safe to acquire during bootstrap,
	 * with control over bean instance caching.
	 *
	 * @since 8.0
	 */
	@Incubating(since = "8.0", group = "bootstrap-safe-beans")
	<T> ManagedBean<T> getBootstrapSafeBean(Class<T> beanClass, BeanInstanceCaching caching);

	/**
	 * Get a bean reference by class, with control over bean instance caching.
	 * <p>
	 * With {@link BeanInstanceCaching#DISALLOW}, the caller should invoke
	 * {@link #releaseBean(ManagedBean)} when it has finished using the returned
	 * bean. Container shutdown provides fallback cleanup for unreleased beans.
	 * <p>
	 * With {@link BeanInstanceCaching#ALLOW}, the returned bean may be shared
	 * with other callers and is normally released at container shutdown.
	 * Only explicitly release it when all callers have finished using it.
	 *
	 * @since 8.0
	 */
	@Incubating(since = "8.0", group = "bootstrap-safe-beans")
	<T> ManagedBean<T> getBean(Class<T> beanClass, BeanInstanceCaching caching);

	/**
	 * Release and unregister a bean previously obtained from this registry.
	 *
	 * @since 8.0
	 */
	@Incubating(since = "8.0", group = "bootstrap-safe-beans")
	void releaseBean(ManagedBean<?> bean);

}
