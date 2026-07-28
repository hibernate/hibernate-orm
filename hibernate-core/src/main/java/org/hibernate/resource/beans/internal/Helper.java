/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import jakarta.annotation.Nullable;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.FallbackContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.ServiceRegistry;

import java.util.function.Supplier;

import static org.hibernate.cfg.ManagedBeanSettings.ALLOW_EXTENSIONS_IN_CDI;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

/**
 * @author Steve Ebersole
 */
public final class Helper {

	private Helper() {
	}

	public static String determineBeanCacheKey(Class<?> beanType) {
		return beanType.getName();
	}

	public static String determineBeanCacheKey(String name, Class<?> beanType) {
		return beanType.getName() + ':' + name;
	}

	public static boolean allowExtensionsInCdi(ServiceRegistry serviceRegistry) {
		return serviceRegistry.requireService( ConfigurationService.class )
				.getSetting( ALLOW_EXTENSIONS_IN_CDI, BOOLEAN, false );
	}

	@Nullable
	public static BeanContainer getBeanContainer(ServiceRegistry serviceRegistry) {
		return getBeanContainer(
				serviceRegistry.requireService( ConfigurationService.class ),
				serviceRegistry.requireService( ManagedBeanRegistry.class )
		);
	}

	@Nullable
	public static BeanContainer getBeanContainer(
			ConfigurationService configurationService,
			ManagedBeanRegistry managedBeanRegistry) {
		return allowExtensionsInCdi( configurationService )
				? managedBeanRegistry.getBeanContainer()
				: null;
	}

	public static boolean allowExtensionsInCdi(ConfigurationService configurationService) {
		return configurationService.getSetting( ALLOW_EXTENSIONS_IN_CDI, BOOLEAN, false );
	}

	@Nullable
	public static <T> T getBean(
			@Nullable BeanContainer container,
			Class<T> beanType,
			boolean canUseCachedReferences,
			boolean useJpaCompliantCreation,
			@Nullable Supplier<T> fallbackSupplier) {
		return container == null ? null
				: containedBean( container, beanType, canUseCachedReferences, useJpaCompliantCreation, fallbackSupplier )
						.getBeanInstance();
	}

	public static <T> ManagedBean<T> getManagedBean(
			@Nullable BeanContainer container,
			Class<T> beanType,
			boolean canUseCachedReferences,
			boolean useJpaCompliantCreation,
			Supplier<T> fallbackSupplier) {
		final BeanInstanceProducer fallbackProducer = new BeanInstanceProducer() {
			@Override
			@SuppressWarnings("unchecked")
			public <B> B produceBeanInstance(Class<B> beanType) {
				return (B) fallbackSupplier.get();
			}

			@Override
			public <B> B produceBeanInstance(String name, Class<B> beanType) {
				throw new UnsupportedOperationException( "The method shouldn't be called" );
			}
		};
		final ManagedBean<T> managedBean = container == null
				? new FallbackContainedBean<>( beanType, fallbackProducer )
				: container.getBean(
						beanType,
						new BeanContainer.LifecycleOptions() {
							@Override
							public boolean canUseCachedReferences() {
								return canUseCachedReferences;
							}

							@Override
							public boolean useJpaCompliantCreation() {
								return useJpaCompliantCreation;
							}
						},
						fallbackProducer
				);
		return new StableManagedBean<>( managedBean );
	}

	/**
	 * Stabilizes the bean reference represented by a {@link ManagedBean}.
	 * Containers are expected to represent one contextual reference, but some
	 * integrations compute that reference from each {@code getBeanInstance()}
	 * call. Generator preparation requires the configured/exported instance to
	 * be the instance handed to the runtime model.
	 */
	private static final class StableManagedBean<T> implements ManagedBean<T> {
		private final ManagedBean<T> delegate;
		private final T beanInstance;

		private StableManagedBean(ManagedBean<T> delegate) {
			this.delegate = delegate;
			this.beanInstance = delegate.getBeanInstance();
		}

		@Override
		public Class<T> getBeanClass() {
			return delegate.getBeanClass();
		}

		@Override
		public T getBeanInstance() {
			return beanInstance;
		}
	}

	private static <T> ContainedBean<T> containedBean(
			BeanContainer container,
			Class<T> beanType,
			boolean canUseCachedReferences,
			boolean useJpaCompliantCreation,
			Supplier<T> fallbackSupplier) {
		return container.getBean(
				beanType,
				new BeanContainer.LifecycleOptions() {
					@Override
					public boolean canUseCachedReferences() {
						return canUseCachedReferences;
					}

					@Override
					public boolean useJpaCompliantCreation() {
						return useJpaCompliantCreation;
					}
				},
				new BeanInstanceProducer() {
					@Override @SuppressWarnings( "unchecked" )
					public <B> B produceBeanInstance(Class<B> beanType) {
						return fallbackSupplier != null
								? (B) fallbackSupplier.get()
								: null;
					}

					@Override
					public <B> B produceBeanInstance(String name, Class<B> beanType) {
						throw new UnsupportedOperationException( "The method shouldn't be called" );
					}
				}
		);
	}

}
