/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Yanming Zhou
 */
@SuppressWarnings({"unchecked", "unused"})
public class TestBeanContainer implements BeanContainer {

	@Override
	public <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return new ContainedBean<>() {
			@Override
			public B getBeanInstance() {
				return (B) (beanType == CurrentTenantIdentifierResolver.class ?
						TestCurrentTenantIdentifierResolver.INSTANCE_FOR_BEAN_CONTAINER : fallbackProducer.produceBeanInstance( beanType ) );
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
