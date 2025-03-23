/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.autoflush;

import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_SPACES;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = JpaAutoflushTest.Thing.class)
public class JpaAutoflushTest {

	@Test void test1(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing( "Widget" ) );
			List<?> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.getResultList();
			// spec says we must flush before native query in tx
			assertEquals( 1, resultList.size() );
		} );
	}

	@Test void test2(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing("Widget") );
			List<?> resultList =
					em.createNativeQuery( "select typeOfThing from Thing", String.class )
							.getResultList();
			// spec says we must flush before native query in tx
			assertEquals(1, resultList.size());
		} );
	}

	@Test void test3(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inEntityManager( em -> {
			em.persist( new Thing("Widget") );
			List<?> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.getResultList();
			// spec says we must NOT flush before native query outside tx
			assertEquals(0, resultList.size());
		} );
	}

	@Test void test4(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.setFlushMode( FlushModeType.COMMIT );
			em.persist( new Thing("Widget") );
			List<?> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.getResultList();
			// spec says we must NOT flush before native query with FMT.COMMIT
			assertEquals(0, resultList.size());
		} );
	}

	@Test void test5(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing("Widget") );
			List<?> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.setHint( HINT_NATIVE_SPACES, "Thing" )
							.getResultList();
			// we should not flush because user specified that the query touches the table
			assertEquals(1, resultList.size());
		} );
	}

	@Test void test6(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing("Widget") );
			List<?> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.setHint( HINT_NATIVE_SPACES, "XXX" )
							.getResultList();
			// we should not flush because user specified that the query doesn't touch the table
			assertEquals(0, resultList.size());
		} );
	}

	@Entity(name="Thing")
	public static class Thing {
		@Id
		long id;
		String typeOfThing;

		public Thing(String typeOfThing) {
			this.typeOfThing = typeOfThing;
		}

		public Thing() {
		}
	}
}
