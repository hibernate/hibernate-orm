/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.LockMode;
import org.hibernate.ObjectDeletedException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SessionFactory
@DomainModel(annotatedClasses = LockModeAcrossTransactionsTest.Cached.class)
class LockModeAcrossTransactionsTest {

	@Test void testWithEvict(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Cached(5L) );
		} );
		scope.getSessionFactory().getCache().evict(Cached.class);
		scope.inSession( session -> {
			Cached cached = session.find( Cached.class, 5L );
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
		} );
		scope.getSessionFactory().getCache().evict(Cached.class);
		scope.inTransaction( session -> {
			Cached cached = session.find( Cached.class, 5L );
			assertEquals( LockMode.READ, session.getCurrentLockMode( cached ) );
		} );
		scope.getSessionFactory().getCache().evict(Cached.class);
		scope.inSession( session -> {
			Cached cached = session.createQuery( "from Cached", Cached.class ).getSingleResult();
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
		} );
		scope.getSessionFactory().getCache().evict(Cached.class);
		scope.inTransaction( session -> {
			Cached cached = session.createQuery( "from Cached", Cached.class ).getSingleResult();
			assertEquals( LockMode.READ, session.getCurrentLockMode( cached ) );
		} );
		scope.getSessionFactory().getCache().evict(Cached.class);
		scope.inSession( session -> {
			Cached cached = session.fromTransaction( tx -> {
				Cached c = session.find( Cached.class, 5L );
				assertEquals( LockMode.READ, session.getCurrentLockMode( c ) );
				return c;
			} );
			session.inTransaction( tx -> {
				assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
			} );
		} );
		scope.inSession( session -> {
			Cached cached = session.find( Cached.class, 5L );
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
		} );
		scope.inTransaction( session -> {
			Cached cached = session.find( Cached.class, 5L );
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
		} );
	}

	@Test void testWithoutEvict(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Cached cached = new Cached( 3L );
			session.persist( cached );
			assertEquals( LockMode.WRITE, session.getCurrentLockMode( cached ) );
		} );
		scope.inSession( session -> {
			Cached cached = session.find( Cached.class, 3L );
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
		} );
		scope.inTransaction( session -> {
			Cached cached = session.find( Cached.class, 3L );
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
			cached.name = "Gavin";
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
			session.flush();
			assertEquals( LockMode.WRITE, session.getCurrentLockMode( cached ) );
		} );
		scope.inTransaction( session -> {
			Cached cached = session.find( Cached.class, 3L );
			assertEquals( LockMode.NONE, session.getCurrentLockMode( cached ) );
			session.remove( cached );
			assertThrows( ObjectDeletedException.class,
					() -> session.getCurrentLockMode( cached ) );
		} );
	}

	@Cacheable @Entity(name = "Cached")
	static class Cached {
		@Id
		Long id;
		String name;
		Cached(Long id) {
			this.id = id;
		}
		Cached() {
		}
	}
}
