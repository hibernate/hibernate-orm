/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				AbstractMutationStrategyIdTest.Person.class,
				AbstractMutationStrategyIdTest.Doctor.class,
				AbstractMutationStrategyIdTest.Engineer.class
		}
)
@SessionFactory
public abstract class AbstractMutationStrategyIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < entityCount(); i++ ) {
				Doctor doctor = new Doctor();
				doctor.setId( i + 1 );
				doctor.setEmployed( (i % 2) == 0 );
				session.persist( doctor );
			}

			for ( int i = 0; i < entityCount(); i++ ) {
				Engineer engineer = new Engineer();
				engineer.setId( i + 1 + entityCount() );
				engineer.setEmployed( (i % 2) == 0 );
				engineer.setFellow( (i % 2) == 1 );
				session.persist( engineer );
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}


	protected int entityCount() {
		return 10;
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updateCount = session.createMutationQuery( "update Person set name = :name where employed = :employed" )
					.setParameter( "name", "John Doe" )
					.setParameter( "employed", true )
					.executeUpdate();

			assertThat( updateCount ).isEqualTo( entityCount() );
		} );
	}

	@Test
	@Jira(value = "HHH-18373")
	public void testNullValueUpdateWithCriteria(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityManager entityManager = session.unwrap( EntityManager.class );

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaUpdate<Person> update = cb.createCriteriaUpdate( Person.class ).set( "name", null );
			Root<Person> person = update.from( Person.class );
			update.where( cb.equal( person.get( "employed" ), true ) );
			int updateCount = entityManager.createQuery( update ).executeUpdate();

			assertThat( updateCount ).isEqualTo( entityCount() );
		} );
	}

	@Test
	public void testDeleteFromPerson(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updateCount = session.createMutationQuery(
							"delete from Person where employed = :employed" )
					.setParameter( "employed", false )
					.executeUpdate();
			assertThat( updateCount ).isEqualTo( entityCount() );
		} );
	}

	@Test
	public void testDeleteFromEngineer(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updateCount = session.createMutationQuery( "delete from Engineer where fellow = :fellow" )
					.setParameter( "fellow", true )
					.executeUpdate();
			assertThat( updateCount ).isEqualTo( entityCount() / 2 );
		} );
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery(
							"insert into Engineer(id, name, employed, fellow) values (0, :name, :employed, false)" )
					.setParameter( "name", "John Doe" )
					.setParameter( "employed", true )
					.executeUpdate();

			final Engineer engineer = session.find( Engineer.class, 0 );
			assertThat( engineer.getName() ).isEqualTo( "John Doe" );
			assertThat( engineer.isEmployed() ).isTrue();
			assertThat( engineer.isFellow() ).isFalse();
		} );
	}

	@Test
	public void testInsertSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final int insertCount = session.createQuery( "insert into Engineer(id, name, employed, fellow) "
														+ "select d.id + " + (entityCount() * 2) + ", 'John Doe', true, false from Doctor d" )
					.executeUpdate();
			final AbstractMutationStrategyIdTest.Engineer engineer =
					session.find( AbstractMutationStrategyIdTest.Engineer.class, entityCount() * 2 + 1 );
			assertThat( insertCount ).isEqualTo( entityCount() );
			assertThat( engineer.getName() ).isEqualTo( "John Doe" );
			assertThat( engineer.isEmployed() ).isTrue();
			assertThat( engineer.isFellow() ).isFalse();
		} );
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		private Integer id;

		private String name;

		private boolean employed;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isEmployed() {
			return employed;
		}

		public void setEmployed(boolean employed) {
			this.employed = employed;
		}
	}
	//end::batch-bulk-hql-temp-table-base-class-example[]

	//tag::batch-bulk-hql-temp-table-sub-classes-example[]
	@Entity(name = "Doctor")
	public static class Doctor extends Person {
	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {

		private boolean fellow;

		public boolean isFellow() {
			return fellow;
		}

		public void setFellow(boolean fellow) {
			this.fellow = fellow;
		}
	}
	//end::batch-bulk-hql-temp-table-sub-classes-example[]

}
