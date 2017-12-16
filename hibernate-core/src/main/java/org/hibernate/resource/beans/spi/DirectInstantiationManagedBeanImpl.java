/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.InstantiationException;

import org.jboss.logging.Logger;

/**
 * ManagedBean implementation for cases where we have a Class and will simply
 * directly instantiate it.
 *
 * @author Steve Ebersole
 */
public class DirectInstantiationManagedBeanImpl<T> implements ManagedBean<T> {
	private static final Logger log = Logger.getLogger( DirectInstantiationManagedBeanImpl.class );

	private final Class<T> beanClass;
	private final T bean;

	public DirectInstantiationManagedBeanImpl(Class<T> beanClass) {
		log.debugf( "Creating ManagedBean[%s] using direct instantiation", beanClass.getName() );
		this.beanClass = beanClass;
		try {
			this.bean = beanClass.newInstance();
		}
		catch (Exception e) {
			throw new InstantiationException( "Could not instantiate managed bean directly", beanClass, e );
		}
	}

	@Override
	public Class<T> getBeanClass() {
		return beanClass;
	}

	@Override
	public T getBeanInstance() {
		return bean;
	}

	@Override
	public void release() {
		// nothing to do
	}

}
