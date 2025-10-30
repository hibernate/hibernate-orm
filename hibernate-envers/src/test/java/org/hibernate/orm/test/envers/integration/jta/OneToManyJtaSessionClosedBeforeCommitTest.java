/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.jta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11570")
@EnversTest
@Jpa(
		annotatedClasses = {SetRefIngEntity.class, SetRefEdEntity.class},
		integrationSettings = @Setting(name = AvailableSettings.ALLOW_JTA_TRANSACTION_ACCESS, value = "true"),
		settingConfigurations = @SettingConfiguration(configurer = TestingJtaBootstrap.class)
)
public class OneToManyJtaSessionClosedBeforeCommitTest {
	private static final Integer ENTITY_ID = 1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) throws Exception {
		TestingJtaPlatformImpl.INSTANCE.getTransactionManager().begin();
		var entityManager = scope.getEntityManagerFactory().createEntityManager();
		try {
			SetRefEdEntity edEntity = new SetRefEdEntity( 2, "edEntity" );
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
			entityManager.close();
			TestingJtaPlatformImpl.tryCommit();
		}
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> assertEquals(
				Arrays.asList( 1 ),
				AuditReaderFactory.get( entityManager ).getRevisions( SetRefIngEntity.class, ENTITY_ID )
		) );
	}

	@Test
	public void testRevisionHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> assertEquals(
				new SetRefIngEntity( ENTITY_ID, "ingEntity" ),
				AuditReaderFactory.get( entityManager ).find( SetRefIngEntity.class, ENTITY_ID, 1 )
		) );
	}
}
