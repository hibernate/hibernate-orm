/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.AbstractCdiBeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class CdiBeanContainerDelayedAccessImpl extends AbstractCdiBeanContainer {
	private final BeanManager beanManager;

	private CdiBeanContainerDelayedAccessImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	public BeanManager getUsableBeanManager() {
		return beanManager;
	}

	@Override
	protected <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanType, 
			BeanLifecycleStrategy lifecycleStrategy, 
			BeanInstanceProducer fallbackProducer) {
		return new BeanImpl<>( beanType, lifecycleStrategy, fallbackProducer );
	}

	@Override
	protected <B> ContainedBeanImplementor<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		return new NamedBeanImpl<>( name, beanType, lifecycleStrategy, fallbackProducer );
	}

	private class BeanImpl<B> implements ContainedBeanImplementor<B> {
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBeanImplementor<B> delegateBean;

		private BeanImpl(
				Class<B> beanType,
				BeanLifecycleStrategy lifecycleStrategy, 
				BeanInstanceProducer fallbackProducer) {
			this.beanType = beanType;
			this.lifecycleStrategy = lifecycleStrategy;
			this.fallbackProducer = fallbackProducer;
		}

		@Override
		public void initialize() {
			if ( delegateBean == null ) {
				delegateBean = lifecycleStrategy.createBean( beanType, fallbackProducer, CdiBeanContainerDelayedAccessImpl.this );
			}
		}

		@Override
		public B getBeanInstance() {
			if ( delegateBean == null ) {
				initialize();
			}
			return delegateBean.getBeanInstance();
		}

		@Override
		public void release() {
			delegateBean.release();
		}
	}

	private class NamedBeanImpl<B> implements ContainedBeanImplementor<B> {
		private final String name;
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBeanImplementor<B> delegateBean;

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
		public void initialize() {
			if ( delegateBean == null ) {
				delegateBean = lifecycleStrategy.createBean( name, beanType, fallbackProducer, CdiBeanContainerDelayedAccessImpl.this );
			}
		}

		@Override
		public B getBeanInstance() {
			if ( delegateBean == null ) {
				initialize();
			}
			return delegateBean.getBeanInstance();
		}

		@Override
		public void release() {
			delegateBean.release();
		}
	}
}
