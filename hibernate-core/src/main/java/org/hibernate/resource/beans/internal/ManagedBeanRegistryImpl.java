/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.FallbackContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.spi.Stoppable;

/**
 * Abstract support (template pattern) for {@link ManagedBeanRegistry} implementations
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryImpl implements ManagedBeanRegistry, BeanContainer.LifecycleOptions, Stoppable {
	private final Map<String,ManagedBean<?>> registrations = new HashMap<>();

	private final BeanContainer beanContainer;

	public ManagedBeanRegistryImpl(BeanContainer beanContainer) {
		this.beanContainer = beanContainer;
	}

	@Override
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
	public <T> ManagedBean<T> getBean(Class<T> beanClass) {
		return getBean( beanClass, FallbackBeanInstanceProducer.INSTANCE );
	}

	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass, BeanInstanceProducer fallbackBeanInstanceProducer) {
		final ManagedBean<?> existing = registrations.get( beanClass.getName() );
		if ( existing != null ) {
			//noinspection unchecked
			return (ManagedBean<T>) existing;
		}
		else {
			final ManagedBean<T> bean = createBean( beanClass, fallbackBeanInstanceProducer );
			registrations.put( beanClass.getName(), bean );
			return bean;
		}
	}

	@Override
	public <T> ManagedBean<T> getBean(String beanName, Class<T> beanContract) {
		return getBean( beanName, beanContract, FallbackBeanInstanceProducer.INSTANCE );
	}

	@Override
	public <T> ManagedBean<T> getBean(
			String beanName,
			Class<T> beanContract,
			BeanInstanceProducer fallbackBeanInstanceProducer) {
		final String key = beanContract.getName() + ':' + beanName;
		final ManagedBean<?> existing = registrations.get( key );
		if ( existing != null ) {
			//noinspection unchecked
			return (ManagedBean<T>) existing;
		}
		else {
			final ManagedBean<T> bean = createBean( beanName, beanContract, fallbackBeanInstanceProducer );
			registrations.put( key, bean );
			return bean;
		}
	}

	private <T> ManagedBean<T> createBean(Class<T> beanClass, BeanInstanceProducer fallbackBeanInstanceProducer) {
		return beanContainer == null
				? new FallbackContainedBean<>( beanClass, fallbackBeanInstanceProducer )
				: beanContainer.getBean( beanClass, this, fallbackBeanInstanceProducer );
	}

	private <T> ManagedBean<T> createBean(
			String beanName, Class<T> beanContract, BeanInstanceProducer fallbackBeanInstanceProducer) {
		return beanContainer == null
				? new FallbackContainedBean<>( beanName, beanContract, fallbackBeanInstanceProducer )
				: beanContainer.getBean( beanName, beanContract, this, fallbackBeanInstanceProducer );
	}

	@Override
	public void stop() {
		if ( beanContainer != null ) {
			beanContainer.stop();
		}
		registrations.clear();
	}
}
