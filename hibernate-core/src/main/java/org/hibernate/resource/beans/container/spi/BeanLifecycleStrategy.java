/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.spi;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * Models how the lifecycle for a bean should be managed.
 */
public interface BeanLifecycleStrategy {

	<B> ContainedBeanImplementor<B> findRegisteredBean(Class<B> beanClass, BeanContainerImplementor container);
	<B> ContainedBeanImplementor<B> findRegisteredBean(String beanName, Class<B> beanClass, BeanContainerImplementor container);

	<B> ContainedBeanImplementor<B> createBean(
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainerImplementor container);

	<B> ContainedBeanImplementor<B> createBean(
			String beanName,
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainerImplementor container);

}
