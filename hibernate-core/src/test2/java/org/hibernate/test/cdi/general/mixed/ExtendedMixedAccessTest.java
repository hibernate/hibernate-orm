/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.mixed;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.internal.CdiBeanContainerExtendedAccessImpl;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.tool.schema.Action;

import org.hibernate.test.cdi.testsupport.TestingExtendedBeanManager;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class ExtendedMixedAccessTest implements BeanContainer.LifecycleOptions {
	@Test
	public void testExtendedMixedAccess() {
		doTest( TestingExtendedBeanManager.create() );
	}

	/**
	 * NOTE : we use the deprecated one here to make sure this continues to work.
	 * Scott still uses this in WildFly and we need it to continue to work there
	 */
	@Test
	public void testLegacyExtendedMixedAccess() {
		doTest( TestingExtendedBeanManager.createLegacy() );
	}

	private void doTest(TestingExtendedBeanManager extendedBeanManager) {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
				.applySetting( AvailableSettings.CDI_BEAN_MANAGER, extendedBeanManager )
				.build();

		final BeanContainer beanContainer = ssr.getService( ManagedBeanRegistry.class ).getBeanContainer();

		assertThat( beanContainer, instanceOf( CdiBeanContainerExtendedAccessImpl.class ) );

		try ( final SeContainer cdiContainer = Helper.createSeContainer() ) {
			final BeanManager beanManager = cdiContainer.getBeanManager();
			extendedBeanManager.notifyListenerReady( beanManager );

			assertThat( beanManager, sameInstance( ( (CdiBeanContainerExtendedAccessImpl) beanContainer ).getUsableBeanManager() ) );

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

			extendedBeanManager.notifyListenerShuttingDown( beanManager );
		}
	}

	@Override
	public boolean canUseCachedReferences() {
		return true;
	}

	@Override
	public boolean useJpaCompliantCreation() {
		return true;
	}
}
