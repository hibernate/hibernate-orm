/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/// A container which obtains beans exclusively through the supplied fallback producer.
/// Distinct beans are not retained since the producer has no destruction contract.
///
/// @author Steve Ebersole
public class FallbackBeanContainerImpl extends AbstractBeanContainer {
	@Override
	public boolean isFallback() {
		return true;
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return new FallbackContainedBean<>( beanType, producer );
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			LifecycleOptions options,
			BeanInstanceProducer producer) {
		return new FallbackContainedBean<>( name, beanType, producer );
	}
}
