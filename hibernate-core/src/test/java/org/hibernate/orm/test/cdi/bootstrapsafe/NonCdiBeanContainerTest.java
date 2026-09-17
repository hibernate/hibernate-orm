/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.bootstrapsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.internal.AbstractBeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.Helper;
import org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl;
import org.hibernate.resource.beans.spi.BeanInstanceCaching;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Contracts for fallback and custom containers without CDI dependencies.
///
/// @author Steve Ebersole
@Timeout(60)
class NonCdiBeanContainerTest {
	@Test
	void releaseEvictsOnlyTheMatchingCacheEntry() {
		final var registry = new ManagedBeanRegistryImpl( null );
		final var otherRegistry = new ManagedBeanRegistryImpl( null );
		try {
			final var first = registry.getBean( "first", Object.class );
			final var second = registry.getBean( "second", Object.class );
			final var distinct = registry.getBean( Object.class, BeanInstanceCaching.DISALLOW );
			registry.releaseBean( distinct );
			assertSame( first, registry.getBean( "first", Object.class ) );
			assertSame( second, registry.getBean( "second", Object.class ) );
			final var firstInstance = first.getBeanInstance();
			registry.releaseBean( first );
			final var replacement = registry.getBean( "first", Object.class );
			assertNotSame( first, replacement );
			assertNotSame( firstInstance, replacement.getBeanInstance() );
			registry.releaseBean( first );
			otherRegistry.releaseBean( replacement );
			assertSame( replacement, registry.getBean( "first", Object.class ) );
			assertSame( second, registry.getBean( "second", Object.class ) );
		}
		finally {
			registry.stop();
			otherRegistry.stop();
		}
	}

	@Test
	void bootstrapAcquisitionDefersCustomContainerCreation() {
		final var container = new TrackingBeanContainer();
		final var registry = new ManagedBeanRegistryImpl( container );
		try {
			final var bean = registry.getBootstrapSafeBean( Object.class );
			assertEquals( 0, container.creations );
			( (ContainedBean<?>) bean ).initialize();
			assertEquals( 1, container.creations );
			final var instance = bean.getBeanInstance();
			assertEquals( 1, container.creations );
			assertSame( instance, bean.getBeanInstance() );
			registry.releaseBean( bean );
			assertEquals( 1, container.releases );
			assertThrows( IllegalStateException.class, ( (ContainedBean<?>) bean )::initialize );
		}
		finally {
			registry.stop();
		}
	}

	@Test
	void releaseDelegatesToCustomContainer() {
		final var container = new TrackingBeanContainer();
		final var registry = new ManagedBeanRegistryImpl( container );
		final var bean = registry.getBean( Object.class, BeanInstanceCaching.DISALLOW );
		registry.releaseBean( bean );
		assertEquals( 1, container.releases );
		registry.releaseBean( bean );
		registry.stop();
		assertEquals( 1, container.releases );
	}

	@Test
	void concurrentFallbackReuseAndDistinctAcquisition() throws Exception {
		final var registry = new ManagedBeanRegistryImpl( null );
		assertTrue( registry.getBeanContainer().isFallback() );
		FallbackBean.created.set( 0 );
		final var start = new CountDownLatch( 1 );
		final List<Future<ManagedBean<FallbackBean>>> futures = new ArrayList<>();
		final var executor = Executors.newFixedThreadPool( 8 );
		try {
			for ( int i = 0; i < 40; i++ ) {
				futures.add( executor.submit( () -> {
					start.await();
					final var bean = registry.getBootstrapSafeBean( FallbackBean.class );
					bean.getBeanInstance();
					return bean;
				} ) );
			}
			start.countDown();
			final var shared = futures.get( 0 ).get();
			for ( var future : futures ) {
				assertSame( shared, future.get() );
			}
			assertEquals( 1, FallbackBean.created.get() );
			assertSame( shared, registry.getBean( FallbackBean.class ) );
			final var distinct = registry.getBean( FallbackBean.class, BeanInstanceCaching.DISALLOW );
			assertNotSame( shared.getBeanInstance(), distinct.getBeanInstance() );
			registry.releaseBean( shared );
			assertNotSame( shared, registry.getBean( FallbackBean.class ) );
		}
		finally {
			executor.shutdownNow();
			registry.stop();
		}
	}

	@Test
	void fallbackPreservesOptionalExtensionLookup() {
		try ( var services = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, true ).build() ) {
			assertTrue( services.requireService( ManagedBeanRegistry.class ).getBeanContainer().isFallback() );
			assertNull(
					Helper.getBeanContainer( services ) );
		}
	}

	@Test
	void fallbackHonorsNamedProducer() {
		final var registry = new ManagedBeanRegistryImpl( null );
		final var supplied = new Object();
		final var calls = new AtomicInteger();
		final var producer = new BeanInstanceProducer() {
			@Override
			public <B> B produceBeanInstance(Class<B> type) {
				throw new AssertionError( "Expected named acquisition" );
			}

			@Override
			public <B> B produceBeanInstance(String name, Class<B> type) {
				assertEquals( "custom", name );
				calls.incrementAndGet();
				return type.cast( supplied );
			}
		};
		final var bean = registry.getBean( "custom", Object.class, producer );
		assertSame( supplied, bean.getBeanInstance() );
		assertSame( bean, registry.getBean( "custom", Object.class, producer ) );
		assertEquals( 1, calls.get() );
		registry.releaseBean( bean );
		assertNotSame( bean, registry.getBean( "custom", Object.class, producer ) );
		assertEquals( 2, calls.get() );
		registry.stop();
	}

	public static class FallbackBean {
		static final AtomicInteger created = new AtomicInteger();

		public FallbackBean() {
			created.incrementAndGet();
		}
	}

	private static class TrackingBeanContainer extends AbstractBeanContainer {
		private final Set<ContainedBean<?>> beans = Collections.newSetFromMap( new IdentityHashMap<>() );
		int creations;
		int releases;

		@Override
		protected <B> ContainedBean<B> createBean(
				Class<B> type, LifecycleOptions options, BeanInstanceProducer producer) {
			return register( type, producer.produceBeanInstance( type ) );
		}

		@Override
		protected <B> ContainedBean<B> createBean(
				String name, Class<B> type, LifecycleOptions options, BeanInstanceProducer producer) {
			return register( type, producer.produceBeanInstance( name, type ) );
		}

		private <B> ContainedBean<B> register(Class<B> type, B instance) {
			// A custom handle with lifecycle bookkeeping owned by this container.
			final var bean = new ContainedBean<B>() {
				@Override
				public void initialize() {
					// No deferred initialization.
				}

				@Override
				public void release() {
					releases++;
				}

				@Override
				public Class<B> getBeanClass() {
					return type;
				}

				@Override
				public B getBeanInstance() {
					return instance;
				}
			};
			beans.add( bean );
			creations++;
			return bean;
		}

		@Override
		public void releaseBean(ManagedBean<?> bean) {
			super.releaseBean( bean );
			if ( beans.remove( bean ) ) {
				( (ContainedBean<?>) bean ).release();
			}
		}

		@Override
		public void stop() {
			super.stop();
			List.copyOf( beans ).forEach( this::releaseBean );
		}
	}
}
