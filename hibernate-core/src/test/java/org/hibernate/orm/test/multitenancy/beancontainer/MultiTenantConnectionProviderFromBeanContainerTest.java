/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.orm.test.multitenancy.AbstractMultiTenancyTest;
import org.hibernate.orm.test.multitenancy.ConfigurableMultiTenantConnectionProvider;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Yanming Zhou
 */
@RequiresDialect(H2Dialect.class)
public class MultiTenantConnectionProviderFromBeanContainerTest extends AbstractMultiTenancyTest {

	private ConfigurableMultiTenantConnectionProvider providerFromBeanContainer;

	@Override
	protected Map<String, Object> createSettings() {
		Map<String, Object> settings = new HashMap<>();

		providerFromBeanContainer = new ConfigurableMultiTenantConnectionProvider( connectionProviderMap);
		settings.put( AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, "true" );
		settings.put( AvailableSettings.BEAN_CONTAINER, new BeanContainer() {
			@Override
			@SuppressWarnings("unchecked")
			public <B> ContainedBean<B> getBean(
					Class<B> beanType,
					LifecycleOptions lifecycleOptions,
					BeanInstanceProducer fallbackProducer) {
				return () -> (B) ( beanType == MultiTenantConnectionProvider.class ? providerFromBeanContainer : fallbackProducer.produceBeanInstance( beanType ) );
			}

			@Override
			public <B> ContainedBean<B> getBean(
					String name,
					Class<B> beanType,
					LifecycleOptions lifecycleOptions,
					BeanInstanceProducer fallbackProducer) {
				return () -> (B) fallbackProducer.produceBeanInstance( beanType );
			}

			@Override
			public void stop() {

			}
		} );
		return settings;
	}

	@Override
	protected String tenantUrl(String originalUrl, String tenantIdentifier) {
		return originalUrl.replace("db1", tenantIdentifier);
	}

	@Test
	public void testProviderInUse() {
		MultiTenantConnectionProvider<?> providerInUse = ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService( MultiTenantConnectionProvider.class );
		assertSame( providerInUse, expectedProviderInUse());
	}

	protected MultiTenantConnectionProvider<?> expectedProviderInUse() {
		return providerFromBeanContainer;
	}
}
