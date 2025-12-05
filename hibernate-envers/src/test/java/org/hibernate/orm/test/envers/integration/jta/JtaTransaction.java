/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.IntTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingConfiguration;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Same as {@link org.hibernate.orm.test.envers.integration.basic.Simple}, but in a JTA environment.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(
		annotatedClasses = {IntTestEntity.class},
		integrationSettings = @Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true"),
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
public class JtaTransaction {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws Exception {
		final var emf = scope.getEntityManagerFactory();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		var entityManager = emf.createEntityManager();
		IntTestEntity ite;
		try {
			ite = new IntTestEntity( 10 );
			entityManager.persist( ite );
			id1 = ite.getId();
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
			entityManager.close();
		}

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();

		entityManager = emf.createEntityManager();
		try {
			ite = entityManager.find( IntTestEntity.class, id1 );
			ite.setNumber( 20 );
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
			entityManager.close();
		}
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> assertEquals(
				2,
				AuditReaderFactory.get( entityManager ).getRevisions( IntTestEntity.class, id1 ).size()
		) );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		IntTestEntity ver1 = new IntTestEntity( 10, id1 );
		IntTestEntity ver2 = new IntTestEntity( 20, id1 );

		scope.inEntityManager( entityManager -> {
			List<Number> revisions = AuditReaderFactory.get( entityManager ).getRevisions( IntTestEntity.class, id1 );

			assertEquals(
					ver1,
					AuditReaderFactory.get( entityManager ).find( IntTestEntity.class, id1, revisions.get( 0 ) )
			);
			assertEquals(
					ver2,
					AuditReaderFactory.get( entityManager ).find( IntTestEntity.class, id1, revisions.get( 1 ) )
			);
		} );
	}
}
