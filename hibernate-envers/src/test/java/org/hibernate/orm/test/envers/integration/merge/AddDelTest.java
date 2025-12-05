/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.merge;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6753")
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class, GivenIdStrEntity.class})
public class AddDelTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inTransaction( em -> {
			GivenIdStrEntity entity = new GivenIdStrEntity( 1, "data" );
			em.persist( entity );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			em.persist( new StrTestEntity( "another data" ) ); // Just to create second revision.
			GivenIdStrEntity entity = em.find( GivenIdStrEntity.class, 1 );
			em.remove( entity ); // First try to remove the entity.
			em.persist( entity ); // Then save it.
		} );

		// Revision 3
		scope.inTransaction( em -> {
			GivenIdStrEntity entity = em.find( GivenIdStrEntity.class, 1 );
			em.remove( entity ); // First try to remove the entity.
			entity.setData( "modified data" ); // Then change it's state.
			em.persist( entity ); // Finally save it.
		} );
	}

	@Test
	public void testRevisionsCountOfGivenIdStrEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 2 has not changed entity's state.
			assertEquals( Arrays.asList( 1, 3 ), AuditReaderFactory.get( em ).getRevisions( GivenIdStrEntity.class, 1 ) );
		} );
	}

	@Test
	public void testHistoryOfGivenIdStrEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( new GivenIdStrEntity( 1, "data" ), auditReader.find( GivenIdStrEntity.class, 1, 1 ) );
			assertEquals(
					new GivenIdStrEntity( 1, "modified data" ),
					auditReader.find( GivenIdStrEntity.class, 1, 3 )
			);
		} );
	}
}
