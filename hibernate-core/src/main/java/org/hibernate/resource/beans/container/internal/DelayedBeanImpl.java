/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * A {@link ContainedBeanImplementor} that defers creation of its delegate bean
 * until first access via {@link #getBeanInstance()}. Used by
 * {@link CdiBeanContainerDelayedAccessImpl} and {@link ManagedBeanRegistryImpl}
 * to acquire beans that are safe to reference during bootstrap, before the CDI
 * container is fully available.
 *
 * @author Sean Okafor
 */
public class DelayedBeanImpl<B> implements ContainedBeanImplementor<B> {
	private final Class<B> beanType;
	private final BeanLifecycleStrategy lifecycleStrategy;
	private final BeanInstanceProducer fallbackProducer;
	private final BeanContainer beanContainer;
	private ContainedBeanImplementor<B> delegateBean;

	public DelayedBeanImpl(Class<B> beanType, BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer, BeanContainer beanContainer) {
		this.beanType = beanType;
		this.lifecycleStrategy = lifecycleStrategy;
		this.fallbackProducer = fallbackProducer;
		this.beanContainer = beanContainer;
	}

	@Override
	public Class<B> getBeanClass() {
		return beanType;
	}

	@Override
	public void initialize(){
		if (delegateBean == null) {
			delegateBean = lifecycleStrategy.createBean( beanType, fallbackProducer, beanContainer);
		}
		delegateBean.initialize();
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
		if ( delegateBean != null ) {
			delegateBean.release();
		}
		delegateBean = null;
	}
}
