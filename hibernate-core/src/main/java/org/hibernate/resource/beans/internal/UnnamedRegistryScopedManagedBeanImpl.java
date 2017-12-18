/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.resource.beans.spi.ManagedBean;

class UnnamedRegistryScopedManagedBeanImpl<T> implements ManagedBean<T> {

	private final Class<T> beanClass;

	private final InjectionTarget<T> injectionTarget;
	private final CreationalContext<T> creationContext;
	private T beanInstance;

	UnnamedRegistryScopedManagedBeanImpl(BeanManager beanManager, Class<T> beanClass) {
		this.beanClass = beanClass;

		final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
		this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
		this.creationContext = beanManager.createCreationalContext( null );

		this.beanInstance = injectionTarget.produce( creationContext );
		injectionTarget.inject( this.beanInstance, creationContext );

		injectionTarget.postConstruct( this.beanInstance );
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
