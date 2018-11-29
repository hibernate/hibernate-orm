/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.jboss.logging.Logger;

/**
 * BeanInstanceProducer implementation based on direct instantiation
 *
 * In normal Hibernate use this is used when either:
 * 		* there is no configured back-end container
 * 		* the back-end container did not define a bean for this class
 *
 * @author Steve Ebersole
 */
public class FallbackBeanInstanceProducer implements BeanInstanceProducer {
	private static final Logger log = Logger.getLogger( FallbackBeanInstanceProducer.class );

	/**
	 * Singleton access
	 */
	public static final FallbackBeanInstanceProducer INSTANCE = new FallbackBeanInstanceProducer();

	private FallbackBeanInstanceProducer() {
	}

	@Override
	public <B> B produceBeanInstance(Class<B> beanType) {
		log.tracef( "Creating ManagedBean(%s) using direct instantiation", beanType.getName() );
		try {
			Constructor<B> constructor = beanType.getDeclaredConstructor();
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
