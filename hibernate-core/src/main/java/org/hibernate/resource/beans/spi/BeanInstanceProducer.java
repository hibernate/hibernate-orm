/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

/**
 * Contract for producing a bean instance
 *
 * @author Steve Ebersole
 */
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
