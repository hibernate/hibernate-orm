/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.nullargs;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = DetachedEntityManagerArgumentsTest.Thing.class)
class DetachedEntityManagerArgumentsTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			var operations = List.<Consumer<Object>>of(
					em::refresh,
					e -> em.lock( e, LockModeType.NONE ),
					em::getLockMode
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
