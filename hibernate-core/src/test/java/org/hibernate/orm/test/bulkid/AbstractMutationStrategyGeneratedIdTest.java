/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				AbstractMutationStrategyGeneratedIdTest.Person.class,
				AbstractMutationStrategyGeneratedIdTest.Doctor.class,
				AbstractMutationStrategyGeneratedIdTest.Engineer.class
		}
)
@SessionFactory
public abstract class AbstractMutationStrategyGeneratedIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Doctor doctor = new Doctor();
			doctor.setName( "Doctor John" );
			doctor.setEmployed( true );
			session.persist( doctor );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testInsertStatic(SessionFactoryScope scope) {
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
	public void testInsertGenerated(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery(
							"insert into Engineer(name, employed, fellow) values (:name, :employed, false)" )
					.setParameter( "name", "John Doe" )
					.setParameter( "employed", true )
					.executeUpdate();
			final Engineer engineer = session.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
					.getSingleResult();
			assertThat( engineer.getName() ).isEqualTo( "John Doe" );
			assertThat( engineer.isEmployed() ).isTrue();
			assertThat( engineer.isFellow() ).isFalse();
		} );
	}

	@Test
	public void testInsertSelectStatic(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final int insertCount = session.createMutationQuery( "insert into Engineer(id, name, employed, fellow) "
																+ "select d.id + 1, 'John Doe', true, false from Doctor d" )
					.executeUpdate();

			final Engineer engineer = session.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
					.getSingleResult();
			assertThat( insertCount ).isEqualTo( 1 );
			assertThat( engineer.getName() ).isEqualTo( "John Doe" );
			assertThat( engineer.isEmployed() ).isTrue();
			assertThat( engineer.isFellow() ).isFalse();
		} );
	}

	@Test
	public void testInsertSelectGenerated(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final int insertCount = session.createMutationQuery( "insert into Engineer(name, employed, fellow) "
																+ "select 'John Doe', true, false from Doctor d" )
					.executeUpdate();
			final Engineer engineer = session.createQuery( "from Engineer e where e.name = 'John Doe'", Engineer.class )
					.getSingleResult();
			assertThat( insertCount ).isEqualTo( 1 );
			assertThat( engineer.getName() ).isEqualTo( "John Doe" );
			assertThat( engineer.isEmployed() ).isTrue();
			assertThat( engineer.isFellow() ).isFalse();
		} );
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@SequenceGenerator(allocationSize = 1)
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

}
