/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SessionFactory
@DomainModel( annotatedClasses = {
		LeftJoinFetchSubclassesTest.Entity1.class,
		LeftJoinFetchSubclassesTest.SuperClass.class,
		LeftJoinFetchSubclassesTest.SubClass1.class,
		LeftJoinFetchSubclassesTest.SubClass2.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16798" )
public class LeftJoinFetchSubclassesTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Entity1 entity1A = new Entity1( 1L );
			session.persist( entity1A );
			final SubClass1 subClass1 = new SubClass1();
			subClass1.setId( 2L );
			subClass1.setEntity1( entity1A );
			session.persist( subClass1 );
			final Entity1 entity1B = new Entity1( 3L );
			session.persist( entity1B );
			final SubClass2 subClass2 = new SubClass2();
			subClass2.setId( 4L );
			subClass2.setEntity1( entity1B );
			session.persist( subClass2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testJoinFetchSub1(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//noinspection removal
			final Entity1 entity1 = session.createQuery(
					"select e from Entity1 e left join fetch e.subClass1 where e.id = 1",
					Entity1.class
			).getSingleResult();
			assertThat( entity1.getSubClass1().getId() ).isEqualTo( 2L );
			assertThat( entity1.getSubClass2() ).isNull();
		} );
	}

	@Test
	public void testJoinFetchSub2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//noinspection removal
			final Entity1 entity1 = session.createQuery(
					"select e from Entity1 e left join fetch e.subClass2 where e.id = 3",
					Entity1.class
			).getSingleResult();
			assertThat( entity1.getSubClass1() ).isNull();
			assertThat( entity1.getSubClass2().getId() ).isEqualTo( 4L );
		} );
	}

	@Test
	public void testJoinFetchBoth(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//noinspection removal
			final List<Entity1> resultList = session.createQuery(
					"select e from Entity1 e left join fetch e.subClass1 left join fetch e.subClass2",
					Entity1.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
		} );
	}

	@Test
	public void testJoinBoth(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//noinspection removal
			final List<Entity1> resultList = session.createQuery(
					"select e from Entity1 e left join e.subClass1 left join e.subClass2",
					Entity1.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
		} );
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		private Long id;

		public Entity1() {
		}

		public Entity1(Long id) {
			this.id = id;
		}

		@OneToOne( fetch = FetchType.LAZY, mappedBy = "entity1" )
		private SubClass1 subClass1;

		@OneToOne( fetch = FetchType.LAZY, mappedBy = "entity1" )
		private SubClass2 subClass2;

		public SubClass1 getSubClass1() {
			return subClass1;
		}

		public SubClass2 getSubClass2() {
			return subClass2;
		}
	}

	@Entity( name = "SuperClass" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	public static abstract class SuperClass {
		@Id
		private Long id;

		@OneToOne
		private Entity1 entity1;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Entity1 getEntity1() {
			return entity1;
		}

		public void setEntity1(Entity1 entity1) {
			this.entity1 = entity1;
		}
	}

	@Entity( name = "SubClass1" )
	@DiscriminatorValue( "1" )
	public static class SubClass1 extends SuperClass {
	}

	@Entity( name = "SubClass2" )
	@DiscriminatorValue( "2" )
	public static class SubClass2 extends SuperClass {
	}
}
