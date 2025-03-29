/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.jboss.logging.Logger;

/**
 * A {@link BeanLifecycleStrategy} to use when CDI compliance is required
 * (i.e. when the bean lifecycle is to be managed by the CDI runtime, not the JPA runtime).
 * <p>
 * The main characteristic of this strategy is that every create/destroy operation is delegated
 * to the CDI runtime.
 * <p>
 * In particular, @Singleton-scoped or @ApplicationScoped beans are retrieved from the CDI context,
 * and are not duplicated, in contrast to {@link JpaCompliantLifecycleStrategy}.
 */
public class ContainerManagedLifecycleStrategy implements BeanLifecycleStrategy {
	private static final Logger log = Logger.getLogger( ContainerManagedLifecycleStrategy.class );

	public static final ContainerManagedLifecycleStrategy INSTANCE = new ContainerManagedLifecycleStrategy();

	private ContainerManagedLifecycleStrategy() {
		// private constructor, do not use
	}


	@Override
	public <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainer beanContainer) {
		return new BeanImpl<>( beanClass, fallbackProducer, ( (CdiBasedBeanContainer) beanContainer ).getUsableBeanManager() );
	}

	@Override
	public <B> ContainedBeanImplementor<B> createBean(
			String beanName,
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainer beanContainer) {
		return new NamedBeanImpl<>( beanName, beanClass, fallbackProducer, ( (CdiBasedBeanContainer) beanContainer ).getUsableBeanManager() );
	}



	private static abstract class AbstractBeanImpl<B> implements ContainedBeanImplementor<B> {
		final Class<B> beanType;

		BeanInstanceProducer fallbackProducer;
		BeanManager beanManager;

		Instance<B> instance;
		B beanInstance;

		private AbstractBeanImpl(
				Class<B> beanType,
				BeanInstanceProducer fallbackProducer,
				BeanManager beanManager) {
			this.beanType = beanType;
			this.fallbackProducer = fallbackProducer;
			this.beanManager = beanManager;
		}

		@Override
		public Class<B> getBeanClass() {
			return beanType;
		}

		@Override
		public B getBeanInstance() {
			if ( beanInstance == null ) {
				initialize();
			}
			return beanInstance;
		}

		@Override
		public void initialize() {
			if ( beanInstance != null ) {
				return;
			}

			try {
				instance = resolveContainerInstance();
				beanInstance = instance.get();
			}
			catch (NotYetReadyException e) {
				throw e;
			}
			catch (Exception e) {
				log.debug( "Error resolving CDI bean - using fallback" );
				beanInstance = produceFallbackInstance();
				instance = null;
			}

			this.beanManager = null;
		}

		protected abstract Instance<B> resolveContainerInstance();

		@Override
		public void release() {
			if ( beanInstance == null ) {
				return;
			}

			try {
				if ( instance == null ) {
					// todo : BeanInstanceProducer#release?
					return;
				}

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
			finally {
				beanInstance = null;
				instance = null;
				beanManager = null;
				fallbackProducer = null;
			}
		}

		protected abstract B produceFallbackInstance();
	}

	private static class BeanImpl<B> extends AbstractBeanImpl<B> {
		private BeanImpl(
				Class<B> beanType,
				BeanInstanceProducer fallbackProducer,
				BeanManager beanManager) {
			super( beanType, fallbackProducer, beanManager );
		}

		@Override
		protected Instance<B> resolveContainerInstance() {
			final Instance<Object> root;
			try {
				root = beanManager.createInstance();
			}
			catch (Exception e) {
				// this indicates that the BeanManager is not yet ready to use,
				// which should be considered an error
				throw new NotYetReadyException( e );
			}

			try {
				return root.select( beanType );
			}
			catch (Exception e) {
				throw new NoSuchBeanException( "Bean class not known to CDI : " + beanType.getName(), e );
			}
		}

		@Override
		protected B produceFallbackInstance() {
			return fallbackProducer.produceBeanInstance( beanType );
		}
	}

	private static class NamedBeanImpl<B> extends AbstractBeanImpl<B> {
		private final String beanName;

		private NamedBeanImpl(
				String beanName,
				Class<B> beanType,
				BeanInstanceProducer fallbackProducer,
				BeanManager beanManager) {
			super( beanType, fallbackProducer, beanManager );
			this.beanName = beanName;
		}

		@Override
		protected Instance<B> resolveContainerInstance() {
			final Instance<Object> root;
			try {
				root = beanManager.createInstance();
			}
			catch (Exception e) {
				// this indicates that the BeanManager is not yet ready to use,
				// which should be considered an error
				throw new NotYetReadyException( e );
			}

			try {
				return root.select( beanType, new NamedBeanQualifier( beanName ) );
			}
			catch (Exception e) {
				throw new NoSuchBeanException( "Bean class not known to CDI: " + beanType.getName(), e );
			}
		}

		@Override
		protected B produceFallbackInstance() {
			return fallbackProducer.produceBeanInstance( beanName, beanType );
		}
	}
}
