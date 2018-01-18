/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cdi.general.nonregistrymanaged;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.resource.beans.spi.BeanInstanceProducer;

public class TheFallbackBeanInstanceProducer implements BeanInstanceProducer {
	private final AtomicInteger instantiationCount = new AtomicInteger( 0 );
	private final AtomicInteger namedInstantiationCount = new AtomicInteger( 0 );

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		try {
			instantiationCount.getAndIncrement();
			return beanType.newInstance();
		}
		catch (IllegalAccessException|InstantiationException|RuntimeException e) {
			throw new AssertionError( "Unexpected error instantiating a bean by type using reflection", e );
		}
	}

	@Override
	public <B> B produceBeanInstance(String name, Class<B> beanType) {
		try {
			Class<?> clazz = getClass().getClassLoader().loadClass( name );
			if ( beanType.isAssignableFrom( clazz ) ) {
				namedInstantiationCount.getAndIncrement();
				return (B) clazz.newInstance();
			}
			else {
				throw new RuntimeException( clazz + " does not extend the contract " + beanType + " as expected" );
			}
		}
		catch (ClassNotFoundException|IllegalAccessException|InstantiationException|RuntimeException e) {
			throw new AssertionError( "Unexpected error instantiating a bean by name using reflection", e );
		}
	}

	public int currentInstantiationCount() {
		return instantiationCount.get();
	}

	public int currentNamedInstantiationCount() {
		return namedInstantiationCount.get();
	}
}
