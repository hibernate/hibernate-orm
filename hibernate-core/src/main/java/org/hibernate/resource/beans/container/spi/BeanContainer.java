/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

import org.hibernate.SPI;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.spi.Stoppable;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/**
 * Abstracts any kind of container for managed beans, for example,
 * the CDI {@link jakarta.enterprise.inject.spi.BeanManager}. A
 * custom bean container may be integrated with Hibernate by
 * implementing this interface and specifying the implementation
 * using {@value org.hibernate.cfg.AvailableSettings#BEAN_CONTAINER}.
 *
 * @see org.hibernate.cfg.AvailableSettings#BEAN_CONTAINER
 * @see org.hibernate.resource.beans.container.internal.CdiBasedBeanContainer
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface BeanContainer extends Stoppable {
	/// Stop the container after all acquisition, access, and release operations
	/// have completed. Shutdown need not support concurrent calls.
	@Override
	void stop();

	/// Acquisition policy read by container implementations. Integrations calling
	/// this container directly may implement and supply their own options.
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public interface LifecycleOptions {
		/// Whether a cached handle may be reused. When false, bypass reference caches.
		boolean canUseCachedReferences();
		/// Whether instances have independent, Hibernate-managed lifetimes rather
		/// than the contextual lifetimes supplied by the backing container.
		boolean useJpaCompliantCreation();
	}

	<B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer);

	<B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer);

	/// Acquire a handle without requiring the backing container to be ready.
	/// Accessing the instance requires the backing container to be ready.
	<B> ContainedBean<B> getBootstrapSafeBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer);

	/// Unregister and destroy a bean owned by this container. Repeated releases
	/// and releases of foreign handles have no effect. Callers must finish using
	/// the bean before releasing it, including all users of a shared bean.
	void releaseBean(ManagedBean<?> bean);

	/// Whether this container obtains beans exclusively through the fallback producer.
	default boolean isFallback() {
		return false;
	}
}
