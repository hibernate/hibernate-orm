/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.spi.ManagedBean;

class StandardCdiLifecycleManagementStrategy implements CdiLifecycleManagementStrategy {

	static final StandardCdiLifecycleManagementStrategy INSTANCE = new StandardCdiLifecycleManagementStrategy();

	private StandardCdiLifecycleManagementStrategy() {
		// private constructor, do not use
	}

	@Override
	public <T> ManagedBean<T> createBean(BeanManager beanManager, Class<T> beanClass) {
		Bean<T> bean = Helper.INSTANCE.getBean( beanClass, beanManager );

		// Pass the bean to createCreationalContext here so that an existing instance can be returned
		CreationalContext<T> creationalContext = beanManager.createCreationalContext( bean );

		T beanInstance = bean.create( creationalContext );

		return new BeanManagerManagedBeanImpl<>( beanClass, creationalContext, beanInstance );
	}

	@Override
	public <T> ManagedBean<T> createBean(BeanManager beanManager, String beanName, Class<T> beanClass) {
		Bean<T> bean = Helper.INSTANCE.getNamedBean( beanName, beanClass, beanManager );

		// Pass the bean to createCreationalContext here so that an existing instance can be returned
		CreationalContext<T> creationalContext = beanManager.createCreationalContext( bean );

		T beanInstance = bean.create( creationalContext );

		return new BeanManagerManagedBeanImpl<>( beanClass, creationalContext, beanInstance );
	}

	private static class BeanManagerManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final CreationalContext<T> creationContext;
		private final T beanInstance;

		private BeanManagerManagedBeanImpl(
				Class<T> beanClass,
				CreationalContext<T> creationContext, T beanInstance) {
			this.beanClass = beanClass;
			this.creationContext = creationContext;
			this.beanInstance = beanInstance;
		}

		@Override
		public Class<T> getBeanClass() {
			return beanClass;
		}

		@Override
		public T getBeanInstance() {
			return beanInstance;
		}

		@Override
		public void release() {
			creationContext.release();
		}
	}
}
