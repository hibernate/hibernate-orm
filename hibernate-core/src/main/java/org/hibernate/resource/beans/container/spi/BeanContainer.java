/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.spi.Stoppable;

/**
 * Abstracts any kind of container for managed beans, for example,
 * the CDI {@link jakarta.enterprise.inject.spi.BeanManager}. A
 * custom bean container may be integrated with Hibernate by
 * implementing this interface and specifying the implementation
 * using {@value org.hibernate.cfg.AvailableSettings#BEAN_CONTAINER}.
 *
 * @see org.hibernate.cfg.AvailableSettings#BEAN_CONTAINER
 * @see org.hibernate.resource.beans.container.internal.CdiBasedBeanContainer
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface BeanContainer extends Stoppable {
	interface LifecycleOptions {
		boolean canUseCachedReferences();
		boolean useJpaCompliantCreation();
	}

	<B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer);

	<B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer);
}
