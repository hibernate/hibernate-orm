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
 * A CDI-based ManagedBeanRegistry for Hibernate delaying access to the
 * BeanManager until first need.
 *
 * @see ManagedBeanRegistryCdiExtendedImpl
 * @see ManagedBeanRegistryCdiStandardImpl
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryCdiDelayedImpl extends AbstractManagedBeanRegistry {
	private static final Logger log = Logger.getLogger( ManagedBeanRegistryCdiDelayedImpl.class );

	private final BeanManager beanManager;

	private ManagedBeanRegistryCdiDelayedImpl(BeanManager beanManager) {
		this.beanManager = beanManager;
		log.debugf( "Delayed access requested to CDI BeanManager : " + beanManager );
	}

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass) {
		return new ManagedBeanImpl<T>( beanClass );
	}

	private class ManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;

		private boolean initialized = false;

		private InjectionTarget<T> injectionTarget;
		private CreationalContext<T> creationalContext;
		private T beanInstance;

		public ManagedBeanImpl(Class<T> beanClass) {
			this.beanClass = beanClass;
		}

		@Override
		public Class<T> getBeanClass() {
			return beanClass;
		}

		@Override
		public T getBeanInstance() {
			if ( !initialized ) {
				initialize();
			}
			return beanInstance;
		}

		private void initialize() {
			log.debug( "Delayed initialization of CDI bean on first use : " + beanClass );

			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationalContext = beanManager.createCreationalContext( null );

			this.beanInstance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( this.beanInstance, creationalContext );

			injectionTarget.postConstruct( this.beanInstance );

			this.initialized = true;
		}

		@Override
		public void release() {
			if ( !initialized ) {
				log.debug( "Skipping release for delayed CDI bean [" + beanClass + "] as it was not initialized" );
				return;
			}

			log.debug( "Releasing CDI listener : " + beanClass );

			injectionTarget.preDestroy( beanInstance );
			injectionTarget.dispose( beanInstance );
			creationalContext.release();
		}
	}
}