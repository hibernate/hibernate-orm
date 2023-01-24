/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

	/**
	 * Form of {@link #getBean(Class, LifecycleOptions, BeanInstanceProducer)}
	 * allowing to indicate whether it is required to use CDI if it is available
	 *
	 * @implNote Defaulted for backwards compatibility
	 */
	default <B> ContainedBean<B> getBean(
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		return getBean( beanType, lifecycleOptions, fallbackProducer );
	}

	<B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer);

	/**
	 * Form of {@link #getBean(String, Class, LifecycleOptions, BeanInstanceProducer)}
	 * allowing to indicate whether it is required to use CDI if it is available
	 *
	 * @implNote Defaulted for backwards compatibility
	 */
	default <B> ContainedBean<B> getBean(
			String name,
			Class<B> beanType,
			LifecycleOptions lifecycleOptions,
			BeanInstanceProducer fallbackProducer,
			boolean cdiRequiredIfAvailable) {
		return getBean( name, beanType, lifecycleOptions, fallbackProducer );
	}
}
