/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.beans;

import org.hibernate.resource.beans.container.internal.DelayedBeanImpl;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.FallbackContainedBean;
import org.hibernate.resource.beans.internal.ManagedBeanRegistryImpl;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
	public void testUnsafeContainerReturnsDelayedBean() {
		final var registry = new ManagedBeanRegistryImpl( new EagerBeanContainer() );
		final var bean = registry.getBootstrapSafeBean( SimpleBean.class );
		assertInstanceOf( DelayedBeanImpl.class, bean );
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
}
