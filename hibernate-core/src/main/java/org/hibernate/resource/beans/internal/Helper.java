/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.resource.beans.container.internal.ContainerManagedLifecycleStrategy;
import org.hibernate.resource.beans.container.internal.JpaCompliantLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
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

	@SuppressWarnings("unused")
	public static BeanLifecycleStrategy getLifecycleStrategy(boolean shouldRegistryManageLifecycle) {
		return shouldRegistryManageLifecycle
				? JpaCompliantLifecycleStrategy.INSTANCE
				: ContainerManagedLifecycleStrategy.INSTANCE;
	}

	@Nullable
	public static BeanContainer getBeanContainer(ServiceRegistry serviceRegistry) {
		return allowExtensionsInCdi( serviceRegistry ) ? serviceRegistry.requireService( ManagedBeanRegistry.class ).getBeanContainer() : null;
	}

	@SuppressWarnings( "unchecked" )
	@Nullable
	public static <T> T getBean(@Nullable BeanContainer beanContainer, Class<?> beanType, boolean canUseCachedReferences, boolean useJpaCompliantCreation, @Nullable Supplier<T> fallbackSupplier) {
		if ( beanContainer == null ) {
			return null;
		}
		return (T) beanContainer.getBean(
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

					@Override
					public <B> B produceBeanInstance(Class<B> beanType) {
						return (B) (fallbackSupplier != null ? fallbackSupplier.get() : null);
					}

					@Override
					public <B> B produceBeanInstance(String name, Class<B> beanType) {
						throw new UnsupportedOperationException("The method shouldn't be called");
					}
				}
		).getBeanInstance();
	}

}
