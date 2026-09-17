/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.multitenancy.AbstractMultiTenancyTest;
import org.hibernate.orm.test.multitenancy.ConfigurableMultiTenantConnectionProvider;
import org.hibernate.resource.beans.container.internal.AbstractBeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
		settings.put( AvailableSettings.BEAN_CONTAINER, new AbstractBeanContainer() {
			@Override
			@SuppressWarnings("unchecked")
			protected <B> ContainedBean<B> createBean(
					Class<B> beanType,
					LifecycleOptions lifecycleOptions,
					BeanInstanceProducer fallbackProducer) {
				return new ContainedBean<>() {
					@Override
					public void initialize() {
						// No deferred initialization.
					}

					@Override
					public void release() {
						// No resources owned by this handle.
					}

					@Override
					public B getBeanInstance() {
						return (B) (beanType == MultiTenantConnectionProvider.class ? providerFromBeanContainer : fallbackProducer.produceBeanInstance( beanType ) );
					}
					@Override
					public Class<B> getBeanClass() {
						return beanType;
					}
				};
			}

			@Override
			protected <B> ContainedBean<B> createBean(
					String name,
					Class<B> beanType,
					LifecycleOptions lifecycleOptions,
					BeanInstanceProducer fallbackProducer) {
				return new ContainedBean<>() {
					@Override
					public void initialize() {
						// No deferred initialization.
					}

					@Override
					public void release() {
						// No resources owned by this handle.
					}

					@Override
					public B getBeanInstance() {
						return fallbackProducer.produceBeanInstance( beanType );
					}
					@Override
					public Class<B> getBeanClass() {
						return beanType;
					}
				};
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
		MultiTenantConnectionProvider<?> providerInUse =
				((SessionFactoryImplementor) sessionFactory).getServiceRegistry()
						.getService( MultiTenantConnectionProvider.class );
		assertThat( providerInUse).isSameAs( expectedProviderInUse() );
	}

	protected MultiTenantConnectionProvider<?> expectedProviderInUse() {
		return providerFromBeanContainer;
	}
}
