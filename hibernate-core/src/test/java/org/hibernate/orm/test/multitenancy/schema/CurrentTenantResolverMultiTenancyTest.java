/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.junit.Assert;

/**
 * SessionFactory has to use the {@link CurrentTenantIdentifierResolver} when
 * {@link SessionFactory#openSession()} is called.
 *
 * @author Stefan Schulze
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-7306")
@RequiresDialectFeature( value = ConnectionProviderBuilder.class )
public class CurrentTenantResolverMultiTenancyTest extends SchemaBasedMultiTenancyTest {

	private TestCurrentTenantIdentifierResolver currentTenantResolver = new TestCurrentTenantIdentifierResolver();

	@Override
	protected void configure(SessionFactoryBuilder sfb) {
		sfb.applyCurrentTenantIdentifierResolver( currentTenantResolver );
	}

	@Override
	protected SessionBuilder newSession(String tenant) {
		currentTenantResolver.currentTenantIdentifier = tenant;
		SessionBuilder sessionBuilder = sessionFactory.withOptions();
		try(Session session = sessionBuilder.openSession()) {
			Assert.assertEquals( tenant, session.getTenantIdentifier() );
		}
		return sessionBuilder;
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
