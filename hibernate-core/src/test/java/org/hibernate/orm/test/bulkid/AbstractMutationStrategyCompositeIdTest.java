/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				AbstractMutationStrategyCompositeIdTest.Person.class,
				AbstractMutationStrategyCompositeIdTest.Doctor.class,
				AbstractMutationStrategyCompositeIdTest.Engineer.class
		}
)
@SessionFactory
public abstract class AbstractMutationStrategyCompositeIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < entityCount(); i++ ) {
				Doctor doctor = new Doctor();
				doctor.setId( i + 1 );
				doctor.setCompanyName( "Red Hat USA" );
				doctor.setEmployed( (i % 2) == 0 );
				session.persist( doctor );
			}

			for ( int i = 0; i < entityCount(); i++ ) {
				Engineer engineer = new Engineer();
				engineer.setId( i + 1 + entityCount() );
				engineer.setCompanyName( "Red Hat Europe" );
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
		return 4;
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
	public void testDeleteFromPerson(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			//tag::batch-bulk-hql-temp-table-delete-query-example[]
			int updateCount = session.createMutationQuery(
							"delete from Person where employed = :employed" )
					.setParameter( "employed", false )
					.executeUpdate();
			//end::batch-bulk-hql-temp-table-delete-query-example[]
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
							"insert into Engineer(id, companyName, name, employed, fellow) values (0, 'Red Hat', :name, :employed, false)" )
					.setParameter( "name", "John Doe" )
					.setParameter( "employed", true )
					.executeUpdate();
			final Engineer engineer = session.find( Engineer.class,
					new AbstractMutationStrategyCompositeIdTest_.Person_.Id( 0, "Red Hat" ) );
			assertThat( engineer.getName() ).isEqualTo( "John Doe" );
			assertThat( engineer.isEmployed() ).isTrue();
			assertThat( engineer.isFellow() ).isFalse();
		} );
	}

	@Test
	public void testInsertSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final int insertCount = session.createMutationQuery(
							"insert into Engineer(id, companyName, name, employed, fellow) "
							+ "select d.id + " + (entityCount() * 2) + ", 'Red Hat', 'John Doe', true, false from Doctor d" )
					.executeUpdate();
			final Engineer engineer = session.find( Engineer.class,
					new AbstractMutationStrategyCompositeIdTest_.Person_.Id( entityCount() * 2 + 1, "Red Hat" ) );
			assertThat( insertCount ).isEqualTo( entityCount() );
			assertThat( engineer.getName() ).isEqualTo( "John Doe" );
			assertThat( engineer.isEmployed() ).isTrue();
			assertThat( engineer.isFellow() ).isFalse();
		} );
	}

	//tag::batch-bulk-hql-temp-table-base-class-example[]
	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person implements Serializable {

		@Id
		private Integer id;

		@Id
		private String companyName;

		private String name;

		private boolean employed;

		//Getters and setters are omitted for brevity

		//end::batch-bulk-hql-temp-table-base-class-example[]

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getCompanyName() {
			return companyName;
		}

		public void setCompanyName(String companyName) {
			this.companyName = companyName;
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

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !(o instanceof Person) ) {
				return false;
			}
			Person person = (Person) o;
			return Objects.equals( getId(), person.getId() ) &&
				Objects.equals( getCompanyName(), person.getCompanyName() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getId(), getCompanyName() );
		}
		//tag::batch-bulk-hql-temp-table-base-class-example[]
	}
	//end::batch-bulk-hql-temp-table-base-class-example[]

	//tag::batch-bulk-hql-temp-table-sub-classes-example[]
	@Entity(name = "Doctor")
	public static class Doctor extends Person {
	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {

		private boolean fellow;

		//Getters and setters are omitted for brevity

		//end::batch-bulk-hql-temp-table-sub-classes-example[]

		public boolean isFellow() {
			return fellow;
		}

		public void setFellow(boolean fellow) {
			this.fellow = fellow;
		}
		//tag::batch-bulk-hql-temp-table-sub-classes-example[]
	}
	//end::batch-bulk-hql-temp-table-sub-classes-example[]
}
