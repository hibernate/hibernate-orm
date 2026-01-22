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

@DomainModel(annotatedClasses = {
		StatelessSessionMultipleOpsTest.Person.class
})
@SessionFactory(useCollectingStatementInspector = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-20065")
public class StatelessSessionMultipleOpsTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInsertMultiple(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		final var count = 5;
		final var people = new ArrayList<Person>();
		for ( int i = 0; i < count; i++ ) {
			people.add( new Person( i, "person_ " + i ) );
		}

		scope.inStatelessTransaction( session -> {
			session.insertMultiple( people );

			assertThat( inspector.getSqlQueries() )
					.as( "Should have batched insert into single statement preparation" )
					.hasSize( 1 );

			for ( int i = 0; i < count; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).isNotNull().extracting( "name" ).isEqualTo( "person_ " + i );
			}
		} );
	}

	@Test
	public void testUpdateMultiple(SessionFactoryScope scope) {
		final var count = 5;
		final var people = initPeople( count, scope );

		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		for ( final var p : people ) {
			p.name = "Updated " + p.id;
		}

		scope.inStatelessTransaction( session -> {
			session.updateMultiple( people );

			assertThat( inspector.getSqlQueries() )
					.as( "Should have batched update into single statement preparation" )
					.hasSize( 1 );

			for ( int i = 0; i < count; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).isNotNull().extracting( "name" ).isEqualTo( "Updated " + i );
			}
		} );
	}

	@Test
	public void testDeleteMultiple(SessionFactoryScope scope) {
		final var count = 5;
		final var people = initPeople( count, scope );

		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inStatelessTransaction( session -> {
			session.deleteMultiple( people );

			assertThat( inspector.getSqlQueries() )
					.as( "Should have batched delete into single statement preparation" )
					.hasSize( 1 );

			for ( int i = 0; i < count; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).isNull();
			}
		} );
	}

	@Test
	public void testUpsertMultiple(SessionFactoryScope scope) {
		final var initialCount = 5;
		final var people = initPeople( initialCount, scope );

		final var finalCount = 8;
		{
			// Update existing
			var i = 0;
			for ( final var p : people ) {
				p.name = "updated_" + i++;
			}

			// Add new
			for ( ; i < finalCount; i++ ) {
				people.add( new Person( i, "new_" + i ) );
			}
		}

		scope.inStatelessTransaction( session -> {
			session.upsertMultiple( people );

			for ( int i = 0; i < finalCount; i++ ) {
				final var p = session.get( Person.class, i );
				assertThat( p ).isNotNull().extracting( "name" )
						.isEqualTo( i < initialCount ? "updated_" + i : "new_" + i );
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
