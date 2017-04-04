/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.multitenancy.schema;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.junit.Assert;

/**
 * SessionFactory has to use the {@link CurrentTenantIdentifierResolver} when
 * {@link SessionFactory#openSession()} is called.
 *
 * @author Stefan Schulze
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-7306")
@RequiresDialectFeature( value = ConnectionProviderBuilder.class )
public class CurrentTenantResolverMultiTenancyTest extends SchemaBasedMultiTenancyTest {

	private TestCurrentTenantIdentifierResolver currentTenantResolver = new TestCurrentTenantIdentifierResolver();

	@Override
	protected void configure(SessionFactoryBuilder sfb) {
		sfb.applyCurrentTenantIdentifierResolver( currentTenantResolver );
	}

	@Override
	protected Session getNewSession(String tenant) {
		currentTenantResolver.currentTenantIdentifier = tenant;
		Session session = sessionFactory.openSession();
		Assert.assertEquals( tenant, session.getTenantIdentifier() );
		return session;
	}


	private static class TestCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {
		private String currentTenantIdentifier;

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}

		@Override
		public String resolveCurrentTenantIdentifier() {
			return currentTenantIdentifier;
		}
	}
}
