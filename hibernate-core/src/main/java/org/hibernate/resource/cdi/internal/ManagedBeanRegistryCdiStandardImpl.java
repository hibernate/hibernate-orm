/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.cdi.internal;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.resource.cdi.spi.AbstractManagedBeanRegistry;
import org.hibernate.resource.cdi.spi.ManagedBean;

import org.jboss.logging.Logger;

/**
 * A CDI-based ManagedBeanRegistry for Hibernate following the JPA standard
 * prescribed manner... namely assuming immediate access to the BeanManager is
 * allowed.
 *
 * @see ManagedBeanRegistryCdiExtendedImpl
 * @see ManagedBeanRegistryCdiDelayedImpl
 *
 * @author Steve Ebersole
 */
class ManagedBeanRegistryCdiStandardImpl extends AbstractManagedBeanRegistry {
	private static final Logger log = Logger.getLogger( ManagedBeanRegistryCdiStandardImpl.class );

	private final BeanManager beanManager;

	private ManagedBeanRegistryCdiStandardImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
		log.debugf( "Standard access requested to CDI BeanManager : " + beanManager );
	}

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass) {
		return new ManagedBeanImpl<T>( beanClass );
	}

	private class ManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final InjectionTarget<T> injectionTarget;
		private final CreationalContext<T> creationalContext;
		private final T beanInstance;

		public ManagedBeanImpl(Class<T> beanClass) {
			this.beanClass = beanClass;

			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationalContext = beanManager.createCreationalContext( null );

			this.beanInstance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( this.beanInstance, creationalContext );

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
			creationalContext.release();
		}
	}
}