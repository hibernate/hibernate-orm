/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.IntNoAutoIdTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that reusing identifiers doesn't cause auditing misbehavior.
 *
 * @author adar
 */
@JiraKey(value = "HHH-8280")
@EnversTest
@Jpa(annotatedClasses = {IntNoAutoIdTestEntity.class},
		integrationSettings = @Setting(name = EnversSettings.ALLOW_IDENTIFIER_REUSE, value = "true"))
public class IdentifierReuseTest {

	@Test
	public void testIdentifierReuse(EntityManagerFactoryScope scope) {
		final Integer reusedId = 1;

		saveUpdateAndRemoveEntity( scope, reusedId );
		saveUpdateAndRemoveEntity( scope, reusedId );

		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 1, 2, 3, 4, 5, 6 ),
					AuditReaderFactory.get( em ).getRevisions( IntNoAutoIdTestEntity.class, reusedId )
			);
		} );
	}

	private void saveUpdateAndRemoveEntity(EntityManagerFactoryScope scope, Integer id) {
		scope.inTransaction( em -> {
			IntNoAutoIdTestEntity entity = new IntNoAutoIdTestEntity( 0, id );
			em.persist( entity );
			assertEquals( id, entity.getId() );
		} );

		scope.inTransaction( em -> {
			IntNoAutoIdTestEntity entity = em.find( IntNoAutoIdTestEntity.class, id );
			entity.setNumVal( 1 );
			entity = em.merge( entity );
			assertEquals( id, entity.getId() );
		} );

		scope.inTransaction( em -> {
			IntNoAutoIdTestEntity entity = em.find( IntNoAutoIdTestEntity.class, id );
			assertNotNull( entity );
			em.remove( entity );
		} );
	}
}
