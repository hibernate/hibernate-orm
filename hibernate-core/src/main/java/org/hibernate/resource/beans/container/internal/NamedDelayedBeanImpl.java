/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * Named variant of {@link DelayedBeanImpl}. Defers creation of its delegate
 * bean until first access, using a bean name to distinguish instances of the
 * same type within the CDI container.
 *
 * @author Sean Okafor
 */
public class NamedDelayedBeanImpl <B> implements ContainedBeanImplementor<B> {
	private final String beanName;
	private final Class<B> beanType;
	private final BeanLifecycleStrategy lifecycleStrategy;
	private final BeanInstanceProducer fallbackProducer;
	private final BeanContainer beanContainer;
	private ContainedBeanImplementor<B> delegateBean;

	public NamedDelayedBeanImpl(String beanName, Class<B> beanType, BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer, BeanContainer beanContainer) {
		this.beanName = beanName;
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
			delegateBean = lifecycleStrategy.createBean(beanName, beanType, fallbackProducer, beanContainer);
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
