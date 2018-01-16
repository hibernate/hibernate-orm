/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.internal;

import javax.enterprise.inject.spi.BeanManager;

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
	protected <T> ManagedBean<T> createBean(Class<T> beanClass, boolean shouldRegistryManageLifecycle) {
		return new LazilyInitializedManagedBeanImpl<>( beanClass,
				Helper.INSTANCE.getLifecycleManagementStrategy( shouldRegistryManageLifecycle ) );
	}

	@Override
	protected <T> ManagedBean<T> createBean(String beanName, Class<T> beanContract, boolean shouldRegistryManageLifecycle) {
		return new LazilyInitializedNamedManagedBeanImpl<>( beanName, beanContract,
				Helper.INSTANCE.getLifecycleManagementStrategy( shouldRegistryManageLifecycle ) );
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

	/**
	 * A {@link ManagedBean} that is lazily initialized upon the first call to {@link #getBeanInstance()},
	 * relying on a {@link CdiLifecycleManagementStrategy} to initialize a delegate.
	 *
	 * Note that initialization is forced by the {@link ManagedBeanRegistryCdiExtendedImpl}
	 * as soon as the bean manager is initialized, see {@link #beanManagerInitialized(BeanManager)}.
	 *
	 * @param <T> The type of bean instances
	 */
	private class LazilyInitializedManagedBeanImpl<T> implements ManagedBean<T> {
		private final Class<T> beanClass;
		private final CdiLifecycleManagementStrategy strategy;

		private ManagedBean<T> delegate = null;

		LazilyInitializedManagedBeanImpl(Class<T> beanClass, CdiLifecycleManagementStrategy strategy) {
			this.beanClass = beanClass;
			this.strategy = strategy;
		}

		@Override
		public Class<T> getBeanClass() {
			return beanClass;
		}

		@Override
		public T getBeanInstance() {
			if ( delegate == null ) {
				initialize();
			}
			return delegate.getBeanInstance();
		}

		private void initialize() {
			delegate = strategy.createBean( getUsableBeanManager(), beanClass );
		}

		@Override
		public void release() {
			if ( delegate == null ) {
				log.debugf( "Skipping release for (extended) CDI bean [%s] as it was not initialized", beanClass.getName() );
				return;
			}

			log.debugf( "Releasing (extended) CDI bean : %s", beanClass.getName() );

			delegate.release();
			delegate = null;
		}
	}

	/**
	 * A named {@link ManagedBean} that is lazily initialized upon the first call to {@link #getBeanInstance()}.
	 *
	 * @param <T> The type of bean instances
	 *
	 * @see LazilyInitializedManagedBeanImpl
	 */
	private class LazilyInitializedNamedManagedBeanImpl<T> implements ManagedBean<T> {
		private final String beanName;
		private final Class<T> beanContract;
		private final CdiLifecycleManagementStrategy strategy;

		private ManagedBean<T> delegate = null;

		LazilyInitializedNamedManagedBeanImpl(String beanName, Class<T> beanContract, CdiLifecycleManagementStrategy strategy) {
			this.beanName = beanName;
			this.beanContract = beanContract;
			this.strategy = strategy;
		}

		@Override
		public Class<T> getBeanClass() {
			return beanContract;
		}

		@Override
		public T getBeanInstance() {
			if ( delegate == null ) {
				initialize();
			}
			return delegate.getBeanInstance();
		}

		private void initialize() {
			delegate = strategy.createBean( getUsableBeanManager(), beanName, beanContract );
		}

		@Override
		public void release() {
			if ( delegate == null ) {
				log.debugf( "Skipping release for (extended) CDI bean [%s : %s] as it was not initialized", beanName, beanContract.getName() );
				return;
			}

			log.debugf( "Releasing (extended) CDI bean [%s : %s]", beanName, beanContract.getName() );

			delegate.release();
			delegate = null;
		}
	}

}
