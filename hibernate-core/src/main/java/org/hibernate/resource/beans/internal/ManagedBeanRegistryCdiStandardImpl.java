/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.resource.beans.spi.AbstractManagedBeanRegistry;
import org.hibernate.resource.beans.spi.ManagedBean;

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
		return new ManagedBeanImpl<>( beanClass );
	}

	@Override
	protected <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract) {
		return new NamedManagedBeanImpl<>( beanName, beanContract );
	}

	private class ManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final InjectionTarget<T> injectionTarget;

		private boolean initialized;

		private final CreationalContext<T> creationContext;
		private final T beanInstance;

		public ManagedBeanImpl(Class<T> beanClass) {
			this.beanClass = beanClass;

			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
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
			if ( !initialized ) {
				log.debugf( "Skipping release for (standard) CDI bean [%s] as it was not initialized", beanClass.getName() );
				return;
			}

			log.debugf( "Releasing (standard) CDI bean [%s]", beanClass.getName() );

			injectionTarget.preDestroy( beanInstance );
			injectionTarget.dispose( beanInstance );
			creationContext.release();
		}
	}

	private class NamedManagedBeanImpl<T> implements ManagedBean<T> {
		private final String beanName;
		private final Class<T> beanContract;

		private boolean initialized;

		private CreationalContext<T> creationContext;
		private T beanInstance;

		private NamedManagedBeanImpl(String beanName, Class<T> beanContract) {
			this.beanName = beanName;
			this.beanContract = beanContract;

			final Bean<T> bean = Helper.INSTANCE.getNamedBean( beanName, beanContract, beanManager );

			this.creationContext = beanManager.createCreationalContext( bean );
			this.beanInstance = beanContract.cast( beanManager.getReference( bean, beanContract, creationContext ) );

			this.initialized = true;
		}
		@Override
		public Class<T> getBeanClass() {
			return beanContract;
		}

		@Override
		public T getBeanInstance() {
			return beanInstance;
		}

		@Override
		public void release() {
			if ( !initialized ) {
				log.debugf( "Skipping release for (standard) CDI bean [%s : %s] as it was not initialized", beanName, beanContract.getName() );
				return;
			}

			log.debugf( "Releasing (standard) CDI bean [%s : %s]", beanName, beanContract.getName() );

			creationContext.release();

			initialized = false;
		}
	}
}
