/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
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
@JiraKey(value = "HHH-6613")
@EnversTest
@Jpa(annotatedClasses = {DarkCharacter.class})
public class BasicEmbeddableCollection {
	private int id = -1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - empty element collection
		scope.inTransaction( em -> {
			DarkCharacter darkCharacter = new DarkCharacter( 1, 1 );
			em.persist( darkCharacter );
			id = darkCharacter.getId();
		} );

		// Revision 2 - adding collection element
		scope.inTransaction( em -> {
			DarkCharacter darkCharacter = em.find( DarkCharacter.class, id );
			darkCharacter.getNames().add( new Name( "Action", "Hank" ) );
			em.merge( darkCharacter );
		} );

		// Revision 3 - adding another collection element
		scope.inTransaction( em -> {
			DarkCharacter darkCharacter = em.find( DarkCharacter.class, id );
			darkCharacter.getNames().add( new Name( "Green", "Lantern" ) );
			em.merge( darkCharacter );
		} );

		// Revision 4 - removing single collection element
		scope.inTransaction( em -> {
			DarkCharacter darkCharacter = em.find( DarkCharacter.class, id );
			darkCharacter.getNames().remove( new Name( "Action", "Hank" ) );
			em.merge( darkCharacter );
		} );

		// Revision 5 - removing all collection elements
		scope.inTransaction( em -> {
			DarkCharacter darkCharacter = em.find( DarkCharacter.class, id );
			darkCharacter.getNames().clear();
			em.merge( darkCharacter );
		} );
	}

	@Test
	public void testRevisionsCount(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4, 5 ), auditReader.getRevisions( DarkCharacter.class, id ) );
		} );
	}

	@Test
	public void testHistoryOfCharacter(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			DarkCharacter darkCharacter = new DarkCharacter( id, 1 );

			DarkCharacter ver1 = auditReader.find( DarkCharacter.class, id, 1 );

			assertEquals( darkCharacter, ver1 );
			assertEquals( 0, ver1.getNames().size() );

			darkCharacter.getNames().add( new Name( "Action", "Hank" ) );
			DarkCharacter ver2 = auditReader.find( DarkCharacter.class, id, 2 );

			assertEquals( darkCharacter, ver2 );
			assertEquals( darkCharacter.getNames(), ver2.getNames() );

			darkCharacter.getNames().add( new Name( "Green", "Lantern" ) );
			DarkCharacter ver3 = auditReader.find( DarkCharacter.class, id, 3 );

			assertEquals( darkCharacter, ver3 );
			assertEquals( darkCharacter.getNames(), ver3.getNames() );

			darkCharacter.getNames().remove( new Name( "Action", "Hank" ) );
			DarkCharacter ver4 = auditReader.find( DarkCharacter.class, id, 4 );

			assertEquals( darkCharacter, ver4 );
			assertEquals( darkCharacter.getNames(), ver4.getNames() );

			darkCharacter.getNames().clear();
			DarkCharacter ver5 = auditReader.find( DarkCharacter.class, id, 5 );

			assertEquals( darkCharacter, ver5 );
			assertEquals( darkCharacter.getNames(), ver5.getNames() );
		} );
	}
}
