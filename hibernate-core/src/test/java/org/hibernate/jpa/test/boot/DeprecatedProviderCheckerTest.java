/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.boot;


import org.hibernate.internal.HEMLogging;
import org.hibernate.jpa.boot.spi.ProviderChecker;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that deprecated (and removed) provider, "org.hibernate.ejb.HibernatePersistence",
 * is recognized as a Hibernate persistence provider.
 *
 * @author Gail Badner
 */
public class DeprecatedProviderCheckerTest {
	final static String DEPRECATED_PROVIDER_NAME = "org.hibernate.ejb.HibernatePersistence";

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			HEMLogging.messageLogger( ProviderChecker.class.getName() )
	);

	@Test
	@TestForIssue( jiraKey = "HHH-13027")
	public void testDeprecatedProvider() {
		Triggerable triggerable = logInspection.watchForLogMessages( "HHH015016" );
		triggerable.reset();
		assertTrue( ProviderChecker.hibernateProviderNamesContain( DEPRECATED_PROVIDER_NAME ) );
		triggerable.wasTriggered();
		assertEquals(
				"HHH015016: Encountered a deprecated javax.persistence.spi.PersistenceProvider [org.hibernate.ejb.HibernatePersistence]; [org.hibernate.jpa.HibernatePersistenceProvider] will be used instead.",
				triggerable.triggerMessage()
		);
	}
}

