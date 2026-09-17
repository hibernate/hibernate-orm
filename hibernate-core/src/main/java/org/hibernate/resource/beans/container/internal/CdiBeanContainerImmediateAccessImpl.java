/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.inject.spi.BeanManager;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import static org.hibernate.resource.beans.internal.BeansMessageLogger.BEANS_MSG_LOGGER;

/**
 * @author Steve Ebersole
 */
public class CdiBeanContainerImmediateAccessImpl extends AbstractCdiBeanContainer {

	private final BeanManager beanManager;

	CdiBeanContainerImmediateAccessImpl(BeanManager beanManager) {
		BEANS_MSG_LOGGER.standardAccessToBeanManager();
		this.beanManager = beanManager;
	}

	@Override
	public BeanManager getUsableBeanManager() {
		return beanManager;
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBean<B> bean =
				lifecycleStrategy.createBean( beanType, fallbackProducer, this );
		bean.initialize();
		return bean;
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		final ContainedBean<B> bean =
				lifecycleStrategy.createBean( name, beanType, fallbackProducer, this );
		bean.initialize();
		return bean;
	}
}
