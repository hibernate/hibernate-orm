/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Steve Ebersole
 */
public class CdiBeanContainerDelayedAccessImpl extends AbstractCdiBeanContainer {
	private final BeanManager beanManager;

	CdiBeanContainerDelayedAccessImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	public BeanManager getUsableBeanManager() {
		return beanManager;
	}

	@Override
	protected <B> ContainedBean<B> createBootstrapSafeBean(
			Class<B> beanType, LifecycleOptions options, BeanInstanceProducer producer) {
		return createBean( beanType, options, producer );
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		return new BeanImpl<>( beanType, lifecycleStrategy, fallbackProducer );
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		return new NamedBeanImpl<>( name, beanType, lifecycleStrategy, fallbackProducer );
	}

	private class BeanImpl<B> implements ContainedBean<B> {
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBean<B> delegateBean;

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
			if ( delegateBean == null ) {
				delegateBean = lifecycleStrategy.createBean( beanType, fallbackProducer, CdiBeanContainerDelayedAccessImpl.this );
			}
		}

		@Override
		public synchronized B getBeanInstance() {
			if ( delegateBean == null ) {
				initialize();
			}
			return delegateBean.getBeanInstance();
		}

		@Override
		public synchronized void release() {
			if ( delegateBean != null ) {
				delegateBean.release();
			}
		}
	}

	private class NamedBeanImpl<B> implements ContainedBean<B> {
		private final String name;
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBean<B> delegateBean;

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
			if ( delegateBean == null ) {
				delegateBean = lifecycleStrategy.createBean( name, beanType, fallbackProducer, CdiBeanContainerDelayedAccessImpl.this );
			}
		}

		@Override
		public synchronized B getBeanInstance() {
			if ( delegateBean == null ) {
				initialize();
			}
			return delegateBean.getBeanInstance();
		}

		@Override
		public synchronized void release() {
			if ( delegateBean != null ) {
				delegateBean.release();
			}
		}
	}
}
