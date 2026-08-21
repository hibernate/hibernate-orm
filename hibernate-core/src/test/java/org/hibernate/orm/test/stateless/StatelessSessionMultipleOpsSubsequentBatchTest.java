/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for verifying that subsequent calls to *Multiple operations
 * execute their batches immediately, not deferred until transaction commit.
 */
@DomainModel(annotatedClasses = {
		StatelessSessionMultipleOpsSubsequentBatchTest.Person.class
})
@SessionFactory(useCollectingStatementInspector = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-20065")
public class StatelessSessionMultipleOpsSubsequentBatchTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInsertMultipleSubsequentBatch(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		// First batch: 5 entities
		final var firstBatch = new ArrayList<Person>();
		for ( int i = 0; i < 5; i++ ) {
			firstBatch.add( new Person( i, "first_" + i ) );
		}

		// Second batch: 3 entities (different count)
		final var secondBatch = new ArrayList<Person>();
		for ( int i = 5; i < 8; i++ ) {
			secondBatch.add( new Person( i, "second_" + i ) );
		}

		scope.inStatelessTransaction( session -> {
			session.insertMultiple( firstBatch );
			assertThat( inspector.getSqlQueries() )
					.as( "First batch should be executed immediately after insertMultiple call in a single query" )
					.hasSize( 1 );
			// Verify first batch entities were inserted immediately
			for ( int i = 0; i < 5; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should exist after first batch", i ).isNotNull();
				assertThat( p.name ).isEqualTo( "first_" + i );
			}
			inspector.clear();

			session.insertMultiple( secondBatch );
			assertThat( inspector.getSqlQueries() )
					.as( "Second batch should be executed immediately, not deferred" )
					.hasSize( 1 );
			// Verify second batch entities were inserted immediately
			for ( int i = 5; i < 8; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should exist after second batch", i ).isNotNull();
				assertThat( p.name ).isEqualTo( "second_" + i );
			}
		} );
	}

	@Test
	public void testUpdateMultipleSubsequentBatch(SessionFactoryScope scope) {
		// Initialize 8 entities
		final var people = initPeople( 8, scope );

		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		// First batch: update 5 entities
		final var firstBatch = people.subList( 0, 5 );
		for ( final var p : firstBatch ) {
			p.name = "first_" + p.id;
		}

		// Second batch: update 3 entities (different count)
		final var secondBatch = people.subList( 5, 8 );
		for ( final var p : secondBatch ) {
			p.name = "second_" + p.id;
		}

		scope.inStatelessTransaction( session -> {
			session.updateMultiple( firstBatch );
			assertThat( inspector.getSqlQueries() )
					.as( "First batch should be executed immediately after updateMultiple call in a single query" )
					.hasSize( 1 );
			// Verify first batch entities were updated immediately
			for ( int i = 0; i < 5; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should exist", i ).isNotNull();
				assertThat( p.name ).isEqualTo( "first_" + i );
			}
			// Verify second batch entities still have original names
			for ( int i = 5; i < 8; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should exist", i ).isNotNull();
				assertThat( p.name ).isEqualTo( "person_ " + i );
			}
			inspector.clear();

			session.updateMultiple( secondBatch );
			assertThat( inspector.getSqlQueries() )
					.as( "Second batch should be executed immediately, not deferred" )
					.hasSize( 1 );
			// Verify second batch entities were updated immediately
			for ( int i = 5; i < 8; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should exist", i ).isNotNull();
				assertThat( p.name ).isEqualTo( "second_" + i );
			}
		} );
	}

	@Test
	public void testDeleteMultipleSubsequentBatch(SessionFactoryScope scope) {
		// Initialize 8 entities
		final var people = initPeople( 8, scope );

		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		// First batch: delete 5 entities
		final var firstBatch = people.subList( 0, 5 );

		// Second batch: delete 3 entities (different count)
		final var secondBatch = people.subList( 5, 8 );

		scope.inStatelessTransaction( session -> {
			session.deleteMultiple( firstBatch );
			assertThat( inspector.getSqlQueries() )
					.as( "First batch should be executed immediately after deleteMultiple call in a single query" )
					.hasSize( 1 );
			// Verify first batch entities were deleted immediately
			for ( int i = 0; i < 5; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should be deleted after first batch", i ).isNull();
			}
			// Verify second batch entities still exist
			for ( int i = 5; i < 8; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should still exist before second batch", i ).isNotNull();
			}
			inspector.clear();

			session.deleteMultiple( secondBatch );
			assertThat( inspector.getSqlQueries() )
					.as( "Second batch should be executed immediately, not deferred" )
					.hasSize( 1 );
			// Verify second batch entities were deleted immediately
			for ( int i = 5; i < 8; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should be deleted after second batch", i ).isNull();
			}
		} );
	}

	@Test
	public void testUpsertMultipleSubsequentBatch(SessionFactoryScope scope) {
		// Initialize 5 entities
		initPeople( 5, scope );

		// First batch: 5 entities (mix of update and insert)
		final var firstBatch = new ArrayList<Person>();
		for ( int i = 3; i < 8; i++ ) {
			firstBatch.add( new Person( i, "first_" + i ) );
		}

		// Second batch: 3 entities (different count, mix of update and insert)
		final var secondBatch = new ArrayList<Person>();
		for ( int i = 6; i < 9; i++ ) {
			secondBatch.add( new Person( i, "second_" + i ) );
		}

		scope.inStatelessTransaction( session -> {
			session.upsertMultiple( firstBatch );
			// Verify state after first batch: ids 0-2 original, 3-7 from first batch
			for ( int i = 0; i < 8; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should exist after first batch", i ).isNotNull();
				if ( i < 3 ) {
					assertThat( p.name ).isEqualTo( "person_ " + i );
				}
				else {
					assertThat( p.name ).isEqualTo( "first_" + i );
				}
			}
			// Verify entity 8 doesn't exist yet
			assertThat( session.get( Person.class, 8 ) )
					.as( "Entity 8 should not exist before second batch" )
					.isNull();

			session.upsertMultiple( secondBatch );
			// Verify state after second batch: ids 0-2 original, 3-5 from first batch, 6-8 from second batch
			for ( int i = 0; i < 9; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).as( "Entity %d should exist after second batch", i ).isNotNull();
				if ( i < 3 ) {
					assertThat( p.name ).isEqualTo( "person_ " + i );
				}
				else if ( i < 6 ) {
					assertThat( p.name ).isEqualTo( "first_" + i );
				}
				else {
					assertThat( p.name ).isEqualTo( "second_" + i );
				}
			}
		} );
	}

	private static List<Person> initPeople(int count, SessionFactoryScope scope) {
		final var people = new ArrayList<Person>();
		for ( int i = 0; i < count; i++ ) {
			final var p = new Person( i, "person_ " + i );
			people.add( p );
		}
		scope.inStatelessTransaction( session -> session.insertMultiple( people ) );
		return people;
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		public Integer id;
		public String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
