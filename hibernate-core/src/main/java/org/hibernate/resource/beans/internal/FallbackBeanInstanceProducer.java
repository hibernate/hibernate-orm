/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;


import org.hibernate.InstantiationException;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.jboss.logging.Logger;

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
	private static final Logger LOG = Logger.getLogger( FallbackBeanInstanceProducer.class );

	/**
	 * Singleton access
	 */
	public static final FallbackBeanInstanceProducer INSTANCE = new FallbackBeanInstanceProducer();

	private FallbackBeanInstanceProducer() {
	}

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		LOG.tracef( "Creating ManagedBean [%s] using direct instantiation", beanType.getName() );
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
