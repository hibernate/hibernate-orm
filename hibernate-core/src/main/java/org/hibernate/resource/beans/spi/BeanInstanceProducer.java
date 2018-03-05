/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
