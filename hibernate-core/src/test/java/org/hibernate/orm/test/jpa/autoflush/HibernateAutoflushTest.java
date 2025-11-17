/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.autoflush;

import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = HibernateAutoflushTest.Thing.class)
public class HibernateAutoflushTest {

	@Test void test1(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing( "Widget" ) );
			List<Thing> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.getResultList();
			// Hibernate does NOT autoflush before a native query
			assertEquals( 0, resultList.size() );
		} );
	}

	@Test void test2(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing("Widget") );
			List<String> resultList =
					em.createNativeQuery( "select typeOfThing from Thing", String.class )
							.getResultList();
			// Hibernate does NOT autoflush before a native query
			assertEquals(0, resultList.size());
		} );
	}

	@Test void test3(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inSession( em -> {
			em.persist( new Thing("Widget") );
			List<Thing> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.getResultList();
			// spec says we must NOT flush before native query outside tx
			assertEquals(0, resultList.size());
		} );
	}

	@Test void test4(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.setFlushMode( FlushModeType.COMMIT );
			em.persist( new Thing("Widget") );
			List<Thing> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.getResultList();
			// spec says we must NOT flush before native query with FMT.COMMIT
			assertEquals(0, resultList.size());
		} );
	}

	@Test void test5(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing("Widget") );
			List<Thing> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.addSynchronizedQuerySpace( "Thing" )
							.getResultList();
			// we should not flush because user specified that the query touches the table
			assertEquals(1, resultList.size());
		} );
	}

	@Test void test6(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing("Widget") );
			List<Thing> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.addSynchronizedQuerySpace( "XXX" )
							.getResultList();
			// we should not flush because user specified that the query doesn't touch the table
			assertEquals(0, resultList.size());
		} );
	}

	@Test void test7(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( em -> {
			em.persist( new Thing( "Widget" ) );
			List<Thing> resultList =
					em.createNativeQuery( "select * from Thing", Thing.class )
							.setQueryFlushMode( QueryFlushMode.FLUSH )
							.getResultList();
			// we should flush because of the QueryFlushMode
			assertEquals( 1, resultList.size() );
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
