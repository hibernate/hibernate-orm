/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
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

		final ManagedBean<T> bean;
		if ( beanContainer == null ) {
			bean = new FallbackContainedBean<>( beanClass, fallbackBeanInstanceProducer );
		}
		else {
			final ContainedBean<T> containedBean = beanContainer.getBean(
					beanClass,
					this,
					fallbackBeanInstanceProducer
			);

			if ( containedBean instanceof ManagedBean ) {
				//noinspection unchecked
				bean = (ManagedBean<T>) containedBean;
			}
			else {
				bean = new ContainedBeanManagedBeanAdapter<>( beanClass, containedBean );
			}
		}

		registrations.put( beanClass.getName(), bean );

		return bean;
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

		final ManagedBean<T> bean;
		if ( beanContainer == null ) {
			bean = new FallbackContainedBean<>( beanName, beanContract, fallbackBeanInstanceProducer );
		}
		else {
			final ContainedBean<T> containedBean = beanContainer.getBean(
					beanName,
					beanContract,
					this,
					fallbackBeanInstanceProducer
			);

			if ( containedBean instanceof ManagedBean ) {
				//noinspection unchecked
				bean = (ManagedBean<T>) containedBean;
			}
			else {
				bean = new ContainedBeanManagedBeanAdapter<>( beanContract, containedBean );
			}
		}

		registrations.put( key, bean );

		return bean;
	}

	@Override
	public void stop() {
		if ( beanContainer != null ) {
			beanContainer.stop();
		}
		registrations.clear();
	}

	private static class ContainedBeanManagedBeanAdapter<B> implements ManagedBean<B> {
		private final Class<B> beanClass;
		private final ContainedBean<B> containedBean;

		private ContainedBeanManagedBeanAdapter(Class<B> beanClass, ContainedBean<B> containedBean) {
			this.beanClass = beanClass;
			this.containedBean = containedBean;
		}

		@Override
		public Class<B> getBeanClass() {
			return beanClass;
		}

		@Override
		public B getBeanInstance() {
			return containedBean.getBeanInstance();
		}
	}
}
