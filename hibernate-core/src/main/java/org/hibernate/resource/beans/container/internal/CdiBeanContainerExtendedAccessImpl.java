/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager;
import org.hibernate.resource.beans.container.spi.AbstractBeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class CdiBeanContainerExtendedAccessImpl
		extends AbstractBeanContainer
		implements ExtendedBeanManager.LifecycleListener {

	// NOTE : we continue to use the deprecated form for now since that is what WildFly needs for the time being still

	private static final Logger log = Logger.getLogger( CdiBeanContainerExtendedAccessImpl.class );

	private BeanManager usableBeanManager;

	private CdiBeanContainerExtendedAccessImpl(ExtendedBeanManager beanManager) {
		beanManager.registerLifecycleListener( this );
		log.debugf( "Extended access requested to CDI BeanManager : " + beanManager );
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		if ( usableBeanManager == null ) {
			final BeanImpl<B> bean = new BeanImpl<>( beanType, lifecycleStrategy, fallbackProducer );
			registerContainedBean( beanType.getName(), bean );
			return bean;
		}
		else {
			return lifecycleStrategy.createBean( beanType, fallbackProducer, this );
		}
	}

	@Override
	protected <B> ContainedBean<B> createBean(
			String name,
			Class<B> beanType,
			BeanLifecycleStrategy lifecycleStrategy,
			BeanInstanceProducer fallbackProducer) {
		if ( usableBeanManager == null ) {
			final NamedBeanImpl<B> bean = new NamedBeanImpl<>(
					name,
					beanType,
					lifecycleStrategy,
					fallbackProducer
			);
			registerContainedBean( Helper.INSTANCE.determineBeanRegistrationKey( name, beanType), bean );
			return bean;
		}
		else {
			return lifecycleStrategy.createBean( name, beanType, fallbackProducer, this );
		}
	}

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		this.usableBeanManager = beanManager;
		forEachBean( ContainedBeanImplementor::initialize );
	}

	@Override
	public void beforeBeanManagerDestroyed(BeanManager beanManager) {
		stop();
		this.usableBeanManager = null;
	}

	@Override
	public BeanManager getUsableBeanManager() {
		if ( usableBeanManager == null ) {
			throw new IllegalStateException( "ExtendedBeanManager.LifecycleListener callback not yet called: CDI not (yet) usable" );
		}
		return usableBeanManager;
	}

	private class BeanImpl<B> implements ContainedBeanImplementor<B> {
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBeanImplementor<B> delegateContainedBean;

		private BeanImpl(
				Class<B> beanType,
				BeanLifecycleStrategy lifecycleStrategy,
				BeanInstanceProducer fallbackProducer) {
			this.beanType = beanType;
			this.lifecycleStrategy = lifecycleStrategy;
			this.fallbackProducer = fallbackProducer;
		}


		@Override
		public void initialize() {
			if ( delegateContainedBean == null ) {
				delegateContainedBean = lifecycleStrategy.createBean( beanType, fallbackProducer, DUMMY_BEAN_CONTAINER );
			}
			delegateContainedBean.initialize();
		}

		@Override
		public B getBeanInstance() {
			if ( delegateContainedBean == null ) {
				initialize();
			}
			return delegateContainedBean.getBeanInstance();
		}

		@Override
		public void release() {
			delegateContainedBean.release();
			delegateContainedBean = null;
		}
	}

	private class NamedBeanImpl<B> implements ContainedBeanImplementor<B> {
		private final String name;
		private final Class<B> beanType;
		private final BeanLifecycleStrategy lifecycleStrategy;
		private final BeanInstanceProducer fallbackProducer;

		private ContainedBeanImplementor<B> delegateContainedBean;

		private NamedBeanImpl(
				String name,
				Class<B> beanType,
				BeanLifecycleStrategy lifecycleStrategy, 
				BeanInstanceProducer fallbackProducer) {
			this.name = name;
			this.beanType = beanType;
			this.lifecycleStrategy = lifecycleStrategy;
			this.fallbackProducer = fallbackProducer;
		}

		@Override
		public void initialize() {
			if ( delegateContainedBean == null ) {
				delegateContainedBean = lifecycleStrategy.createBean(
						name,
						beanType,
						fallbackProducer,
						DUMMY_BEAN_CONTAINER
				);
				delegateContainedBean.initialize();
			}
		}

		@Override
		public B getBeanInstance() {
			if ( delegateContainedBean == null ) {
				initialize();
			}
			return delegateContainedBean.getBeanInstance();
		}

		@Override
		public void release() {
			delegateContainedBean.release();
			delegateContainedBean = null;
		}
	}

	private final CdiBasedBeanContainer DUMMY_BEAN_CONTAINER = new CdiBasedBeanContainer() {
		@Override
		public BeanManager getUsableBeanManager() {
			return usableBeanManager;
		}

		@Override
		public ContainedBeanImplementor findRegistered(String key) {
			return CdiBeanContainerExtendedAccessImpl.this.findRegistered( key );
		}

		@Override
		public void registerContainedBean(String key, ContainedBeanImplementor bean) {
		}

		@Override
		public <B> ContainedBean<B> getBean(Class<B> beanType, BeanLifecycleStrategy lifecycleStrategy, BeanInstanceProducer fallbackProducer) {
			return CdiBeanContainerExtendedAccessImpl.this.getBean( beanType, lifecycleStrategy, fallbackProducer );
		}

		@Override
		public <B> ContainedBean<B> getBean(
				String name,
				Class<B> beanType,
				BeanLifecycleStrategy lifecycleStrategy,
				BeanInstanceProducer fallbackProducer) {
			return CdiBeanContainerExtendedAccessImpl.this.getBean( name, beanType, lifecycleStrategy, fallbackProducer );
		}

		@Override
		public void stop() {
		}
	};
}
