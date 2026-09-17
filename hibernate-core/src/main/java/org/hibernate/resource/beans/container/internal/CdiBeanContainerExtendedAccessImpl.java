/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.inject.spi.BeanManager;
import org.hibernate.Internal;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;


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
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		if ( usableBeanManager == null ) {
			return new BeanImpl<>( beanType, lifecycleStrategy, fallbackProducer );
		}
		else {
			return lifecycleStrategy.createBean( beanType, fallbackProducer, this );
		}
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		if ( usableBeanManager == null ) {
			return new NamedBeanImpl<>(
					name,
					beanType,
					lifecycleStrategy,
					fallbackProducer
			);
		}
		else {
			return lifecycleStrategy.createBean( name, beanType, fallbackProducer, this );
		}
	}

	@Override
	protected <B> ContainedBean<B> createBootstrapSafeBean(
			Class<B> beanType, LifecycleOptions options, BeanInstanceProducer producer) {
		return createBean( beanType, options, producer );
	}

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		this.usableBeanManager = beanManager;
		forEachBean( ContainedBean::initialize );
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

	private class BeanImpl<B> implements ContainedBean<B> {
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBean<B> delegateContainedBean;

		private BeanImpl(
				Class<B> beanType,
				BeanLifecycleStrategy lifecycleStrategy,
				BeanInstanceProducer fallbackProducer) {
			this.beanType = beanType;
			this.lifecycleStrategy = lifecycleStrategy;
			this.fallbackProducer = fallbackProducer;
		}

		@Override
		public Class<B> getBeanClass() {
			return beanType;
		}

		@Override
		public synchronized void initialize() {
			if ( delegateContainedBean == null ) {
				delegateContainedBean = lifecycleStrategy.createBean( beanType, fallbackProducer, DUMMY_BEAN_CONTAINER );
			}
			delegateContainedBean.initialize();
		}

		@Override
		public synchronized B getBeanInstance() {
			if ( delegateContainedBean == null ) {
				initialize();
			}
			return delegateContainedBean.getBeanInstance();
		}

		@Override
		public synchronized void release() {
			if ( delegateContainedBean != null ) {
				delegateContainedBean.release();
			}
			delegateContainedBean = null;
		}
	}

	private class NamedBeanImpl<B> implements ContainedBean<B> {
		private final String name;
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBean<B> delegateContainedBean;

		private NamedBeanImpl(
				String name,
				Class<B> beanType,
				BeanLifecycleStrategy lifecycleStrategy,
				BeanInstanceProducer fallbackProducer) {
			this.name = name;
			this.beanType = beanType;
			this.lifecycleStrategy = lifecycleStrategy;
			this.fallbackProducer = fallbackProducer;
		}

		@Override
		public Class<B> getBeanClass() {
			return beanType;
		}

		@Override
		public synchronized void initialize() {
			if ( delegateContainedBean == null ) {
				delegateContainedBean =
						lifecycleStrategy.createBean( name, beanType, fallbackProducer, DUMMY_BEAN_CONTAINER );
				delegateContainedBean.initialize();
			}
		}

		@Override
		public synchronized B getBeanInstance() {
			if ( delegateContainedBean == null ) {
				initialize();
			}
			return delegateContainedBean.getBeanInstance();
		}

		@Override
		public synchronized void release() {
			if ( delegateContainedBean != null ) {
				delegateContainedBean.release();
			}
			delegateContainedBean = null;
		}
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
		public <B> ContainedBean<B> getBootstrapSafeBean(
				Class<B> beanType, LifecycleOptions options, BeanInstanceProducer producer) {
			return CdiBeanContainerExtendedAccessImpl.this.getBootstrapSafeBean( beanType, options, producer );
		}

		@Override
		public void releaseBean(ManagedBean<?> bean) {
			CdiBeanContainerExtendedAccessImpl.this.releaseBean( bean );
		}

		@Override
		public void stop() {
		}
	};
}
