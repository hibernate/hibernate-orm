/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DomainModel(annotatedClasses = {
		ConcurrentStatelessSessionUpsertTest.Person.class
})
@SessionFactory
public class ConcurrentStatelessSessionUpsertTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpsertIndividualAgainstLongRunningLockConflictFirst(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, true, false );
	}

	@Test
	public void testUpsertIndividualAgainstLongRunningLockConflictLast(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, false, false );
	}

	@Test
	public void testUpsertMultipleAgainstLongRunningLockConflictFirst(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, true, true );
	}

	@Test
	public void testUpsertMultipleAgainstLongRunningLockConflictLast(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, false, true );
	}

	public void testUpsertAgainstLongRunningLockConflictLast(SessionFactoryScope scope, boolean first, boolean multiple) {
		final var futureHolder = new MutableObject<Future<?>>();
		scope.inStatelessTransaction( session -> {
			// Insert the single person that would produce a constraint violation in the TX
			// that spans across the other transactions
			session.insert(new Person(100, "person"));
			futureHolder.set( multiple
					? testConcurrentUpsertMultiple( scope, first )
					: testConcurrentUpsertIndividual( scope, first )
			);
			waitUntilMergeIsBlocked();
		} );
		awaitFuture( futureHolder.get() );
		scope.inStatelessSession( session -> {
			Person person = session.find( Person.class, 100 );
			assertNotNull( person );
			assertEquals( "person_100", person.name );
			assertEquals( 6L, session.createSelectionQuery( "from Person", Person.class ).getResultCount() );
		} );
	}

	private void waitUntilMergeIsBlocked() {
		// Wait two seconds in the main thread to give the concurrent thread enough time to be blocked on the merge
		try {
			Thread.sleep( 2_000L );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	private Future<?> testConcurrentUpsertMultiple(SessionFactoryScope scope, boolean first) {
		final var executor = Executors.newFixedThreadPool( 1 );
		try {
			final List<Person> people = buildPeople( 5, first );
			return executor.submit(() -> scope.inStatelessTransaction( statelessSession -> statelessSession.upsertMultiple( people ) ) );
		}
		finally {
			executor.shutdown();
		}
	}

	private Future<?> testConcurrentUpsertIndividual(SessionFactoryScope scope, boolean first) {
		final var executor = Executors.newFixedThreadPool( 1 );
		try {
			final List<Person> people = buildPeople( 5, first );
			return executor.submit(() -> scope.inStatelessTransaction( statelessSession -> {
				for ( Person person : people ) {
					statelessSession.upsert( person );
				}
			} ) );
		}
		finally {
			executor.shutdown();
		}
	}

	private void awaitFuture(Future<?> future) {
		try {
			future.get();
		}
		catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException( e );
		}
	}

	private static List<Person> buildPeople(int count, boolean first) {
		final var people = new ArrayList<Person>();
		for ( int i = 0; i < count; i++ ) {
			final var p = new Person( i, "person_ " + i );
			people.add( p );
		}
		people.add(first ? 0 : people.size(), new Person(100, "person_100"));
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
