/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.SetRefIngEntity;

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
 * @author VladoKuruc
 */
@JiraKey(value = "HHH-14061")
@EnversTest
@Jpa(
		annotatedClasses = { SetRefIngEntity.class, SetRefEdEntity.class },
		integrationSettings = @Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true"),
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
public class OneToManyLazyJtaSessionClosedBeforeCommitTest {
	private static final Integer PARENT_ID = 2;
	private static final Integer ENTITY_ID = 1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws Exception {
		final var emf = scope.getEntityManagerFactory();

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		var entityManager = emf.createEntityManager();
		try {
			SetRefIngEntity refIngEntity = new SetRefIngEntity( 3, "ingEntityRef" );
			entityManager.persist( refIngEntity );

			SetRefEdEntity edEntity = new SetRefEdEntity( PARENT_ID, "edEntity" );
			edEntity.setRef( refIngEntity );
			entityManager.persist( edEntity );

			SetRefIngEntity ingEntity = new SetRefIngEntity( ENTITY_ID, "ingEntity" );

			Set<SetRefIngEntity> sries = new HashSet<>();
			sries.add( ingEntity );
			ingEntity.setReference( edEntity );
			edEntity.setReffering( sries );

			entityManager.persist( ingEntity );
			entityManager.flush();
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
			entityManager.close();
		}

		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		entityManager = emf.createEntityManager();
		try {
			entityManager.unwrap( Session.class ).setHibernateFlushMode( FlushMode.MANUAL );
			SetRefEdEntity edEntity = entityManager.find( SetRefEdEntity.class, PARENT_ID );
			Set<SetRefIngEntity> reffering = edEntity.getReffering();
			SetRefIngEntity ingEntity = reffering.iterator().next();
			ingEntity.setReference( null );
			reffering.remove( ingEntity );
			entityManager.merge( ingEntity );
			entityManager.flush();
			// clear context in transaction
			entityManager.clear();
			entityManager.merge( edEntity );
			entityManager.flush();
		}
		finally {
			TestingJtaPlatformImpl.tryCommit();
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			assertEquals(
					Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( entityManager ).getRevisions( SetRefIngEntity.class, ENTITY_ID )
			);
			assertEquals(
					Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( entityManager ).getRevisions( SetRefEdEntity.class, PARENT_ID )
			);
		} );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			assertEquals(
					Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( entityManager ).getRevisions( SetRefIngEntity.class, ENTITY_ID )
			);
			assertEquals(
					Arrays.asList( 1, 2 ),
					AuditReaderFactory.get( entityManager ).getRevisions( SetRefEdEntity.class, PARENT_ID )
			);
		} );
	}
}
