/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Yanming Zhou
 */
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, value = "true"),
				@Setting(name = AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, value = "org.hibernate.orm.test.multitenancy.beancontainer.TestCurrentTenantIdentifierResolver"),
				@Setting(name = AvailableSettings.BEAN_CONTAINER, value = "org.hibernate.orm.test.multitenancy.beancontainer.TestBeanContainer")
		}
)
public class TenantResolverFromSettingsOverBeanContainerTest extends AbstractTenantResolverBeanContainerTest {

	@Test
	void tenantResolverFromSettingsShouldBeUsed(SessionFactoryScope scope) {
		CurrentTenantIdentifierResolver<?> tenantResolver = scope.getSessionFactory().getCurrentTenantIdentifierResolver();
		assertThat(tenantResolver, instanceOf(TestCurrentTenantIdentifierResolver.class));
		assertThat(tenantResolver, is(not(TestCurrentTenantIdentifierResolver.INSTANCE_FOR_BEAN_CONTAINER)));
	}

}
