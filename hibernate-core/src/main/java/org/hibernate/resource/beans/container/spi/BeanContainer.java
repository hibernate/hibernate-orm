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
