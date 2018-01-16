/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.resource.beans.container.internal.CdiBasedBeanContainer;
import org.hibernate.resource.beans.internal.BeansMessageLogger;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBeanContainer implements CdiBasedBeanContainer {
	private Map<String,ContainedBeanImplementor<?>> registrations = new HashMap<>();

	@Override
	public final void registerContainedBean(String key, ContainedBeanImplementor bean) {
		registrations.put( key, bean );
	}

	@Override
	public final ContainedBeanImplementor findRegistered(String key) {
		return registrations.get( key );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final <B> ContainedBean<B> getBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBean existing = lifecycleStrategy.findRegisteredBean( beanType, this );
		if ( existing != null ) {
			return existing;
		}

		return createBean( beanType, lifecycleStrategy, fallbackProducer );
	}

	protected abstract <B> ContainedBean<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer);

	@Override
	@SuppressWarnings("unchecked")
	public final <B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBean existing = lifecycleStrategy.findRegisteredBean( name, beanType, this );
		if ( existing != null ) {
			return existing;
		}

		return createBean( name, beanType, lifecycleStrategy, fallbackProducer );
	}

	protected abstract <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer);


	@SuppressWarnings("WeakerAccess")
	protected final void forEachBean(Consumer<ContainedBeanImplementor<?>> consumer) {
		registrations.values().forEach( consumer );
	}

	@Override
	public final void stop() {
		BeansMessageLogger.BEANS_LOGGER.stoppingBeanContainer( this );
		forEachBean( ContainedBeanImplementor::release );
		registrations.clear();
	}

}
