/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.semantics;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityHandler;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TransactionRequiredException;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the semantics of the loading with locks using get/find methods.
 *
 * @see jakarta.persistence.EntityHandler#find(Class, Object, jakarta.persistence.FindOption...)
 * @see jakarta.persistence.EntityHandler#get(Class, Object, jakarta.persistence.FindOption...)
 *
 * @author Steve Ebersole
 */
public class LockingLoadSemanticTests extends Base {
	private static final List<Integer> MULTIPLE_IDS = List.of( 1, 2 );

	@Test
	void testTransactionalLoadingStateful(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var book = session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
			assertThat( book ).isNotNull();
		} );

		factoryScope.inTransaction( (session) -> {
			var book = session.get( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
			assertThat( book ).isNotNull();
		} );
	}

	@Test
	void testTransactionalLoadingStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessTransaction( (session) -> {
			var book = session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
			assertThat( book ).isNotNull();
		} );

		factoryScope.inStatelessTransaction( (session) -> {
			var book = session.get( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
			assertThat( book ).isNotNull();
		} );
	}

	@Test
	void testNonTransactionalLoadingStateful(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			try {
				session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (TransactionRequiredException expected) {}
		} );

		factoryScope.inSession( (session) -> {
			try {
				session.get( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (TransactionRequiredException expected) {}
		} );
	}

	@Test
	void testNonTransactionalLoadingStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessSession( (session) -> {
			try {
				session.find( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (TransactionRequiredException expected) {}
		} );

		factoryScope.inStatelessSession( (session) -> {
			try {
				session.get( Book.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (TransactionRequiredException expected) {}
		} );
	}

	@Test
	void testNonTransactionalMultipleLoadingStateful(SessionFactoryScope factoryScope) {
		var graph = factoryScope.getSessionFactory().createEntityGraph( Book.class );

		factoryScope.inSession( (session) -> assertNonTransactionalMultipleLoadingRequiresTransaction( session, graph ) );
	}

	@Test
	void testNonTransactionalMultipleLoadingStateless(SessionFactoryScope factoryScope) {
		var graph = factoryScope.getSessionFactory().createEntityGraph( Book.class );

		factoryScope.inStatelessSession(
				(session) -> assertNonTransactionalMultipleLoadingRequiresTransaction( session, graph )
		);
	}

	@Test
	void testNoMatchStateful(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var book = session.find( Book.class, 9, LockModeType.PESSIMISTIC_WRITE );
			assertThat( book ).isNull();
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Book.class, 9, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (EntityNotFoundException expected) {}
		} );
	}

	@Test
	void testNoMatchStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessTransaction( (session) -> {
			var book = session.find( Book.class, 9, LockModeType.PESSIMISTIC_WRITE );
			assertThat( book ).isNull();
		} );
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.get( Book.class, 9, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (EntityNotFoundException expected) {}
		} );
	}

	@Test
	void testBadKeyTypeStateful(SessionFactoryScope factoryScope) {
		var uuid = UUID.randomUUID();

		factoryScope.inTransaction( (session) -> {
			try {
				session.find( Book.class, uuid, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Book.class, uuid, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}

	@Test
	void testBadKeyTypeStateless(SessionFactoryScope factoryScope) {
		var uuid = UUID.randomUUID();

		factoryScope.inTransaction( (session) -> {
			try {
				session.find( Book.class, uuid, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Book.class, uuid, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}

	@Test
	void testBadEntityTypeStateful(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.find( Long.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Long.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}

	@Test
	void testBadEntityTypeStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.find( Long.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.get( Long.class, 1, LockModeType.PESSIMISTIC_WRITE );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}

	private static void assertTransactionRequired(Executable executable) {
		assertThrows( TransactionRequiredException.class, executable );
	}

	private static void assertNonTransactionalMultipleLoadingRequiresTransaction(
			EntityHandler entityHandler,
			EntityGraph<Book> graph) {
		assertTransactionRequired(
				() -> entityHandler.findMultiple( Book.class, MULTIPLE_IDS, LockModeType.PESSIMISTIC_READ )
		);
		assertTransactionRequired(
				() -> entityHandler.getMultiple( Book.class, MULTIPLE_IDS, LockModeType.PESSIMISTIC_READ )
		);
		assertTransactionRequired( () -> entityHandler.findMultiple( graph, MULTIPLE_IDS, LockModeType.PESSIMISTIC_READ ) );
		assertTransactionRequired( () -> entityHandler.getMultiple( graph, MULTIPLE_IDS, LockModeType.PESSIMISTIC_READ ) );
	}
}
