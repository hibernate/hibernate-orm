/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.general.mixed;

import jakarta.enterprise.inject.se.SeContainer;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerBuilder;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerImmediateAccessImpl;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for "mixed access" to both hosted and non-hosted beans.
 *
 * @author Steve Ebersole
 */
public class ImmediateMixedAccessTests implements BeanContainer.LifecycleOptions {
	@Override
	public boolean canUseCachedReferences() {
		return true;
	}

	@Override
	public boolean useJpaCompliantCreation() {
		return true;
	}

	@Test
	public void testImmediateMixedAccess() {
		try ( final SeContainer cdiContainer = Helper.createSeContainer();
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build() ) {

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.build();

			final BeanContainer beanContainer = CdiBeanContainerBuilder.fromBeanManagerReference(
					cdiContainer.getBeanManager(),
					ssr
			);

			assertThat( beanContainer, instanceOf( CdiBeanContainerImmediateAccessImpl.class ) );

			final ContainedBean<HostedBean> hostedBean = beanContainer.getBean(
					HostedBean.class,
					this,
					FallbackBeanInstanceProducer.INSTANCE
			);

			assertThat( hostedBean, notNullValue() );
			assertThat( hostedBean.getBeanInstance(), notNullValue() );

			assertThat( hostedBean.getBeanInstance().getInjectedHostedBean(), notNullValue() );

			final ContainedBean<NonHostedBean> nonHostedBean = beanContainer.getBean(
					NonHostedBean.class,
					this,
					FallbackBeanInstanceProducer.INSTANCE
			);

			assertThat( nonHostedBean, notNullValue() );
			assertThat( nonHostedBean.getBeanInstance(), notNullValue() );
		}
	}
}
