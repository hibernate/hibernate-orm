/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Yanming Zhou
 */
public class SimpleBeanContainer implements BeanContainer {

	public static final long INITIAL_VALUE = 23L;

	@Override
	@SuppressWarnings("unchecked")
	public <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return new ContainedBean<>() {
			@Override
			public B getBeanInstance() {
				return (B) (beanType == SimpleGenerator.class ?
						new SimpleGenerator( new AtomicLong( INITIAL_VALUE ) ) : fallbackProducer.produceBeanInstance( beanType ) );
			}
			@Override
			public Class<B> getBeanClass() {
				return beanType;
			}
		};
	}

	@Override
	public <B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return null;
	}

	@Override
	public void stop() {

	}
}
