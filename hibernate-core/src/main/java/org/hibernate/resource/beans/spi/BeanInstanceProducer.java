/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/**
 * Contract for producing a bean instance
 * <p>
 * Integrations may implement and supply a producer to the acquisition methods
 * of {@link ManagedBeanRegistry}. Custom bean containers invoke the supplied
 * producer when fallback creation is needed.
 *
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface BeanInstanceProducer {
	/**
	 * Produce a bean instance
	 *
	 * @param beanType The Java type of bean to produce
	 */
	<B> B produceBeanInstance(Class<B> beanType);

	/**
	 * Produce a named bean instance
	 *
	 * @param name The bean name
	 * @param beanType The Java type that the produced bean should be typed as
	 */
	<B> B produceBeanInstance(String name, Class<B> beanType);
}
