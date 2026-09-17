/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.resource.beans.container.internal.AbstractBeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * @author Yanming Zhou
 */
@SuppressWarnings({"unchecked", "unused"})
public class TestBeanContainer extends AbstractBeanContainer {

	@Override
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		return new ContainedBean<>() {
			@Override
			public void initialize() {
				// No deferred initialization.
			}

			@Override
			public void release() {
				// No resources owned by this handle.
			}

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
	protected <B> ContainedBean<B> createBean(
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
