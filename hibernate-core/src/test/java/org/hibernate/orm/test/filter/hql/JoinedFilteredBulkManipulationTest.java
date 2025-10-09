/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.hql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.orm.test.filter.AbstractStatefulStatelessFilterTest;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Steve Ebersole
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@DomainModel(
		annotatedClasses = {
				JoinedFilteredBulkManipulationTest.Person.class,
				JoinedFilteredBulkManipulationTest.User.class,
				JoinedFilteredBulkManipulationTest.Employee.class,
				JoinedFilteredBulkManipulationTest.Customer.class
		}
)
public class JoinedFilteredBulkManipulationTest extends AbstractStatefulStatelessFilterTest {

	@BeforeEach
	void setUp() {
		scope.inTransaction( session -> {
			session.persist( new Employee( "John", 'M', "john", new Date() ) );
			session.persist( new Employee( "Jane", 'F', "jane", new Date() ) );
			session.persist( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
			session.persist( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testFilteredJoinedSubclassHqlDeleteRoot(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			//noinspection deprecation
			final int count = session.createQuery( "delete Person" ).executeUpdate();
			assertThat( count ).isEqualTo( 2 );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testFilteredJoinedSubclassHqlDeleteNonLeaf(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			//noinspection deprecation
			final int count = session.createQuery( "delete User" ).executeUpdate();
			assertThat( count ).isEqualTo( 2 );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testFilteredJoinedSubclassHqlDeleteLeaf(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			//noinspection deprecation
			final int count = session.createQuery( "delete Employee" ).executeUpdate();
			assertThat( count ).isEqualTo( 1 );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testFilteredJoinedSubclassHqlUpdateRoot(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			//noinspection deprecation
			final int count = session.createQuery( "update Person p set p.name = '<male>'" ).executeUpdate();
			assertThat( count ).isEqualTo( 2 );
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testFilteredJoinedSubclassHqlUpdateNonLeaf(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			//noinspection deprecation
			final int count = session.createQuery( "update User u set u.username = :un where u.name = :n" )
					.setParameter( "un", "charlie" )
					.setParameter( "n", "Wanda" )
					.executeUpdate();
			assertThat( count ).isZero();
		} );
	}

	@ParameterizedTest
	@MethodSource("transactionKind")
	void testFilteredJoinedSubclassHqlUpdateLeaf(BiConsumer<SessionFactoryScope, Consumer<? extends SharedSessionContract>> inTransaction) {
		inTransaction.accept( scope, session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			//noinspection deprecation
			final int count = session.createQuery( "update Customer c set c.company = 'XYZ'" ).executeUpdate();
			assertThat( count ).isEqualTo( 1 );
		} );
	}

	@AfterEach
	void tearDown() {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	@Entity( name = "Person" )
	@Table( name = "FILTER_HQL_JOINED_PERSON" )
	@Inheritance(strategy = InheritanceType.JOINED)
	@FilterDef(
			name = "sex",
			parameters = @ParamDef(
					name= "sexCode",
					type = Character.class
			)
	)
	@Filter( name = "sex", condition = "SEX_CODE = :sexCode" )
	public static class Person {

		@Id @GeneratedValue
		private Long id;

		private String name;

		@Column( name = "SEX_CODE" )
		private char sex;

		protected Person() {
		}

		public Person(String name, char sex) {
			this.name = name;
			this.sex = sex;
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "User" )
	@Table( name = "FILTER_HQL_JOINED_USER" )
	public static class User extends Person {
		private String username;

		protected User() {
			super();
		}

		public User(String name, char sex, String username) {
			super( name, sex );
			this.username = username;
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "Employee" )
	@Table( name = "FILTER_HQL_JOINED_EMP" )
	public static class Employee extends User {
		private Date hireDate;

		protected Employee() {
			super();
		}

		public Employee(String name, char sex, String username, Date hireDate) {
			super( name, sex, username );
			this.hireDate = hireDate;
		}
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity( name = "Customer" )
	@Table( name = "FILTER_HQL_JOINED_CUST" )
	public static class Customer extends User {
		private String company;

		protected Customer() {
			super();
		}

		public Customer(String name, char sex, String username, String company) {
			super( name, sex, username );
			this.company = company;
		}
	}
}
