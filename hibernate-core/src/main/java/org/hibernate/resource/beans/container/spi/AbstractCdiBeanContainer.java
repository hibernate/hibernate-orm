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
	private final Map<String,ContainedBeanImplementor<?>> beanCache = new HashMap<>();
	private final List<ContainedBeanImplementor<?>> registeredBeans = new ArrayList<>();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// un-named bean support

	@Override
	public final <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return getBean( beanType, lifecycleOptions, fallbackProducer, false );
	}

	@Override
	public final <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		if ( lifecycleOptions.canUseCachedReferences() ) {
			return getCacheableBean( beanType, lifecycleOptions, fallbackProducer, cdiRequiredIfAvailable );
		}
		else {
			return createBean( beanType, lifecycleOptions, fallbackProducer, cdiRequiredIfAvailable );
		}
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBean<B> getCacheableBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		final String beanCacheKey = Helper.determineBeanCacheKey( beanType );

		//noinspection rawtypes
		final ContainedBeanImplementor existing = beanCache.get( beanCacheKey );
		if ( existing != null ) {
			return existing;
		}

		//noinspection rawtypes
		final ContainedBeanImplementor bean = createBean( beanType, lifecycleOptions, fallbackProducer, cdiRequiredIfAvailable );
		beanCache.put( beanCacheKey, bean );
		return bean;
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		//noinspection rawtypes
		final ContainedBeanImplementor bean = createBean(
				beanType,
				lifecycleOptions.useJpaCompliantCreation()
						? JpaCompliantLifecycleStrategy.INSTANCE
						: ContainerManagedLifecycleStrategy.INSTANCE,
				fallbackProducer,
				cdiRequiredIfAvailable
		);
		registeredBeans.add( bean );
		return bean;
	}

	protected abstract <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// named bean support

	@Override
	public final <B> ContainedBean<B> getBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return getBean( beanName, beanType, lifecycleOptions, fallbackProducer, false );
	}

	@Override
	public final <B> ContainedBean<B> getBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		if ( lifecycleOptions.canUseCachedReferences() ) {
			return getCacheableBean( beanName, beanType, lifecycleOptions, fallbackProducer, cdiRequiredIfAvailable );
		}
		else {
			return createBean( beanName, beanType, lifecycleOptions, fallbackProducer, cdiRequiredIfAvailable );
		}
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBeanImplementor<B> getCacheableBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		final String beanCacheKey = Helper.determineBeanCacheKey( beanName, beanType );

		//noinspection rawtypes
		final ContainedBeanImplementor existing = beanCache.get( beanCacheKey );
		if ( existing != null ) {
			return existing;
		}

		//noinspection rawtypes
		final ContainedBeanImplementor bean = createBean( beanName, beanType, lifecycleOptions, fallbackProducer, cdiRequiredIfAvailable );
		beanCache.put( beanCacheKey, bean );
		return bean;
	}

	@SuppressWarnings("unchecked")
	private <B> ContainedBeanImplementor<B> createBean(
			String beanName,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		//noinspection rawtypes
		final ContainedBeanImplementor bean = createBean(
				beanName,
				beanType,
				lifecycleOptions.useJpaCompliantCreation()
						? JpaCompliantLifecycleStrategy.INSTANCE
						: ContainerManagedLifecycleStrategy.INSTANCE,
				fallbackProducer,
				cdiRequiredIfAvailable
		);
		registeredBeans.add( bean );
		return bean;
	}

	protected abstract <B> ContainedBeanImplementor<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable);


	protected final void forEachBean(Consumer<ContainedBeanImplementor<?>> consumer) {
		registeredBeans.forEach( consumer );
	}

	@Override
	public final void stop() {
		BeansMessageLogger.BEANS_MSG_LOGGER.stoppingBeanContainer( this );
		forEachBean( ContainedBeanImplementor::release );
		registeredBeans.clear();
		beanCache.clear();
	}

}
