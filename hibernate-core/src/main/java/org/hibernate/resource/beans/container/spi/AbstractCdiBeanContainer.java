/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.resource.beans.container.internal.CdiBasedBeanContainer;
import org.hibernate.resource.beans.container.internal.ContainerManagedLifecycleStrategy;
import org.hibernate.resource.beans.container.internal.JpaCompliantLifecycleStrategy;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import static org.hibernate.resource.beans.internal.BeansMessageLogger.BEANS_MSG_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCdiBeanContainer implements CdiBasedBeanContainer {
	private final Map<String,ContainedBeanImplementor<?>> beanCache = new HashMap<>();
	private final List<ContainedBeanImplementor<?>> registeredBeans = new ArrayList<>();

	@Override
	public <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return lifecycleOptions.canUseCachedReferences()
				? getCacheableBean( beanType, lifecycleOptions, fallbackProducer )
				: createBean( beanType, lifecycleOptions, fallbackProducer );
	}

	private <B> ContainedBean<B> getCacheableBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final String beanCacheKey = Helper.determineBeanCacheKey( beanType );

		final ContainedBeanImplementor<?> existing = beanCache.get( beanCacheKey );
		if ( existing != null ) {
			//noinspection unchecked
			return (ContainedBeanImplementor<B>) existing;
		}

		final ContainedBeanImplementor<B> bean = createBean( beanType, lifecycleOptions, fallbackProducer );
		beanCache.put( beanCacheKey, bean );
		return bean;
	}

	private <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBeanImplementor<B> bean = createBean(
				beanType,
				lifecycleOptions.useJpaCompliantCreation()
						? JpaCompliantLifecycleStrategy.INSTANCE
						: ContainerManagedLifecycleStrategy.INSTANCE,
				fallbackProducer
		);
		registeredBeans.add( bean );
		return bean;
	}

	protected abstract <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer);

	@Override
	public <B> ContainedBean<B> getBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return lifecycleOptions.canUseCachedReferences()
				? getCacheableBean( beanName, beanType, lifecycleOptions, fallbackProducer )
				: createBean( beanName, beanType, lifecycleOptions, fallbackProducer );
	}

	private <B> ContainedBeanImplementor<B> getCacheableBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final String beanCacheKey = Helper.determineBeanCacheKey( beanName, beanType );

		final ContainedBeanImplementor<?> existing = beanCache.get( beanCacheKey );
		if ( existing != null ) {
			//noinspection unchecked
			return (ContainedBeanImplementor<B>) existing;
		}

		final ContainedBeanImplementor<B> bean =
				createBean( beanName, beanType, lifecycleOptions, fallbackProducer );
		beanCache.put( beanCacheKey, bean );
		return bean;
	}

	private <B> ContainedBeanImplementor<B> createBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBeanImplementor<B> bean = createBean(
				beanName,
				beanType,
				lifecycleOptions.useJpaCompliantCreation()
						? JpaCompliantLifecycleStrategy.INSTANCE
						: ContainerManagedLifecycleStrategy.INSTANCE,
				fallbackProducer
		);
		registeredBeans.add( bean );
		return bean;
	}

	protected abstract <B> ContainedBeanImplementor<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer);


	protected final void forEachBean(Consumer<ContainedBeanImplementor<?>> consumer) {
		registeredBeans.forEach( consumer );
	}

	@Override
	public final void stop() {
		BEANS_MSG_LOGGER.stoppingBeanContainer( this );
		forEachBean( ContainedBeanImplementor::release );
		registeredBeans.clear();
		beanCache.clear();
	}

}
