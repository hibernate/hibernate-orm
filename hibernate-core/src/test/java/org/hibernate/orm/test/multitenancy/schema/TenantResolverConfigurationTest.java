/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-10964")
public class TenantResolverConfigurationTest extends BaseCoreFunctionalTestCase {

	private TestCurrentTenantIdentifierResolver currentTenantResolver = new TestCurrentTenantIdentifierResolver();

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setCurrentTenantIdentifierResolver( currentTenantResolver );
	}

	@Test
	public void testConfiguration() throws Exception {
		assertSame(currentTenantResolver, sessionFactory().getCurrentTenantIdentifierResolver());
	}

	private static class TestCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver<Object> {
		private String currentTenantIdentifier;

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}

		@Override
		public Object resolveCurrentTenantIdentifier() {
			return currentTenantIdentifier;
		}
	}
}
