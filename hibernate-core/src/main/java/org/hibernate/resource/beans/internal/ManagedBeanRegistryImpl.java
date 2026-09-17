/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.resource.beans.container.internal.FallbackBeanContainerImpl;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.spi.BeanInstanceCaching;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.Stoppable;

/**
 * Applies Hibernate's acquisition policies to the configured bean container.
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryImpl implements ManagedBeanRegistry, BeanContainer.LifecycleOptions, Stoppable {
	private final BeanContainer beanContainer;

	private static final BeanContainer.LifecycleOptions UNCACHED_LIFECYCLE_OPTIONS =
			new BeanContainer.LifecycleOptions() {
				@Override
				public boolean canUseCachedReferences() {
					return false;
				}
				@Override
				public boolean useJpaCompliantCreation() {
					return true;
				}
			};


	public ManagedBeanRegistryImpl(BeanContainer beanContainer) {
		this.beanContainer = beanContainer == null ? new FallbackBeanContainerImpl() : beanContainer;
	}

	@Override
	@Nonnull
	public BeanContainer getBeanContainer() {
		return beanContainer;
	}

	@Override
	public boolean canUseCachedReferences() {
		return true;
	}

	@Override
	public boolean useJpaCompliantCreation() {
		return true;
	}

	@Override
	public <T> ManagedBean<T> getBootstrapSafeBean(Class<T> beanClass) {
		return beanContainer.getBootstrapSafeBean( beanClass, this, FallbackBeanInstanceProducer.INSTANCE );
	}

	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass) {
		return getBean( beanClass, FallbackBeanInstanceProducer.INSTANCE );
	}

	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass, BeanInstanceProducer producer) {
		return beanContainer.getBean( beanClass, this, producer );
	}

	@Override
	public <T> ManagedBean<? extends T> getBean(String name, Class<T> beanContract) {
		return getBean( name, beanContract, FallbackBeanInstanceProducer.INSTANCE );
	}

	@Override
	public <T> ManagedBean<? extends T> getBean(String name, Class<T> beanContract, BeanInstanceProducer producer) {
		return beanContainer.getBean( name, beanContract, this, producer );
	}

	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass, BeanInstanceCaching caching) {
		return beanContainer.getBean( beanClass,
				caching == BeanInstanceCaching.ALLOW ? this : UNCACHED_LIFECYCLE_OPTIONS,
				FallbackBeanInstanceProducer.INSTANCE );
	}

	@Override
	public void releaseBean(ManagedBean<?> bean) {
		beanContainer.releaseBean( bean );
	}

	@Override
	public void stop() {
		beanContainer.stop();
	}
}
