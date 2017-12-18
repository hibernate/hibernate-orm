/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.resource.beans.spi.ManagedBean;

class JpaCdiLifecycleManagementStrategy implements CdiLifecycleManagementStrategy {

	static final JpaCdiLifecycleManagementStrategy INSTANCE = new JpaCdiLifecycleManagementStrategy();

	private JpaCdiLifecycleManagementStrategy() {
		// private constructor, do not use
	}

	@Override
	public <T> ManagedBean<T> createBean(BeanManager beanManager, Class<T> beanClass) {
		AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
		InjectionTarget<T> injectionTarget = beanManager.createInjectionTarget( annotatedType );
		CreationalContext<T> creationalContext = beanManager.createCreationalContext( null );

		T beanInstance = injectionTarget.produce( creationalContext );
		injectionTarget.inject( beanInstance, creationalContext );

		injectionTarget.postConstruct( beanInstance );

		return new JpaManagedBeanImpl<>( beanClass, injectionTarget, creationalContext, beanInstance );
	}

	@Override
	public <T> ManagedBean<T> createBean(BeanManager beanManager, String beanName, Class<T> beanClass) {
		Bean<T> bean = Helper.INSTANCE.getNamedBean( beanName, beanClass, beanManager );

		CreationalContext<T> creationalContext = beanManager.createCreationalContext( null );

		T beanInstance = bean.create( creationalContext );

		return new NamedJpaManagedBeanImpl<>( beanClass, creationalContext, beanInstance );
	}

	private static class JpaManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final InjectionTarget<T> injectionTarget;
		private final CreationalContext<T> creationContext;
		private final T beanInstance;

		private JpaManagedBeanImpl(
				Class<T> beanClass,
				InjectionTarget<T> injectionTarget, CreationalContext<T> creationContext, T beanInstance) {
			this.beanClass = beanClass;
			this.injectionTarget = injectionTarget;
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
			injectionTarget.preDestroy( beanInstance );
			injectionTarget.dispose( beanInstance );
			creationContext.release();
		}
	}

	private static class NamedJpaManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final CreationalContext<T> creationContext;
		private final T beanInstance;

		private NamedJpaManagedBeanImpl(
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
