/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.resource.beans.spi.AbstractManagedBeanRegistry;
import org.hibernate.resource.beans.spi.ManagedBean;

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
		log.debugf( "Delayed access requested to CDI BeanManager" );
	}

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass) {
		return new ManagedBeanImpl<>( beanClass );
	}

	@Override
	protected <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract) {
		return new NamedManagedBeanImpl<>( beanName, beanContract );
	}

	private class ManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;

		private boolean initialized = false;

		private InjectionTarget<T> injectionTarget;
		private CreationalContext<T> creationContext;
		private T beanInstance;

		ManagedBeanImpl(Class<T> beanClass) {
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

			final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationContext = beanManager.createCreationalContext( null );

			this.beanInstance = injectionTarget.produce( creationContext );
			injectionTarget.inject( this.beanInstance, creationContext );

			injectionTarget.postConstruct( this.beanInstance );

			this.initialized = true;
		}

		@Override
		public void release() {
			if ( !initialized ) {
				log.debug( "Skipping release for (delayed) CDI bean [" + beanClass + "] as it was not initialized" );
				return;
			}

			log.debug( "Releasing (delayed) CDI bean : " + beanClass );

			injectionTarget.preDestroy( beanInstance );
			injectionTarget.dispose( beanInstance );
			creationContext.release();

			initialized = false;
		}
	}

	private class NamedManagedBeanImpl<T> implements ManagedBean<T> {
		private final String beanName;
		private final Class<T> beanContract;

		private boolean initialized = false;

		private CreationalContext<T> creationContext;
		private T beanInstance;

		NamedManagedBeanImpl(String beanName, Class<T> beanContract) {
			this.beanName = beanName;
			this.beanContract = beanContract;
		}

		@Override
		public Class<T> getBeanClass() {
			return beanContract;
		}

		@Override
		public T getBeanInstance() {
			if ( !initialized ) {
				initialize();
			}
			return beanInstance;
		}

		private void initialize() {
			final Bean<T> bean = Helper.INSTANCE.getNamedBean( beanName, beanContract, beanManager );

			this.creationContext = beanManager.createCreationalContext( bean );
			this.beanInstance = beanContract.cast( beanManager.getReference( bean, beanContract, creationContext ) );

			this.initialized = true;
		}

		@Override
		public void release() {
			if ( !initialized ) {
				log.debugf( "Skipping release for (delayed) CDI bean [%s : %s] as it was not initialized", beanName, beanContract.getName() );
				return;
			}

			log.debugf( "Releasing (delayed) CDI bean [%s : %s]", beanName, beanContract.getName() );

			creationContext.release();

			initialized = false;
		}
	}

}
