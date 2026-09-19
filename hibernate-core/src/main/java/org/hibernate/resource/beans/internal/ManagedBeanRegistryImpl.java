/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.container.spi.FallbackContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceAccess;
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
	private final Set<ManagedBean<?>> distinctBeans =
			Collections.newSetFromMap( new IdentityHashMap<>() );

	private final BeanContainer beanContainer;

	private static final BeanContainer.LifecycleOptions DISTINCT_LIFECYCLE_OPTIONS =
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
		this.beanContainer = beanContainer;
	}

	private boolean isContainerBootstrapSafe(){
		return beanContainer == null || beanContainer.isBootstrapSafe();
	}

	@Override
	public BeanContainer getBeanContainer() {
		return beanContainer;
	}

	@Override
	public <T> ManagedBean<T> getBootstrapSafeBean(Class<T> beanClass) {
		if ( isContainerBootstrapSafe() ) {
			return getBean( beanClass );
		}
		final String beanClassName = beanClass.getName();
		//Check if beanClassName already exists
		final var existingBean = registrations.get( beanClassName );
		if ( existingBean != null ) {
			//if beans have same key but different class
			if ( !beanClass.equals( existingBean.getBeanClass() ) ) {
				throw new AssertionFailure( "Wrong bean type: " + beanClassName );
			}
			//noinspection unchecked
			return (ManagedBean<T>) existingBean;
		}
		else {
			final var bean = new DeferredContainerBean<>(
					beanClass,
					beanContainer,
					//the registry itself, implements lifecycle options
					this,
					FallbackBeanInstanceProducer.INSTANCE
			);
			registrations.put( beanClassName, bean );
			return bean;
		}
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
		final String beanClassName = beanClass.getName();
		final var existing = registrations.get( beanClassName );
		if ( existing != null ) {
			if ( !beanClass.equals( existing.getBeanClass() ) ) {
				throw new AssertionFailure( "Wrong type of bean: " + beanClassName );
			}
			//noinspection unchecked (safe since we just checked)
			return (ManagedBean<T>) existing;
		}
		else {
			final var bean = createBean( beanClass, fallbackBeanInstanceProducer );
			registrations.put( beanClassName, bean );
			return bean;
		}
	}

	@Override
	public <T> ManagedBean<? extends T> getBean(String beanName, Class<T> beanContract) {
		return getBean( beanName, beanContract, FallbackBeanInstanceProducer.INSTANCE );
	}

	@Override
	public <T> ManagedBean<? extends T> getBean(
			String beanName,
			Class<T> beanContract,
			BeanInstanceProducer fallbackBeanInstanceProducer) {
		final String key = beanContract.getName() + ':' + beanName;
		final var existing = registrations.get( key );
		if ( existing != null ) {
			if ( !beanContract.isAssignableFrom( existing.getBeanClass() ) ) {
				throw new AssertionFailure( "Wrong type of bean: " + key );
			}
			//noinspection unchecked (safe since we just checked)
			return (ManagedBean<? extends T>) existing;
		}
		else {
			final var bean = createBean( beanName, beanContract, fallbackBeanInstanceProducer );
			registrations.put( key, bean );
			return bean;
		}
	}

	private <T> ManagedBean<T> createBean(Class<T> beanClass, BeanInstanceProducer fallbackBeanInstanceProducer) {
		return beanContainer == null
				? new FallbackContainedBean<>( beanClass, fallbackBeanInstanceProducer )
				: beanContainer.getBean( beanClass, this, fallbackBeanInstanceProducer );
	}

	private <T> ManagedBean<? extends T> createBean(
			String beanName, Class<T> beanContract, BeanInstanceProducer fallbackBeanInstanceProducer) {
		return beanContainer == null
				? new FallbackContainedBean<>( beanName, beanContract, fallbackBeanInstanceProducer )
				: beanContainer.getBean( beanName, beanContract, this, fallbackBeanInstanceProducer );
	}

	@Override
	public <T> ManagedBean<T> getBean(Class<T> beanClass, BeanInstanceAccess access) {
		if ( access == BeanInstanceAccess.REUSE ) {
			return getBean( beanClass );
		}
		final ManagedBean<T> bean = createDistinctBean( beanClass, FallbackBeanInstanceProducer.INSTANCE );
		distinctBeans.add( bean );
		return bean;
	}

	private <T> ManagedBean<T> createDistinctBean(Class<T> beanClass, BeanInstanceProducer fallbackBeanInstanceProducer) {
		return beanContainer == null
				? new FallbackContainedBean<>( beanClass, fallbackBeanInstanceProducer )
				: beanContainer.getBean( beanClass, DISTINCT_LIFECYCLE_OPTIONS, fallbackBeanInstanceProducer );
	}

	@Override
	public void releaseBean(ManagedBean<?> bean) {
		if ( !distinctBeans.remove( bean ) ) {
			return;
		}
		if ( bean instanceof ContainedBeanImplementor<?> containedBean ) {
			containedBean.release();
		}
	}

	@Override
	public void stop() {
		for ( ManagedBean<?> bean : distinctBeans ) {
			if ( bean instanceof ContainedBeanImplementor<?> containedBean ) {
				containedBean.release();
			}
		}
		distinctBeans.clear();

		if ( beanContainer != null ) {
			beanContainer.stop();
		}
		registrations.clear();
	}

}
