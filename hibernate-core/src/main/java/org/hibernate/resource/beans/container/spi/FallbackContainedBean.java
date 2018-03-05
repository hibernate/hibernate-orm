/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.spi;

import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;

/**
 * @author Steve Ebersole
 */
public class FallbackContainedBean<B> implements ContainedBean<B>, ManagedBean<B> {
	private final Class<B> beanType;

	private final B beanInstance;


	public FallbackContainedBean(Class<B> beanType, BeanInstanceProducer producer) {
		this.beanType = beanType;
		this.beanInstance = producer.produceBeanInstance( beanType );
	}

	public FallbackContainedBean(String beanName, Class<B> beanType, BeanInstanceProducer producer) {
		this.beanType = beanType;
		this.beanInstance = producer.produceBeanInstance( beanName, beanType );
	}

	@Override
	public Class<B> getBeanClass() {
		return beanType;
	}

	@Override
	public B getBeanInstance() {
		return beanInstance;
	}
}
