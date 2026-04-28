/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.ScrollMode;
import org.hibernate.jpa.HibernateHints;
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
			assertEquals(
					2,
					session.createQuery( "from Sortable order by uuid" )
							.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, true )
							.setFirstResult( 1 )
							.setMaxResults( 2 )
							.getResultList()
							.size()
			);
			assertEquals(
					1,
					session.createQuery( "from Sortable order by uuid limit 1 offset 1" )
							.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, true )
							.getResultList()
							.size()
			);
			try ( var stream = session.createQuery( "from Sortable order by uuid" )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, true )
					.setFirstResult( 1 )
					.setMaxResults( 2 )
					.getResultStream() ) {
				assertEquals( 2L, stream.count() );
			}
			try ( var scroll = session.unwrap( org.hibernate.Session.class )
					.createQuery( "from Sortable order by uuid", Sortable.class )
					.setHint( HibernateHints.HINT_LIMIT_IN_MEMORY, true )
					.setFirstResult( 1 )
					.setMaxResults( 2 )
					.scroll( ScrollMode.FORWARD_ONLY ) ) {
				long count = 0;
				while ( scroll.next() ) {
					count++;
				}
				assertEquals( 2L, count );
			}
		} );
	}
	@Entity(name = "Sortable")
	static class Sortable {
		@Id
		@GeneratedValue
		UUID uuid;
	}
}
