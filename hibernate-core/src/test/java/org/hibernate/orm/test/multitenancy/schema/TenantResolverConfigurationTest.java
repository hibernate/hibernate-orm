/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.schema;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-10964")
@SessionFactory
@DomainModel
public class TenantResolverConfigurationTest implements SessionFactoryProducer {

	private TestCurrentTenantIdentifierResolver currentTenantResolver = new TestCurrentTenantIdentifierResolver();

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
		sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( currentTenantResolver);
		return (SessionFactoryImplementor) sessionFactoryBuilder.build();
	}

	@Test
	public void testConfiguration(SessionFactoryScope scope) {
		assertSame(currentTenantResolver, scope.getSessionFactory().getCurrentTenantIdentifierResolver());
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
