/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.semantics;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.KeyType;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests the semantics of the loading based on natural id using get/find methods.
 *
 * @see jakarta.persistence.EntityHandler#find(Class, Object, jakarta.persistence.FindOption...)
 * @see jakarta.persistence.EntityHandler#get(Class, Object, jakarta.persistence.FindOption...)
 *
 * @author Steve Ebersole
 */
public class NaturalIdLoadSemanticTests extends Base {
	@Test
	void testTransactionalLoadingStateful(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var book = session.find( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );

		factoryScope.inTransaction( (session) -> {
			var book = session.get( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );
	}

	@Test
	void testTransactionalLoadingStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessTransaction( (session) -> {
			var book = session.find( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );

		factoryScope.inStatelessTransaction( (session) -> {
			var book = session.get( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );
	}

	@Test
	void testNonTransactionalLoadingStateful(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			var book = session.find( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );

		factoryScope.inSession( (session) -> {
			var book = session.get( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );
	}

	@Test
	void testNonTransactionalLoadingStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessSession( (session) -> {
			var book = session.find( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );

		factoryScope.inStatelessSession( (session) -> {
			var book = session.get( Book.class, "123", KeyType.NATURAL );
			assertThat( book ).isNotNull();
		} );
	}

	@Test
	void testNoMatchStateful(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var book = session.find( Book.class, "987", KeyType.NATURAL );
			assertThat( book ).isNull();
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Book.class, "987", KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (EntityNotFoundException expected) {}
		} );
	}

	@Test
	void testNoMatchStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessTransaction( (session) -> {
			var book = session.find( Book.class, "987", KeyType.NATURAL );
			assertThat( book ).isNull();
		} );
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.get( Book.class, "987", KeyType.NATURAL );
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
				session.find( Book.class, uuid, KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Book.class, uuid, KeyType.NATURAL );
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
				session.find( Book.class, uuid, KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Book.class, uuid, KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}

	@Test
	void testBadEntityTypeStateful(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.find( Long.class, "123", KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inTransaction( (session) -> {
			try {
				session.get( Long.class, "123", KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}

	@Test
	void testBadEntityTypeStateless(SessionFactoryScope factoryScope) {
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.find( Long.class, "123", KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
		factoryScope.inStatelessTransaction( (session) -> {
			try {
				session.get( Long.class, "123", KeyType.NATURAL );
				fail( "Expecting failure" );
			}
			catch (IllegalArgumentException expected) {}
		} );
	}
}
