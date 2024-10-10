/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Yanming Zhou
 */
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.ALLOW_EXTENSIONS_IN_CDI, value = "true"),
				@Setting(name = AvailableSettings.BEAN_CONTAINER, value = "org.hibernate.orm.test.multitenancy.beancontainer.TestBeanContainer")
		}
)
public class TenantResolverFromBeanContainerTest extends AbstractTenantResolverBeanContainerTest {

	@Test
	void tenantResolverFromBeanContainerShouldBeUsed(SessionFactoryScope scope) {
		CurrentTenantIdentifierResolver<?> tenantResolver = scope.getSessionFactory().getCurrentTenantIdentifierResolver();
		assertThat(tenantResolver, is(TestCurrentTenantIdentifierResolver.INSTANCE_FOR_BEAN_CONTAINER));
	}

}
