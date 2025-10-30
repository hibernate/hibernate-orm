/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.util.Arrays;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.IntTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Simple test that checks that Envers can still perform its beforeTransactionCompletion
 * callbacks successfully even if the Hibernate Session/EntityManager has been closed
 * prior to the JTA transaction commit.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11232")
@EnversTest
@Jpa(
		annotatedClasses = {IntTestEntity.class},
		integrationSettings = { @Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true") },
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
public class JtaSessionClosedBeforeCommitTest {
	private Integer entityId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws Exception {
		// We need to do this to obtain the SessionFactoryImplementor
		final var emf = scope.getEntityManagerFactory();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		var entityManager = emf.createEntityManager();
		try {
			IntTestEntity ite = new IntTestEntity( 10 );
			entityManager.persist( ite );
			entityId = ite.getId();
			// simulates spring JtaTransactionManager.triggerBeforeCompletion()
			// this closes the entity manager prior to the JTA transaction.
			entityManager.close();
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
		}
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> assertEquals(
				Arrays.asList( 1 ),
				AuditReaderFactory.get( entityManager ).getRevisions( IntTestEntity.class, entityId )
		) );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> assertEquals(
				new IntTestEntity( 10, entityId ),
				AuditReaderFactory.get( entityManager ).find( IntTestEntity.class, entityId, 1 )
		) );
	}
}
