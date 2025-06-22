/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nullargs;

import jakarta.persistence.LockModeType;
import org.hibernate.ReplicationMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory @DomainModel
class NullSessionArgumentsTest {
	@Test void test(SessionFactoryScope scope) {
		scope.getSessionFactory().inTransaction( session -> {
			var operations = List.<Consumer<?>>of(
					session::persist,
					session::refresh,
					session::merge,
					session::detach,
					session::evict,
					session::remove,
					e -> session.lock( e, LockModeType.NONE ),
					e -> session.replicate( e, ReplicationMode.EXCEPTION ),
					session::getLockMode,
					session::getEntityName,
					session::getIdentifier,
					session::contains,
					session::isReadOnly,
					e -> session.setReadOnly( e, true )
			);
			operations.forEach( c -> {
						try {
							c.accept( null );
						}
						catch ( IllegalArgumentException e ) {
							assertTrue( e.getMessage().startsWith( "Entity may not be null" ) );
						}
					} );
		} );
	}
	@Test void testWithEntityName(SessionFactoryScope scope) {
		scope.inStatelessTransaction( ss -> {
			var operations = List.<BiConsumer<String,Object>>of(
					ss::insert,
					ss::upsert,
					ss::update,
					ss::delete,
					ss::refresh
			);
			operations.forEach( c -> {
				try {
					c.accept( "Entity", null );
				}
				catch ( IllegalArgumentException e ) {
					assertTrue( e.getMessage().startsWith( "Entity may not be null" ) );
				}
			} );
		} );
	}
}
