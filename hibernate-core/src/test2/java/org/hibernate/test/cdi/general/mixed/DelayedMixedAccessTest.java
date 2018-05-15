/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.mixed;

import javax.enterprise.inject.se.SeContainer;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerBuilder;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerDelayedAccessImpl;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.tool.schema.Action;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class DelayedMixedAccessTest implements BeanContainer.LifecycleOptions {
	@Override
	public boolean canUseCachedReferences() {
		return true;
	}

	@Override
	public boolean useJpaCompliantCreation() {
		return true;
	}

	@Test
	public void testDelayedMixedAccess() {
		try ( final SeContainer cdiContainer = Helper.createSeContainer() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();

			final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.applySetting( AvailableSettings.DELAY_CDI_ACCESS, "true" )
					.build();

			final BeanContainer beanContainer = CdiBeanContainerBuilder.fromBeanManagerReference(
					cdiContainer.getBeanManager(),
					ssr
			);

			assertThat( beanContainer, instanceOf( CdiBeanContainerDelayedAccessImpl.class ) );

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
