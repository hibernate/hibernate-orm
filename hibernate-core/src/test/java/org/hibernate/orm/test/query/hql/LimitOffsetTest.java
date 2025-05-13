/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = LimitOffsetTest.Sortable.class)
class LimitOffsetTest {
	@Test
	void testLimitOffset(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Sortable() );
			session.persist( new Sortable() );
			session.persist( new Sortable() );
			session.persist( new Sortable() );
		} );
		scope.inTransaction( session -> {
			assertEquals( 2, session.createQuery( "from Sortable limit 2" ).getResultList().size() );
			assertEquals( 2, session.createQuery( "from Sortable offset 2" ).getResultList().size() );
			assertEquals( 1, session.createQuery( "from Sortable limit 1 offset 1" ).getResultList().size() );
		} );
	}
	@Entity(name = "Sortable")
	static class Sortable {
		@Id
		@GeneratedValue
		UUID uuid;
	}
}
