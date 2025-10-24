/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.auditReader;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.IntNoAutoIdTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A test which verifies the behavior of the various {@link AuditReader} find implementations when the
 * configuration option {@link EnversSettings#FIND_BY_REVISION_EXACT_MATCH} is enabled.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-13500")
@EnversTest
@Jpa(annotatedClasses = {IntNoAutoIdTestEntity.class},
		integrationSettings = @Setting(name = EnversSettings.FIND_BY_REVISION_EXACT_MATCH, value = "true"))
public class FindByRevisionExactMatchOptionTest {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Insert entity with id=1, numVal=1, revision 1
		scope.inTransaction( em -> {
			final IntNoAutoIdTestEntity entity1 = new IntNoAutoIdTestEntity( 1, 1 );
			em.persist( entity1 );
		} );

		// Update entity with id=1, setting numVal=11, revision 2
		scope.inTransaction( em -> {
			final IntNoAutoIdTestEntity entity = em.find( IntNoAutoIdTestEntity.class, 1 );
			entity.setNumVal( 11 );
			em.merge( entity );
		} );

		// Insert entity with id=2, numVal=2, revision 3
		scope.inTransaction( em -> {
			final IntNoAutoIdTestEntity entity2 = new IntNoAutoIdTestEntity( 2, 2 );
			em.persist( entity2 );
		} );

		// Update entity with id=2, setting numVal=22, revision 4
		scope.inTransaction( em -> {
			final IntNoAutoIdTestEntity entity = em.find( IntNoAutoIdTestEntity.class, 2 );
			entity.setNumVal( 22 );
			em.merge( entity );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ),
					auditReader.getRevisions( IntNoAutoIdTestEntity.class, 1 ) );
			assertEquals( Arrays.asList( 3, 4 ),
					auditReader.getRevisions( IntNoAutoIdTestEntity.class, 2 ) );
		} );
	}

	@Test
	public void testFindEntityId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( new IntNoAutoIdTestEntity( 1, 1 ), auditReader.find( IntNoAutoIdTestEntity.class, 1, 1 ) );
			assertEquals( new IntNoAutoIdTestEntity( 11, 1 ), auditReader.find( IntNoAutoIdTestEntity.class, 1, 2 ) );
			assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 1, 3 ) );
			assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 1, 4 ) );
		} );
	}

	@Test
	public void testFindEntityId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 2, 1 ) );
			assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 2, 2 ) );
			assertEquals( new IntNoAutoIdTestEntity( 2, 2 ), auditReader.find( IntNoAutoIdTestEntity.class, 2, 3 ) );
			assertEquals( new IntNoAutoIdTestEntity( 22, 2 ), auditReader.find( IntNoAutoIdTestEntity.class, 2, 4 ) );
		} );
	}
}
