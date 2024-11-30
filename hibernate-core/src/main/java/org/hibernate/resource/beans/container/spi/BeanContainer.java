/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.service.spi.Stoppable;

/**
 * Represents a backend "bean container" - CDI, Spring, etc
 *
 * @see org.hibernate.cfg.AvailableSettings#BEAN_CONTAINER
 *
 * @author Steve Ebersole
 */
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
