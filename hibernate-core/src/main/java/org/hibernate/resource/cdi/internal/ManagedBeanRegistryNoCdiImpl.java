/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.cdi.internal;

import org.hibernate.HibernateException;
import org.hibernate.resource.cdi.spi.AbstractManagedBeanRegistry;
import org.hibernate.resource.cdi.spi.ManagedBean;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryNoCdiImpl extends AbstractManagedBeanRegistry {
	private static final Logger log = Logger.getLogger( ManagedBeanRegistryNoCdiImpl.class );

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass) {
		return new ManagedBeanImpl<>( beanClass );
	}

	private class ManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final T bean;

		private ManagedBeanImpl(Class<T> beanClass) {
			log.debugf( "Creating non-cdi ManagedBean[%s]", beanClass.getName() );
			this.beanClass = beanClass;
			try {
				this.bean = beanClass.newInstance();
			}
			catch (Exception e) {
				throw new HibernateException( "Could not instantiate bean : " + beanClass.getName(), e );
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