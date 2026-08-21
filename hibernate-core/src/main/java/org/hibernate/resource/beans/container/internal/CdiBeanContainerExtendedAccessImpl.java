/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.inject.spi.BeanManager;
import org.hibernate.Internal;
import org.hibernate.resource.beans.container.spi.AbstractCdiBeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;


import static org.hibernate.resource.beans.internal.BeansMessageLogger.BEANS_MSG_LOGGER;

/**
 * @author Steve Ebersole
 */
public class CdiBeanContainerExtendedAccessImpl
		extends AbstractCdiBeanContainer
		implements ExtendedBeanManager.LifecycleListener {

	// NOTE : we continue to use the deprecated form for now since that is what WildFly needs for the time being

	private BeanManager usableBeanManager;

	CdiBeanContainerExtendedAccessImpl(ExtendedBeanManager beanManager) {
		beanManager.registerLifecycleListener( this );
		BEANS_MSG_LOGGER.extendedAccessToBeanManager();
	}

	@Override
	protected <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		if ( usableBeanManager == null ) {
			return new DelayedBeanImpl<>( beanType, lifecycleStrategy, fallbackProducer, DUMMY_BEAN_CONTAINER);
		}
		else {
			return lifecycleStrategy.createBean( beanType, fallbackProducer, this );
		}
	}

	@Override
	protected <B> ContainedBeanImplementor<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		if ( usableBeanManager == null ) {
			return new NamedDelayedBeanImpl<>(
					name,
					beanType,
					lifecycleStrategy,
					fallbackProducer,
					DUMMY_BEAN_CONTAINER
			);
		}
		else {
			return lifecycleStrategy.createBean( name, beanType, fallbackProducer, this );
		}
	}

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		this.usableBeanManager = beanManager;
		forEachBean( ContainedBeanImplementor::initialize );
	}

	@Override
	public void beforeBeanManagerDestroyed(BeanManager beanManager) {
		stop();
		this.usableBeanManager = null;
	}

	@Override
	public BeanManager getUsableBeanManager() {
		if ( usableBeanManager == null ) {
			throw new IllegalStateException( "ExtendedBeanManager.LifecycleListener callback not yet called: CDI not (yet) usable" );
		}
		return usableBeanManager;
	}

	@Internal
	public BeanManager getBeanManager() {
		return usableBeanManager;
	}

	private final CdiBasedBeanContainer DUMMY_BEAN_CONTAINER = new CdiBasedBeanContainer() {
		@Override
		public BeanManager getUsableBeanManager() {
			return usableBeanManager;
		}

		@Override
		public <B> ContainedBean<B> getBean(
				Class<B> beanType,
				LifecycleOptions lifecycleOptions,
				BeanInstanceProducer fallbackProducer) {
			// todo (5.3) : should this throw an exception instead?
			return CdiBeanContainerExtendedAccessImpl.this.getBean( beanType, lifecycleOptions, fallbackProducer );
		}

		@Override
		public <B> ContainedBean<B> getBean(
				String beanName,
				Class<B> beanType,
				LifecycleOptions lifecycleOptions,
				BeanInstanceProducer fallbackProducer) {
			// todo (5.3) : should this throw an exception instead?
			return CdiBeanContainerExtendedAccessImpl.this.getBean( beanName, beanType, lifecycleOptions, fallbackProducer );
		}

		@Override
		public void stop() {
		}
	};
}
