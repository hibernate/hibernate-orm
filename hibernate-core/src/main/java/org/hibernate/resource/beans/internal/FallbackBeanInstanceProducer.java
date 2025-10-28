/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;


import org.hibernate.InstantiationException;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import static org.hibernate.resource.beans.internal.BeansMessageLogger.BEANS_MSG_LOGGER;

/**
 * {@link BeanInstanceProducer} implementation based on direct instantiation.
 * Usually, this is used when either:
 * <ul>
 * <li>there is no configured back-end container, or
 * <li>the back-end container did not define a bean for this class.
 * </ul>
 *
 * @author Steve Ebersole
 */
public class FallbackBeanInstanceProducer implements BeanInstanceProducer {
	/**
	 * Singleton access
	 */
	public static final FallbackBeanInstanceProducer INSTANCE = new FallbackBeanInstanceProducer();

	private FallbackBeanInstanceProducer() {
	}

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		BEANS_MSG_LOGGER.creatingManagedBeanUsingDirectInstantiation( beanType.getName() );
		try {
			final var constructor = beanType.getDeclaredConstructor();
			constructor.setAccessible( true );
			return constructor.newInstance();
		}
		catch (Exception e) {
			throw new InstantiationException( "Could not instantiate managed bean directly", beanType, e );
		}
	}

	@Override
	public <B> B produceBeanInstance(String name, Class<B> beanType) {
		return produceBeanInstance( beanType );
	}

}
