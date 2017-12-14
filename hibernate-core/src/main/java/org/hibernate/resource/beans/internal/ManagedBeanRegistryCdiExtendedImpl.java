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
import org.hibernate.resource.beans.spi.ExtendedBeanManager;
import org.hibernate.resource.beans.spi.ManagedBean;

import org.jboss.logging.Logger;

/**
 * A CDI-based ManagedBeanRegistry for Hibernate leveraging a proposed extension to CDI.
 * Specifically the extension is meant to tell us when the CDI BeanManager is usable.
 * So it is a delayed strategy, but delayed until a specific event : namely when our
 * {@link ExtendedBeanManager.LifecycleListener} callbacks occur.
 *
 * @see ManagedBeanRegistryCdiStandardImpl
 * @see ManagedBeanRegistryCdiDelayedImpl
 *
 * @author Steve Ebersole
 */
public class ManagedBeanRegistryCdiExtendedImpl
		extends AbstractManagedBeanRegistry
		implements ExtendedBeanManager.LifecycleListener {

	private static final Logger log = Logger.getLogger( ManagedBeanRegistryCdiStandardImpl.class );

	private BeanManager usableBeanManager;

	private ManagedBeanRegistryCdiExtendedImpl(ExtendedBeanManager beanManager) {
		beanManager.registerLifecycleListener( this );
		log.debugf( "Extended access requested to CDI BeanManager : " + beanManager );
	}

	@Override
	protected <T> ManagedBean<T> createBean(Class<T> beanClass) {
		return new ManagedBeanImpl<>( beanClass );
	}

	@Override
	protected <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract) {
		return new NamedManagedBeanImpl<>( beanName, beanContract );
	}

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		this.usableBeanManager = beanManager;

		// force each bean to initialize
		forEachBean( ManagedBean::getBeanInstance );
	}

	private BeanManager getUsableBeanManager() {
		if ( usableBeanManager == null ) {
			throw new IllegalStateException( "ExtendedBeanManager.LifecycleListener callback not yet called: CDI not (yet) usable" );
		}
		return usableBeanManager;
	}

	private class ManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;

		private boolean initialized = false;

		private InjectionTarget<T> injectionTarget;
		private CreationalContext<T> creationContext;
		private T beanInstance;

		private ManagedBeanImpl(Class<T> beanClass) {
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
			final BeanManager beanManager = getUsableBeanManager();
			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationContext = beanManager.createCreationalContext( null );

			this.beanInstance = injectionTarget.produce( creationContext );
			injectionTarget.inject( this.beanInstance, creationContext );

			injectionTarget.postConstruct( this.beanInstance );

			this.initialized = true;
		}

		public void release() {
			if ( !initialized ) {
				log.debugf( "Skipping release for (extended) CDI bean [%s] as it was not initialized", beanClass.getName() );
				return;
			}

			log.debugf( "Releasing (extended) CDI bean [%s]", beanClass.getName() );

			injectionTarget.preDestroy( beanInstance );
			injectionTarget.dispose( beanInstance );
			creationContext.release();
		}
	}

	private class NamedManagedBeanImpl<T> implements ManagedBean<T> {
		private final String beanName;
		private final Class<T> beanContract;

		private boolean initialized = false;

		private CreationalContext<T> creationContext;
		private T beanInstance;

		public NamedManagedBeanImpl(
				String beanName,
				Class<T> beanContract) {
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
			final BeanManager beanManager = getUsableBeanManager();
			final Bean<T> bean = Helper.INSTANCE.getNamedBean( beanName, beanContract, beanManager );

			this.creationContext = beanManager.createCreationalContext( bean );
			this.beanInstance = beanContract.cast( beanManager.getReference( bean, beanContract, creationContext ) );

			this.initialized = true;
		}

		@Override
		public void release() {
			if ( !initialized ) {
				log.debugf( "Skipping release for (extended) CDI bean [%s : %s] as it was not initialized", beanName, beanContract.getName() );
				return;
			}

			log.debugf( "Releasing (extended) CDI bean [%s : %s]", beanName, beanContract.getName() );

			creationContext.release();

			initialized = false;
		}
	}
}
