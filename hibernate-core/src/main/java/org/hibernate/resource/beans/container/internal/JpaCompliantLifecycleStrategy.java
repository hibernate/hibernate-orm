/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.jboss.logging.Logger;

import java.util.Set;

/**
 * A {@link BeanLifecycleStrategy} to use when JPA compliance is required
 * (i.e. when the bean lifecycle is to be managed by the JPA runtime, not the CDI runtime).
 * <p>
 * The main characteristic of this strategy is that each requested bean is instantiated directly
 * and guaranteed to not be shared in the CDI context.
 * <p>
 * In particular, {@code @Singleton}-scoped or {@code @ApplicationScoped} beans are instantiated
 * directly by this strategy, even if there is already an instance in the CDI context.
 * This means singletons are not really singletons, but this seems to be the behavior required by
 * the JPA 2.2 spec.
 */
public class JpaCompliantLifecycleStrategy implements BeanLifecycleStrategy {
	private static final Logger log = Logger.getLogger( JpaCompliantLifecycleStrategy.class );

	public static final JpaCompliantLifecycleStrategy INSTANCE = new JpaCompliantLifecycleStrategy();

	private JpaCompliantLifecycleStrategy() {
		// private constructor, do not use
	}

	@Override
	public <B> ContainedBeanImplementor<B> createBean(
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainer beanContainer) {
		return new BeanImpl<>(
				beanClass,
				fallbackProducer,
				((CdiBasedBeanContainer) beanContainer).getUsableBeanManager()
		);
	}

	@Override
	public <B> ContainedBeanImplementor<B> createBean(
			String beanName,
			Class<B> beanClass,
			BeanInstanceProducer fallbackProducer,
			BeanContainer beanContainer) {
		return new NamedBeanImpl<>(
				beanName,
				beanClass,
				fallbackProducer,
				((CdiBasedBeanContainer) beanContainer).getUsableBeanManager()
		);
	}


	private static class BeanImpl<B> extends AbstractBean<B> {

		private InjectionTarget<B> injectionTarget;

		public BeanImpl(Class<B> beanType, BeanInstanceProducer fallbackProducer, BeanManager beanManager) {
			super( beanType, fallbackProducer, beanManager );
		}

		@Override
		protected B beanInstance() {
			final AnnotatedType<B> annotatedType;
			try {
				annotatedType = beanManager.createAnnotatedType( beanType );
			}
			catch (Exception e) {
				throw new IllegalStateException( new NotYetReadyException( e ) );
			}

			injectionTarget = beanManager.getInjectionTargetFactory( annotatedType ).createInjectionTarget( null );

			B beanInstance = injectionTarget.produce( creationalContext );
			injectionTarget.inject( beanInstance, creationalContext );

			injectionTarget.postConstruct( beanInstance );

			beanManager = null;

			return beanInstance;
		}

		@Override
		protected B beanInstanceFallback() {
			injectionTarget = null;
			return fallbackProducer.produceBeanInstance( beanType );
		}

		@Override
		protected String getBeanName() {
			return beanType.getName();
		}

		@Override
		protected void doRelease() {
			try {
				if ( injectionTarget == null ) {
					// todo : BeanInstanceProducer#release?
					return;
				}
				injectionTarget.preDestroy( beanInstance );
				injectionTarget.dispose( beanInstance );
			}
			finally {
				creationalContext = null;
				injectionTarget = null;
			}
		}
	}


	private static class NamedBeanImpl<B> extends AbstractBean<B> {
		private final String beanName;
		private Bean<B> bean;

		private NamedBeanImpl(
				String beanName,
				Class<B> beanType,
				BeanInstanceProducer fallbackProducer,
				BeanManager beanManager) {
			super( beanType, fallbackProducer, beanManager );
			this.beanName = beanName;
		}

		@Override
		protected B beanInstance() {
			bean = resolveBean();
			return bean.create( creationalContext );
		}

		@SuppressWarnings("unchecked")
		private Bean<B> resolveBean() {
			final Set<Bean<?>> beans = beanManager.getBeans( beanType, new NamedBeanQualifier( beanName ) );
			return (Bean<B>) beanManager.resolve( beans );
		}

		@Override
		protected B beanInstanceFallback() {
			bean = null;
			return fallbackProducer.produceBeanInstance( beanName, beanType );
		}

		@Override
		protected String getBeanName() {
			return beanName;
		}

		@Override
		protected void doRelease() {
			try {
				if ( bean == null ) {
					// todo : BeanInstanceProducer#release?
					return;
				}
				bean.destroy( beanInstance, creationalContext );
			}
			finally {
				bean = null;
			}
		}
	}

	private static abstract class AbstractBean<B> implements ContainedBeanImplementor<B> {
		protected final Class<B> beanType;
		protected final BeanInstanceProducer fallbackProducer;
		protected BeanManager beanManager;
		protected CreationalContext<B> creationalContext;

		protected B beanInstance;

		private AbstractBean(
				Class<B> beanType,
				BeanInstanceProducer fallbackProducer,
				BeanManager beanManager) {
			this.beanType = beanType;
			this.fallbackProducer = fallbackProducer;
			this.beanManager = beanManager;
		}

		@Override
		public final Class<B> getBeanClass() {
			return beanType;
		}

		@Override
		public final B getBeanInstance() {
			if ( beanInstance == null ) {
				initialize();
			}
			return beanInstance;
		}

		@Override
		public final void initialize() {
			if ( beanInstance != null ) {
				return;
			}

			if ( beanManager == null ) {
				try {
					beanInstance = fallbackProducer.produceBeanInstance( beanType );
					return;
				}
				catch (Exception e) {
					// the CDI BeanManager is not know to be ready for use and the
					// fallback-producer was unable to create the bean...
					throw new IllegalStateException(
							"CDI BeanManager is not known to be ready for use and the fallback-producer was unable to create the bean",
							new NotYetReadyException( e )
					);
				}
			}

			try {
				creationalContext = beanManager.createCreationalContext( null );
			}
			catch (Exception e) {
				throw new NotYetReadyException( e );
			}

			try {
				beanInstance = beanInstance();
			}
			catch (IllegalStateException | NotYetReadyException e) {
				throw e;
			}
			catch (Exception e) {
				log.debugf( "Error resolving CDI bean [%s] - using fallback", getBeanName() );
				beanInstance = beanInstanceFallback();

				try {
					if ( creationalContext != null ) {
						creationalContext.release();
					}
				}
				catch (Exception ignore) {
				}

				creationalContext = null;
			}
		}

		protected abstract B beanInstance();

		protected abstract B beanInstanceFallback();

		protected abstract String getBeanName();


		@Override
		public final void release() {
			if ( beanInstance == null ) {
				return;
			}

			try {
				doRelease();
			}
			catch (Exception ignore) {
			}
			finally {
				if ( creationalContext != null ) {
					try {
						creationalContext.release();
					}
					catch (Exception ignore) {
					}
				}

				beanInstance = null;
				creationalContext = null;
				beanManager = null;
			}
		}

		protected abstract void doRelease();
	}
}
