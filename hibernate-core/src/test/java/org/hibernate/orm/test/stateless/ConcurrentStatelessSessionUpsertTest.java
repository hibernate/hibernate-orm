/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
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
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
@Jira("https://hibernate.atlassian.net/browse/HHH-20894")
public class ConcurrentStatelessSessionUpsertTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpsertIndividualAgainstLongRunningLockConflictFirst(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, InsertionPosition.BEGINNING, false );
	}

	@Test
	public void testUpsertIndividualAgainstLongRunningLockConflictMiddle(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, InsertionPosition.MIDDLE, false );
	}

	@Test
	public void testUpsertIndividualAgainstLongRunningLockConflictLast(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, InsertionPosition.END, false );
	}

	@Test
	public void testUpsertMultipleAgainstLongRunningLockConflictFirst(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, InsertionPosition.BEGINNING, true );
	}

	@Test
	public void testUpsertMultipleAgainstLongRunningLockConflictMiddle(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, InsertionPosition.MIDDLE, true );
	}

	@Test
	public void testUpsertMultipleAgainstLongRunningLockConflictLast(SessionFactoryScope scope) {
		testUpsertAgainstLongRunningLockConflictLast( scope, InsertionPosition.END, true );
	}

	public void testUpsertAgainstLongRunningLockConflictLast(SessionFactoryScope scope, InsertionPosition position, boolean multiple) {
		final var futureHolder = new MutableObject<Future<?>>();
		scope.inStatelessTransaction( session -> {
			// Insert the single person that would produce a constraint violation in the TX
			// that spans across the other transactions
			session.insert(new Person(100, "person"));
			futureHolder.set( multiple
					? testConcurrentUpsertMultiple( scope, position )
					: testConcurrentUpsertIndividual( scope, position )
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

	private Future<?> testConcurrentUpsertMultiple(SessionFactoryScope scope, InsertionPosition position) {
		final var executor = Executors.newFixedThreadPool( 1 );
		try {
			final List<Person> people = buildPeople( 5, position );
			return executor.submit(() -> scope.inStatelessTransaction( statelessSession -> statelessSession.upsertMultiple( people ) ) );
		}
		finally {
			executor.shutdown();
		}
	}

	private Future<?> testConcurrentUpsertIndividual(SessionFactoryScope scope, InsertionPosition position) {
		final var executor = Executors.newFixedThreadPool( 1 );
		try {
			final List<Person> people = buildPeople( 5, position );
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

	private static List<Person> buildPeople(int count, InsertionPosition position) {
		final var people = new ArrayList<Person>();
		for ( int i = 0; i < count; i++ ) {
			final var p = new Person( i, "person_ " + i );
			people.add( p );
		}
		people.add(
				switch ( position ) {
					case BEGINNING -> 0;
					case MIDDLE -> (int) Math.ceil( count / 2D );
					case END -> people.size();
				},
				new Person( 100, "person_100" )
		);
		return people;
	}

	enum InsertionPosition {
		BEGINNING,
		MIDDLE,
		END
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
