/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idprops;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				IdPropertyInSubclassIdInMappedSuperclassTest.Human.class,
				IdPropertyInSubclassIdInMappedSuperclassTest.Genius.class
		}
)
@SessionFactory
public class IdPropertyInSubclassIdInMappedSuperclassTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Genius() );
			session.persist( new Genius( 1L ) );
			session.persist( new Genius( 1L ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-13114")
	public void testHql(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals(
					2,
					session.createQuery( "from Genius g where g.id = :id", Genius.class )
							.setParameter( "id", 1L )
							.list()
							.size()
			);

			assertEquals(
					1,
					session.createQuery( "from Genius g where g.id is null", Genius.class )
							.list()
							.size()
			);

			assertEquals( 3L, session.createQuery( "select count( g ) from Genius g" ).uniqueResult() );

			assertEquals(
					2,
					session.createQuery( "from Genius g where g.id = :id", Human.class )
							.setParameter( "id", 1L )
							.list()
							.size()
			);

			assertEquals(
					1,
					session.createQuery( "from Human h where h.id = :id", Human.class )
							.setParameter( "id", 1L )
							.list()
							.size()
			);

			assertEquals(
					0,
					session.createQuery( "from Human h where h.id is null", Human.class )
							.list()
							.size()
			);

			assertEquals( 3L, session.createQuery( "select count( h ) from Human h" ).uniqueResult() );

		} );
	}

	@MappedSuperclass
	public static class Animal {

		private Long realId;

		private String description;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "realId")
		public Long getRealId() {
			return realId;
		}

		public void setRealId(Long realId) {
			this.realId = realId;
		}
	}

	@Entity(name = "Human")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Human extends Animal {
		private String name;
	}

	@Entity(name = "Genius")
	public static class Genius extends Human {
		private Long id;

		private int age;

		public Genius() {
		}

		public Genius(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}
}
