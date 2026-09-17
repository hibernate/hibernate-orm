/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Steve Ebersole
 */
public class FallbackContainedBean<B> implements ContainedBean<B> {
	private final Class<B> beanType;

	private final B beanInstance;


	public FallbackContainedBean(Class<B> beanType, BeanInstanceProducer producer) {
		this.beanType = beanType;
		this.beanInstance = producer.produceBeanInstance( beanType );
	}

	public FallbackContainedBean(String beanName, Class<B> beanType, BeanInstanceProducer producer) {
		this.beanType = beanType;
		this.beanInstance = producer.produceBeanInstance( beanName, beanType );
	}

	@Override
	public Class<B> getBeanClass() {
		return beanType;
	}

	@Override
	public B getBeanInstance() {
		return beanInstance;
	}
	@Override
	public void initialize() {
		// The producer already created the instance in the constructor.
	}

	@Override
	public void release() {
		// BeanInstanceProducer does not define a destruction contract.
	}
}
