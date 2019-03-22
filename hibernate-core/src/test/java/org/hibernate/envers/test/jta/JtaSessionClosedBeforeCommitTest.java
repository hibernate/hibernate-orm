/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.jta;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Simple test that checks that Envers can still perform its beforeTransactionCompletion
 * callbacks successfully even if the Hibernate Session/EntityManager has been closed
 * prior to the JTA transaction commit.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11232")
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class JtaSessionClosedBeforeCommitTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer entityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IntTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
		settings.put( AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() throws Exception {
		inJtaTransaction(
				entityManager -> {
					IntTestEntity ite = new IntTestEntity( 10 );
					entityManager.persist( ite );
					entityId = ite.getId();
					// simulates spring JtaTransactionManager.triggerBeforeCompletion()
					// this closes the entity manager prior to the JTA transaction.
					entityManager.close();
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( IntTestEntity.class, entityId ), contains( 1 ) );
	}

	@DynamicTest
	public void testRevisionHistory() {
		final IntTestEntity rev1 = new IntTestEntity( 10, entityId );
		assertThat( getAuditReader().find( IntTestEntity.class, entityId, 1 ), equalTo( rev1 ) );
	}
}
