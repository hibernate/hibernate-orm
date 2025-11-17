/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nullargs;

import jakarta.persistence.LockModeType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa
class NullEntityManagerArgumentsTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			var operations = List.<Consumer<?>>of(
					em::persist,
					em::refresh,
					em::merge,
					em::detach,
					em::remove,
					e -> em.lock( e, LockModeType.NONE ),
					em::getLockMode,
					em::contains
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
}
