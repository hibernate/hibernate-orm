/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.resource.beans.internal.BeansMessageLogger;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCdiBeanContainer implements CdiBasedBeanContainer {
	private Map<String,ContainedBeanImplementor<?>> beanCache = new HashMap<>();
	private List<ContainedBeanImplementor<?>> registeredBeans = new ArrayList<>();

	@Override
	public <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		if ( lifecycleOptions.canUseCachedReferences() ) {
			return getCacheableBean( beanType, lifecycleOptions, fallbackProducer );
		}
		else {
			return createBean( beanType, lifecycleOptions, fallbackProducer );
		}
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBean<B> getCacheableBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final String beanCacheKey = Helper.INSTANCE.determineBeanCacheKey( beanType );

		final ContainedBeanImplementor existing = beanCache.get( beanCacheKey );
		if ( existing != null ) {
			return existing;
		}

		final ContainedBeanImplementor bean = createBean( beanType, lifecycleOptions, fallbackProducer );
		beanCache.put( beanCacheKey, bean );
		return bean;
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBeanImplementor bean = createBean(
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
		if ( lifecycleOptions.canUseCachedReferences() ) {
			return getCacheableBean( beanName, beanType, lifecycleOptions, fallbackProducer );
		}
		else {
			return createBean( beanName, beanType, lifecycleOptions, fallbackProducer );
		}
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBeanImplementor<B> getCacheableBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final String beanCacheKey = Helper.INSTANCE.determineBeanCacheKey( beanName, beanType );

		final ContainedBeanImplementor existing = beanCache.get( beanCacheKey );
		if ( existing != null ) {
			return existing;
		}

		final ContainedBeanImplementor bean = createBean( beanName, beanType, lifecycleOptions, fallbackProducer );
		beanCache.put( beanCacheKey, bean );
		return bean;
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBeanImplementor<B> createBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBeanImplementor bean = createBean(
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


	@SuppressWarnings("WeakerAccess")
	protected final void forEachBean(Consumer<ContainedBeanImplementor<?>> consumer) {
		registeredBeans.forEach( consumer );
	}

	@Override
	public final void stop() {
		BeansMessageLogger.BEANS_LOGGER.stoppingBeanContainer( this );
		forEachBean( ContainedBeanImplementor::release );
		registeredBeans.clear();
		beanCache.clear();
	}

}
