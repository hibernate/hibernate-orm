/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nullargs;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory @DomainModel
class NullStatelessSessionArgumentsTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inStatelessTransaction( ss -> {
			var operations = List.<Consumer<Object>>of(
					ss::insert,
					ss::upsert,
					ss::update,
					ss::delete,
					ss::refresh,
					ss::getIdentifier
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
