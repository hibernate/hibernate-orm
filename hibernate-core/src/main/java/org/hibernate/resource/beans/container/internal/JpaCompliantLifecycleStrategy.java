/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.resource.beans.container.internal;

import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.jboss.logging.Logger;

/**
 * A {@link BeanLifecycleStrategy} to use when JPA compliance is required
 * (i.e. when the bean lifecycle is to be managed by the JPA runtime, not the CDI runtime).
 *
 * The main characteristic of this strategy is that each requested bean is instantiated directly
 * and guaranteed to not be shared in the CDI context.
 *
 * In particular, @Singleton-scoped or @ApplicationScoped beans are instantiated directly by this strategy,
 * even if there is already an instance in the CDI context.
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
				( (CdiBasedBeanContainer) beanContainer ).getUsableBeanManager()
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
				( (CdiBasedBeanContainer) beanContainer ).getUsableBeanManager()
		);
	}



	private static class BeanImpl<B> implements ContainedBeanImplementor<B> {
		private final Class<B> beanType;

		private BeanInstanceProducer fallbackProducer;

		private BeanManager beanManager;
		private InjectionTarget<B> injectionTarget;
		private CreationalContext<B> creationalContext;

		private B beanInstance;

		public BeanImpl(
				Class<B> beanType,
				BeanInstanceProducer fallbackProducer,
				BeanManager beanManager) {
			this.beanType = beanType;
			this.fallbackProducer = fallbackProducer;
			this.beanManager = beanManager;
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

			final AnnotatedType<B> annotatedType;
			try {
				annotatedType = beanManager.createAnnotatedType( beanType );
			}
			catch (Exception e) {
				throw new IllegalStateException( new NotYetReadyException( e ) );
			}

			try {
				this.injectionTarget = beanManager.createInjectionTarget( annotatedType );
				this.creationalContext = beanManager.createCreationalContext( null );

				this.beanInstance = this.injectionTarget.produce( creationalContext );
				injectionTarget.inject( beanInstance, creationalContext );

				injectionTarget.postConstruct( beanInstance );
			}
			catch (NotYetReadyException e) {
				throw e;
			}
			catch (Exception e) {
				log.debugf( "Error resolving CDI bean [%s] - using fallback" );
				this.beanInstance = fallbackProducer.produceBeanInstance( beanType );

				try {
					if ( this.creationalContext != null ) {
						this.creationalContext.release();
					}
				}
				catch (Exception ignore) {
				}

				this.creationalContext = null;
				this.injectionTarget = null;
			}

			this.beanManager = null;
		}

		@Override
		public void release() {
			if ( beanInstance == null ) {
				return;
			}

			try {
				if ( injectionTarget == null ) {
					// todo : BeanInstanceProducer#release?
					return;
				}
				injectionTarget.preDestroy( beanInstance );
				injectionTarget.dispose( beanInstance );
				this.creationalContext.release();
			}
			catch (Exception ignore) {

			}
			finally {
				this.beanInstance = null;
				this.creationalContext = null;
				this.injectionTarget = null;
			}
		}
	}


	private static class NamedBeanImpl<B> implements ContainedBeanImplementor<B> {
		private final Class<B> beanType;
		private final String beanName;

		private BeanInstanceProducer fallbackProducer;

		private BeanManager beanManager;
		private Bean<B> bean;
		private CreationalContext<B> creationalContext;

		private B beanInstance;

		private NamedBeanImpl(
				String beanName,
				Class<B> beanType,
				BeanInstanceProducer fallbackProducer,
				BeanManager beanManager) {
			this.beanName = beanName;
			this.beanType = beanType;
			this.fallbackProducer = fallbackProducer;
			this.beanManager = beanManager;
		}

		@Override
		public B getBeanInstance() {
			if ( beanInstance == null ) {
				initialize();
			}
			return beanInstance;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void initialize() {
			if ( beanInstance != null ) {
				return;
			}


			try {
				this.creationalContext = beanManager.createCreationalContext( null );
			}
			catch (Exception e) {
				throw new NotYetReadyException( e );
			}

			try {
				Set<Bean<?>> beans = beanManager.getBeans( beanType, new NamedBeanQualifier( beanName ) );
				this.bean = (Bean<B>) beanManager.resolve( beans );
				this.beanInstance = bean.create( creationalContext );
			}
			catch (Exception e) {
				log.debugf( "Error resolving CDI bean [%s] - using fallback" );
				this.beanInstance = fallbackProducer.produceBeanInstance( beanName, beanType );

				try {
					if ( this.creationalContext != null ) {
						this.creationalContext.release();
					}
				}
				catch (Exception ignore) {
				}

				this.creationalContext = null;
				this.bean = null;
			}
		}

		@Override
		public void release() {
			if ( beanInstance == null ) {
				return;
			}

			try {
				if ( bean == null ) {
					// todo : BeanInstanceProducer#release?
					return;
				}
				bean.destroy( beanInstance, creationalContext );
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

				this.beanInstance = null;
				this.creationalContext = null;
				this.bean = null;
				this.beanManager = null;
			}
		}

	}
}
