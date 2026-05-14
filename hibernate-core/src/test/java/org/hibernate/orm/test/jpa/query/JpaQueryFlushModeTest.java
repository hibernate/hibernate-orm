/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {
		JpaQueryFlushModeTest.Thing.class,
		JpaQueryFlushModeTest.OtherThing.class
})
@JiraKey("HHH-20366")
class JpaQueryFlushModeTest {
	@Test
	void queryFlushModeFlushForcesFlushEvenWhenEntityManagerIsExplicit(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();

		scope.inTransaction( entityManager -> {
			entityManager.setFlushMode( FlushModeType.EXPLICIT );
			entityManager.persist( new Thing( 1L, "thing" ) );

			entityManager.createQuery( "select o from Jpa4OtherThing o", OtherThing.class )
					.setQueryFlushMode( QueryFlushMode.FLUSH )
					.getResultList();

			assertEquals( 1L, countRows( entityManager.unwrap( Session.class ), "JPA4_FLUSH_THING" ) );
		} );
	}

	@Test
	void queryFlushModeNoFlushSuppressesOverlappingAutoFlush(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();

		scope.inTransaction( entityManager -> {
			entityManager.persist( new Thing( 2L, "thing" ) );

			entityManager.createQuery( "select t from Jpa4FlushThing t", Thing.class )
					.setQueryFlushMode( QueryFlushMode.NO_FLUSH )
					.getResultList();

			assertEquals( 0L, countRows( entityManager.unwrap( Session.class ), "JPA4_FLUSH_THING" ) );
		} );
	}

	@Test
	void queryFlushModeDefaultInheritsExplicitFlushMode(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();

		scope.inTransaction( entityManager -> {
			entityManager.setFlushMode( FlushModeType.EXPLICIT );
			entityManager.persist( new Thing( 3L, "thing" ) );

			entityManager.createQuery( "select t from Jpa4FlushThing t", Thing.class )
					.setQueryFlushMode( QueryFlushMode.DEFAULT )
					.getResultList();

			assertEquals( 0L, countRows( entityManager.unwrap( Session.class ), "JPA4_FLUSH_THING" ) );
		} );
	}

	@Test
	void explicitFlushModeSkipsCommitFlush(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();

		scope.inTransaction( entityManager -> {
			entityManager.setFlushMode( FlushModeType.EXPLICIT );
			entityManager.persist( new Thing( 4L, "thing" ) );
		} );

		scope.inTransaction( entityManager ->
				assertEquals( 0L, countRows( entityManager.unwrap( Session.class ), "JPA4_FLUSH_THING" ) ) );
	}

	private static long countRows(Session session, String tableName) {
		return session.doReturningWork( connection -> {
			try ( Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery( "select count(*) from " + tableName ) ) {
				resultSet.next();
				return resultSet.getLong( 1 );
			}
		} );
	}

	@Entity(name = "Jpa4FlushThing")
	@Table(name = "JPA4_FLUSH_THING")
	public static class Thing {
		@Id
		private Long id;

		private String name;

		public Thing() {
		}

		public Thing(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Jpa4OtherThing")
	@Table(name = "JPA4_OTHER_THING")
	public static class OtherThing {
		@Id
		private Long id;
	}
}
