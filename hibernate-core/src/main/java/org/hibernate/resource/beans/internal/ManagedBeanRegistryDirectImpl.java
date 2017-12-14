/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import org.hibernate.InstantiationException;
import org.hibernate.resource.beans.spi.AbstractManagedBeanRegistry;
import org.hibernate.resource.beans.spi.ManagedBean;

import org.jboss.logging.Logger;

/**
 * ManagedBeanRegistry implementation using direct instantiation of the beans.  Used
 * when there is no backing "injection framework" in use as well as the fallback
 * for {@link CompositeManagedBeanRegistry}
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryDirectImpl extends AbstractManagedBeanRegistry {

	private static final Logger log = Logger.getLogger( ManagedBeanRegistryDirectImpl.class );

	@SuppressWarnings("WeakerAccess")
	public ManagedBeanRegistryDirectImpl() {
	}

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass) {
		return new ManagedBeanImpl<>( beanClass );
	}

	@Override
	protected <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract) {
		return new ManagedBeanImpl<>( beanContract );
	}

	private class ManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final T bean;

		private ManagedBeanImpl(Class<T> beanClass) {
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
}
