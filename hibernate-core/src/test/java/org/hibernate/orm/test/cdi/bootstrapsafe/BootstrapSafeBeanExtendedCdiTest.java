/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.bootstrapsafe;

import jakarta.enterprise.inject.se.SeContainer;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerExtendedAccessImpl;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.tool.schema.Action;

import org.hibernate.orm.test.cdi.general.mixed.Helper;
import org.hibernate.orm.test.cdi.general.mixed.HostedBean;
import org.hibernate.orm.test.cdi.testsupport.TestingExtendedBeanManager;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ManagedBeanRegistry#getBootstrapSafeBean} and
 * {@link ManagedBeanRegistry#getUncachedBootstrapSafeBean} with
 * {@link CdiBeanContainerExtendedAccessImpl}.
 * <p>
 * The extended-access container is inherently bootstrap-safe, so
 * {@code getBootstrapSafeBean()} delegates to {@code getBean()}.
 * The key scenario is acquiring a bean reference before CDI is ready,
 * then dereferencing it after {@code notifyListenerReady()}.
 *
 * @author Sean Okafor
 */
public class BootstrapSafeBeanExtendedCdiTest {

	private TestingExtendedBeanManager extendedBeanManager;
	private StandardServiceRegistry ssr;
	private ManagedBeanRegistry registry;

	@BeforeEach
	public void setUp() {
		extendedBeanManager = TestingExtendedBeanManager.create();
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( AvailableSettings.CDI_BEAN_MANAGER, extendedBeanManager )
				.build();
		registry = ssr.getService( ManagedBeanRegistry.class );
	}

	@AfterEach
	public void tearDown() {
		if ( ssr != null ) {
			ssr.close();
		}
	}

	@Test
	public void testAcquireBeforeCdiReady() {
		assertThat( registry.getBeanContainer() ).isInstanceOf( CdiBeanContainerExtendedAccessImpl.class );

		// acquire bean reference before CDI is available, this must not fail
		final ManagedBean<HostedBean> bean = registry.getBootstrapSafeBean( HostedBean.class );
		assertThat( bean ).isNotNull();

		// start CDI and notify the extended bean manager
		try ( final SeContainer cdiContainer = Helper.createSeContainer() ) {
			extendedBeanManager.notifyListenerReady( cdiContainer.getBeanManager() );

			// now the bean instance should be resolvable with CDI injection
			final HostedBean instance = bean.getBeanInstance();
			assertThat( instance ).isNotNull();
			assertThat( instance.getInjectedHostedBean() ).isNotNull();

			extendedBeanManager.notifyListenerShuttingDown( cdiContainer.getBeanManager() );
		}
	}

	@Test
	public void testCaching() {
		final ManagedBean<HostedBean> first = registry.getBootstrapSafeBean( HostedBean.class );
		final ManagedBean<HostedBean> second = registry.getBootstrapSafeBean( HostedBean.class );
		assertThat( first ).isSameAs( second );
	}

	@Test
	public void testUncachedBootstrapSafeBean() {
		final ManagedBean<HostedBean> uncached = registry.getUncachedBootstrapSafeBean( HostedBean.class );
		assertThat( uncached ).isNotNull();

		try ( final SeContainer cdiContainer = Helper.createSeContainer() ) {
			extendedBeanManager.notifyListenerReady( cdiContainer.getBeanManager() );

			final HostedBean instance = uncached.getBeanInstance();
			assertThat( instance ).isNotNull();
			assertThat( instance.getInjectedHostedBean() ).isNotNull();

			extendedBeanManager.notifyListenerShuttingDown( cdiContainer.getBeanManager() );
		}
	}
}
