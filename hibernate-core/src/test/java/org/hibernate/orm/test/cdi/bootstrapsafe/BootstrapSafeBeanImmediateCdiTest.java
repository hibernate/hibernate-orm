/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.bootstrapsafe;

import jakarta.enterprise.inject.se.SeContainer;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerImmediateAccessImpl;
import org.hibernate.resource.beans.internal.DeferredContainerBean;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.tool.schema.Action;

import org.hibernate.orm.test.cdi.general.mixed.Helper;
import org.hibernate.orm.test.cdi.general.mixed.HostedBean;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ManagedBeanRegistry#getBootstrapSafeBean} with
 * {@link CdiBeanContainerImmediateAccessImpl}.
 * <p>
 * The immediate-access container is NOT bootstrap-safe, so
 * {@code getBootstrapSafeBean()} wraps the bean in a {@link DeferredContainerBean}
 * instead of delegating to the container directly.
 *
 * @author Sean Okafor
 */
public class BootstrapSafeBeanImmediateCdiTest {

	private SeContainer cdiContainer;
	private BootstrapServiceRegistry bsr;
	private StandardServiceRegistry ssr;
	private ManagedBeanRegistry registry;

	@BeforeEach
	public void setUp() {
		cdiContainer = Helper.createSeContainer();
		bsr = new BootstrapServiceRegistryBuilder().build();
		ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
				.build();
		registry = ssr.getService( ManagedBeanRegistry.class );
	}

	@AfterEach
	public void tearDown() {
		if ( ssr != null ) {
			ssr.close();
		}
		if ( bsr != null ) {
			bsr.close();
		}
		if ( cdiContainer != null ) {
			cdiContainer.close();
		}
	}

	@Test
	public void testReturnsDeferredBean() {
		assertThat( registry.getBeanContainer() ).isInstanceOf( CdiBeanContainerImmediateAccessImpl.class );

		final ManagedBean<HostedBean> bean = registry.getBootstrapSafeBean( HostedBean.class );
		assertThat( bean ).isInstanceOf( DeferredContainerBean.class );
	}

	@Test
	public void testDelayedBeanIsResolvable() {
		final ManagedBean<HostedBean> bean = registry.getBootstrapSafeBean( HostedBean.class );
		final HostedBean instance = bean.getBeanInstance();
		assertThat( instance ).isNotNull();
		assertThat( instance.getInjectedHostedBean() ).isNotNull();
	}

	@Test
	public void testCaching() {
		final ManagedBean<HostedBean> first = registry.getBootstrapSafeBean( HostedBean.class );
		final ManagedBean<HostedBean> second = registry.getBootstrapSafeBean( HostedBean.class );
		assertThat( first ).isSameAs( second );
	}

}
