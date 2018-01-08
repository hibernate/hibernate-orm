/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.spi.ManagedBean;

import org.jboss.logging.Logger;

/**
 * A {@link CdiLifecycleManagementStrategy} to use when CDI compliance is required
 * (i.e. when the bean lifecycle is to be managed by the CDI runtime, not the JPA runtime).
 *
 * The main characteristic of this strategy is that every create/destroy operation is delegated
 * to the CDI runtime.
 *
 * In particular, @Singleton-scoped or @ApplicationScoped beans are retrieved from the CDI context,
 * and are not duplicated, in contrast to {@link JpaCdiLifecycleManagementStrategy}.
 */
class StandardCdiLifecycleManagementStrategy implements CdiLifecycleManagementStrategy {
	private static final Logger log = Logger.getLogger( CompositeManagedBeanRegistry.class );

	static final StandardCdiLifecycleManagementStrategy INSTANCE = new StandardCdiLifecycleManagementStrategy();

	private StandardCdiLifecycleManagementStrategy() {
		// private constructor, do not use
	}

	@Override
	public <T> ManagedBean<T> createBean(BeanManager beanManager, Class<T> beanClass) {
		Instance<T> instance = beanManager.createInstance().select( beanClass );
		T beanInstance = instance.get();

		return new BeanManagerManagedBeanImpl<>( beanClass, instance, beanInstance );
	}

	@Override
	public <T> ManagedBean<T> createBean(BeanManager beanManager, String beanName, Class<T> beanClass) {
		Instance<T> instance = beanManager.createInstance().select( beanClass, new NamedBeanQualifier( beanName ) );
		T beanInstance = instance.get();

		return new BeanManagerManagedBeanImpl<>( beanClass, instance, beanInstance );
	}

	private static class BeanManagerManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final Instance<T> instance;
		private final T beanInstance;

		private BeanManagerManagedBeanImpl(
				Class<T> beanClass,
				Instance<T> instance, T beanInstance) {
			this.beanClass = beanClass;
			this.instance = instance;
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
			try {
				instance.destroy( beanInstance );
			}
			catch (ContextNotActiveException e) {
				log.debugf(
						"Error destroying managed bean instance [%s] - the context is not active anymore."
								+ " The instance must have been destroyed already - ignoring.",
						instance,
						e
				);
			}
		}
	}
}
