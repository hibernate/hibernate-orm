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
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				AbstractMutationStrategyGeneratedIdentityTest.Person.class,
				AbstractMutationStrategyGeneratedIdentityTest.Doctor.class,
				AbstractMutationStrategyGeneratedIdentityTest.Engineer.class
		}
)
@SessionFactory
public abstract class AbstractMutationStrategyGeneratedIdentityTest {

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
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true,
			reason = "MySQL ignores a provided value for an auto_increment column if it's lower than the current sequence value")
	@SkipForDialect(dialectClass = AbstractTransactSQLDialect.class, matchSubTypes = true,
			reason = "T-SQL complains IDENTITY_INSERT is off when a value for an identity column is provided")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix counts from 1 like a normal person")
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
	@SkipForDialect(dialectClass = OracleDialect.class,
			reason = "Oracle doesn't support insert-select with a returning clause")
	public void testInsertGenerated(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "insert into Engineer(name, employed, fellow) values (:name, :employed, false)" )
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
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true,
			reason = "MySQL ignores a provided value for an auto_increment column if it's lower than the current sequence value")
	@SkipForDialect(dialectClass = AbstractTransactSQLDialect.class, matchSubTypes = true,
			reason = "T-SQL complains IDENTITY_INSERT is off when a value for an identity column is provided")
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
	@SkipForDialect(dialectClass = OracleDialect.class,
			reason = "Oracle doesn't support insert-select with a returning clause")
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
		@GeneratedValue(strategy = GenerationType.IDENTITY)
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
