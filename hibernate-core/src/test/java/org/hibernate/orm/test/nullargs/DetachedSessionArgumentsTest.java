/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nullargs;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = DetachedSessionArgumentsTest.Thing.class)
class DetachedSessionArgumentsTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var operations = List.<Consumer<Object>>of(
					session::refresh,
					e -> session.lock( e, LockModeType.NONE ),
					session::getLockMode,
					session::isReadOnly,
					e -> session.setReadOnly( e, true ),
					session::getIdentifier

			);
			Thing thing = new Thing();
			thing.id = 5L;
			operations.forEach( c -> {
				try {
					c.accept( thing );
				}
				catch ( IllegalArgumentException e ) {
					assertTrue( e.getMessage().startsWith( "Given entity is not associated with the persistence context" ) );
				}
			} );
		} );
	}
	@Entity static class Thing {
		@Id
		Long id;
	}
}
