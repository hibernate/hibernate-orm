/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.semantics;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.hibernate.graph.RootGraph;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests the semantics of the loading based on {@linkplain EntityGraph} using get/find methods.
 *
 * @see jakarta.persistence.EntityHandler#find(EntityGraph, Object, jakarta.persistence.FindOption...)
 * @see jakarta.persistence.EntityHandler#get(EntityGraph, Object, jakarta.persistence.FindOption...)
 *
 * @author Steve Ebersole
 */
public class EntityGraphLoadSemanticTests extends Base {
	@Test
	void testTransactionalLoadingStateful(SessionFactoryScope factoryScope) {
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inTransaction( (session) -> {
			final Book book = session.find( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );

		factoryScope.inTransaction( (session) -> {
			final Book book = session.get( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );
	}

	@Test
	void testTransactionalLoadingStateless(SessionFactoryScope factoryScope) {
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inStatelessTransaction( (session) -> {
			final Book book = session.find( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );

		factoryScope.inStatelessTransaction( (session) -> {
			final Book book = session.get( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );
	}

	@Test
	void testNonTransactionalLoadingStateful(SessionFactoryScope factoryScope) {
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inSession( (session) -> {
			final Book book = session.find( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );

		factoryScope.inSession( (session) -> {
			final Book book = session.get( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );
	}

	@Test
	void testNonTransactionalLoadingStateless(SessionFactoryScope factoryScope) {
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inStatelessSession( (session) -> {
			final Book book = session.find( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );

		factoryScope.inStatelessSession( (session) -> {
			final Book book = session.get( graph, 1 );
			assertThat( book ).isNotNull();
			assertThat( Hibernate.isInitialized( book.getAuthors() ) ).isTrue();
			assertThat( Hibernate.isInitialized( book.getEditors() ) ).isFalse();
		} );
	}

	@Test
	void testNoMatchStateful(SessionFactoryScope factoryScope) {
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inTransaction( (session) -> {
			final Book book = session.find( graph, 5, LockModeType.PESSIMISTIC_READ );
			assertThat( book ).isNull();
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( graph, 5, LockModeType.PESSIMISTIC_READ );
				fail( "Expecting failure" );
			}
			catch (EntityNotFoundException expected) {}
		} );
	}

	@Test
	void testNoMatchStateless(SessionFactoryScope factoryScope) {
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inStatelessTransaction( (session) -> {
			final Book book = session.find( graph, 5, LockModeType.PESSIMISTIC_READ );
			assertThat( book ).isNull();
		} );
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.get( graph, 5, LockModeType.PESSIMISTIC_READ );
				fail( "Expecting failure" );
			}
			catch (EntityNotFoundException expected) {}
		} );
	}

	@Test
	void testBadKeyTypeStateful(SessionFactoryScope factoryScope) {
		var uuid = UUID.randomUUID();
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inTransaction( (session) -> {
			try {
				session.find( graph, uuid, LockModeType.PESSIMISTIC_READ );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( graph, uuid, LockModeType.PESSIMISTIC_READ );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}

	@Test
	void testBadKeyTypeStateless(SessionFactoryScope factoryScope) {
		var uuid = UUID.randomUUID();
		final RootGraph<Book> graph = factoryScope.getSessionFactory().parseEntityGraph( Book.class, "authors" );

		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.find( graph, uuid, LockModeType.PESSIMISTIC_READ );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.get( graph, uuid, LockModeType.PESSIMISTIC_READ );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}
}
