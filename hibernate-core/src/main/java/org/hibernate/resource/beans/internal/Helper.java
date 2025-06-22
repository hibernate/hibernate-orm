/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
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
		return allowExtensionsInCdi( serviceRegistry )
				? serviceRegistry.requireService( ManagedBeanRegistry.class ).getBeanContainer()
				: null;
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
