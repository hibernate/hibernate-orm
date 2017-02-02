/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.multitenancy.schema;

import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-10964")
public class TenantResolverConfigurationTest extends BaseCoreFunctionalTestCase {

	private TestCurrentTenantIdentifierResolver currentTenantResolver = new TestCurrentTenantIdentifierResolver();

	@Override
	protected Configuration constructAndConfigureConfiguration() {
		Configuration configuration = super.constructAndConfigureConfiguration();
		configuration.setCurrentTenantIdentifierResolver( currentTenantResolver );
		return configuration;
	}

	@Test
	public void testConfiguration() throws Exception {
		assertSame(currentTenantResolver, sessionFactory().getCurrentTenantIdentifierResolver());
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
