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
import org.hibernate.resource.cdi.spi.ExtendedBeanManager;
import org.hibernate.resource.cdi.spi.ManagedBean;

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
		return new ManagedBeanImpl<T>( beanClass );
	}

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		this.usableBeanManager = beanManager;
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
		private CreationalContext<T> creationalContext;
		private T beanInstance;

		private ManagedBeanImpl(Class<T> beanClass) {
			this.beanClass = beanClass;
		}

		public void initialize() {
			final BeanManager beanManager = getUsableBeanManager();
			AnnotatedType<T> annotatedType = beanManager.createAnnotatedType( beanClass );
			this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
			this.creationalContext = beanManager.createCreationalContext( null );

			this.beanInstance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( this.beanInstance, creationalContext );

			injectionTarget.postConstruct( this.beanInstance );

			this.initialized = true;
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

		public void release() {
			if ( !initialized ) {
				// log
				return;
			}

			injectionTarget.preDestroy( beanInstance );
			injectionTarget.dispose( beanInstance );
			creationalContext.release();
		}
	}

}