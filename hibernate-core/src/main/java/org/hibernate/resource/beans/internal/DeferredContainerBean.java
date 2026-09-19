/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;

/**
 * A {@link ManagedBean} that defers actual bean resolution through the
 * {@link BeanContainer} until first access via {@link #getBeanInstance()}.
 *
 * @author Sean Okafor
 */
public class DeferredContainerBean<B> implements ManagedBean<B> {
	private final Class<B> beanClass;
	private final BeanContainer beanContainer;
	private final BeanContainer.LifecycleOptions lifecycleOptions;
	private final BeanInstanceProducer fallbackProducer;
	private volatile ContainedBean<B> delegate;

	public DeferredContainerBean(
			Class<B> beanClass,
			BeanContainer beanContainer,
			BeanContainer.LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer) {
		this.beanClass = beanClass;
		this.beanContainer = beanContainer;
		this.lifecycleOptions = lifecycleOptions;
		this.fallbackProducer = fallbackProducer;
	}

	@Override
	public Class<B> getBeanClass() {
		return beanClass;
	}

	@Override
	public B getBeanInstance() {
		if ( delegate == null ) {
			synchronized ( this ) {
				//In case another thread resolved
				if ( delegate == null ) {
					delegate = beanContainer.getBean(
							beanClass, lifecycleOptions, fallbackProducer );
				}
			}
		}
		return delegate.getBeanInstance();
	}
}
