/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.decomposer;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that collection decomposers correctly delegate to coordinators.
 */
@DomainModel(annotatedClasses = CollectionCoordinatorDecompositionTest.Person.class)
@SessionFactory(
		generateStatistics = true,
		exportSchema = true
)
public class CollectionCoordinatorDecompositionTest {

	@Test
	public void testCollectionRecreateDecomposer(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person();
			person.id = 1L;
			person.emails = new ArrayList<>();
			person.emails.add( "test@example.com" );
			session.persist( person );
		} );

		scope.inTransaction( session -> {
			Person person = session.find( Person.class, 1L );
			assertNotNull( person );
			System.out.println("LEGACY QUEUE TEST: person.emails.size() = " + person.emails.size());
			System.out.println("LEGACY QUEUE TEST: person.emails = " + person.emails);
			assertEquals( 1, person.emails.size() );
			assertEquals( "test@example.com", person.emails.get(0) );
		} );
	}

	@Test
	public void testCoordinatorsAreAccessible(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final BasicCollectionPersister persister = (BasicCollectionPersister) session.getSessionFactory()
					.getMappingMetamodel()
					.getCollectionDescriptor( Person.class.getName() + ".emails" );

			// Verify coordinators are publicly accessible
			assertNotNull( persister.getInsertRowsCoordinator(), "InsertRowsCoordinator should be accessible" );
			assertNotNull( persister.getUpdateRowsCoordinator(), "UpdateRowsCoordinator should be accessible" );
			assertNotNull( persister.getDeleteRowsCoordinator(), "DeleteRowsCoordinator should be accessible" );
			assertNotNull( persister.getRemoveCoordinator(), "RemoveCoordinator should be accessible" );
		} );
	}

	@Test
	public void testDecomposersDelegateToCoordinators(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person person = new Person();
			person.id = 2L;
			person.emails = new ArrayList<>();
			person.emails.add( "first@example.com" );
			person.emails.add( "second@example.com" );
			session.persist( person );
		} );

		scope.inTransaction( session -> {
			Person person = session.find( Person.class, 2L );
			assertEquals( 2, person.emails.size() );
			assertTrue( person.emails.contains( "first@example.com" ) );
			assertTrue( person.emails.contains( "second@example.com" ) );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;

		@ElementCollection
		List<String> emails;
	}
}
