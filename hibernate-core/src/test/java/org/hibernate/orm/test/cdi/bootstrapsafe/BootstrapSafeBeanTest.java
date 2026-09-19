/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.bootstrapsafe;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.container.spi.FallbackContainedBean;
import org.hibernate.resource.beans.internal.DeferredContainerBean;
import org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl;
import org.hibernate.resource.beans.spi.BeanInstanceAccess;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.service.spi.Stoppable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Sean Okafor
 */
public class BootstrapSafeBeanTest {

	@Test
	public void testNullContainerReturnsFallbackBean() {
		final var registry = new ManagedBeanRegistryImpl( null );
		final var bean = registry.getBootstrapSafeBean( SimpleBean.class );
		assertNotNull( bean );
		assertInstanceOf( FallbackContainedBean.class, bean );
	}

	@Test
	public void testNullContainerBeanIsUsable() {
		final var registry = new ManagedBeanRegistryImpl( null );
		final var bean = registry.getBootstrapSafeBean( SimpleBean.class );
		assertInstanceOf( SimpleBean.class, bean.getBeanInstance() );
	}

	@Test
	public void testUnsafeContainerReturnsDeferredBean() {
		final var registry = new ManagedBeanRegistryImpl( new EagerBeanContainer() );
		final var bean = registry.getBootstrapSafeBean( SimpleBean.class );
		assertInstanceOf( DeferredContainerBean.class, bean );
	}

	@Test
	public void testUnsafeContainerDoesNotEagerlyInitialize() {
		final var container = new EagerBeanContainer();
		final var registry = new ManagedBeanRegistryImpl( container );
		registry.getBootstrapSafeBean( SimpleBean.class );
		assertSame( 0, container.getBeanCallCount );
	}

	@Test
	public void testCachingReturnsSameBean() {
		final var registry = new ManagedBeanRegistryImpl( new EagerBeanContainer() );
		final var first = registry.getBootstrapSafeBean( SimpleBean.class );
		final var second = registry.getBootstrapSafeBean( SimpleBean.class );
		assertSame( first, second );
	}

	@Test
	public void testNonCdiContainerNoCastException() {
		final var registry = new ManagedBeanRegistryImpl( new EagerBeanContainer() );
		final var bean = registry.getBootstrapSafeBean( SimpleBean.class );
		assertInstanceOf( SimpleBean.class, bean.getBeanInstance() );
	}

	@Test
	public void testDistinctBeansReturnDifferentInstances() {
		final var registry = new ManagedBeanRegistryImpl( new EagerBeanContainer() );
		final ManagedBean<SimpleBean> first = registry.getBean( SimpleBean.class, BeanInstanceAccess.DISTINCT );
		final ManagedBean<SimpleBean> second = registry.getBean( SimpleBean.class, BeanInstanceAccess.DISTINCT );
		assertNotSame( first, second );
		assertNotSame( first.getBeanInstance(), second.getBeanInstance() );
	}

	@Test
	public void testReleaseBeanCallsRelease() {
		final var container = new TrackingBeanContainer();
		final var registry = new ManagedBeanRegistryImpl( container );
		final ManagedBean<SimpleBean> bean = registry.getBean( SimpleBean.class, BeanInstanceAccess.DISTINCT );
		registry.releaseBean( bean );
		final TrackingContainedBean<?> tracked = container.lastCreatedBean();
		assertEquals( 1, tracked.releaseCount );
	}

	@Test
	public void testStopReleasesUnreleasedDistinctBeans() {
		final var container = new TrackingBeanContainer();
		final var registry = new ManagedBeanRegistryImpl( container );
		registry.getBean( SimpleBean.class, BeanInstanceAccess.DISTINCT );
		final TrackingContainedBean<?> tracked = container.lastCreatedBean();
		( (Stoppable) registry ).stop();
		assertEquals( 1, tracked.releaseCount );
	}

	@Test
	public void testNoDoubleDestruction() {
		final var container = new TrackingBeanContainer();
		final var registry = new ManagedBeanRegistryImpl( container );
		final ManagedBean<SimpleBean> bean = registry.getBean( SimpleBean.class, BeanInstanceAccess.DISTINCT );
		final TrackingContainedBean<?> tracked = container.lastCreatedBean();
		registry.releaseBean( bean );
		( (Stoppable) registry ).stop();
		assertEquals( 1, tracked.releaseCount );
	}

	public static class SimpleBean {
		public SimpleBean() {}
	}

	static class EagerBeanContainer implements BeanContainer {
		int getBeanCallCount = 0;

		@Override
		public <B> ContainedBean<B> getBean(
				Class<B> beanType,
				LifecycleOptions lifecycleOptions,
				BeanInstanceProducer fallbackProducer) {
			getBeanCallCount++;
			return new FallbackContainedBean<>( beanType, fallbackProducer );
		}

		@Override
		public <B> ContainedBean<B> getBean(
				String name,
				Class<B> beanType,
				LifecycleOptions lifecycleOptions,
				BeanInstanceProducer fallbackProducer) {
			getBeanCallCount++;
			return new FallbackContainedBean<>( name, beanType, fallbackProducer );
		}

		@Override
		public void stop() {}
	}

	static class TrackingContainedBean<B> implements ContainedBeanImplementor<B> {
		private final Class<B> beanClass;
		private final B beanInstance;
		int releaseCount = 0;

		TrackingContainedBean(Class<B> beanClass, B beanInstance) {
			this.beanClass = beanClass;
			this.beanInstance = beanInstance;
		}

		@Override
		public Class<B> getBeanClass() {
			return beanClass;
		}

		@Override
		public B getBeanInstance() {
			return beanInstance;
		}

		@Override
		public void initialize() {}

		@Override
		public void release() {
			releaseCount++;
		}
	}

	static class TrackingBeanContainer implements BeanContainer {
		private final List<TrackingContainedBean<?>> createdBeans = new ArrayList<>();

		@Override
		public <B> ContainedBean<B> getBean(
				Class<B> beanType,
				LifecycleOptions lifecycleOptions,
				BeanInstanceProducer fallbackProducer) {
			final B instance = fallbackProducer.produceBeanInstance( beanType );
			final var bean = new TrackingContainedBean<>( beanType, instance );
			createdBeans.add( bean );
			return bean;
		}

		@Override
		public <B> ContainedBean<B> getBean(
				String name,
				Class<B> beanType,
				LifecycleOptions lifecycleOptions,
				BeanInstanceProducer fallbackProducer) {
			final B instance = fallbackProducer.produceBeanInstance( name, beanType );
			final var bean = new TrackingContainedBean<>( beanType, instance );
			createdBeans.add( bean );
			return bean;
		}

		TrackingContainedBean<?> lastCreatedBean() {
			return createdBeans.get( createdBeans.size() - 1 );
		}

		@Override
		public void stop() {}
	}
}
