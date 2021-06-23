/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.filter.hql;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
@SessionFactory
public class JoinedFilteredBulkManipulationTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.save( new Employee( "John", 'M', "john", new Date() ) );
			session.save( new Employee( "Jane", 'F', "jane", new Date() ) );
			session.save( new Customer( "Charlie", 'M', "charlie", "Acme" ) );
			session.save( new Customer( "Wanda", 'F', "wanda", "ABC" ) );
		} );
	}

	@Test
	void testFilteredJoinedSubclassHqlDeleteRoot(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			final int count = session.createQuery( "delete Person" ).executeUpdate();
			assertThat( count, is( 2 ) );
		} );
	}

	@Test
	void testFilteredJoinedSubclassHqlDeleteNonLeaf(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			final int count = session.createQuery( "delete User" ).executeUpdate();
			assertThat( count, is( 2 ) );
		} );
	}

	@Test
	void testFilteredJoinedSubclassHqlDeleteLeaf(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			final int count = session.createQuery( "delete Employee" ).executeUpdate();
			assertThat( count, is( 1 ) );
		} );
	}

	@Test
	void testFilteredJoinedSubclassHqlUpdateRoot(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			final int count = session.createQuery( "update Person p set p.name = '<male>'" ).executeUpdate();
			assertThat( count, is( 2 ) );
		} );
	}

	@Test
	void testFilteredJoinedSubclassHqlUpdateNonLeaf(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			final int count = session.createQuery( "update User u set u.username = :un where u.name = :n" )
					.setParameter( "un", "charlie" )
					.setParameter( "n", "Wanda" )
					.executeUpdate();
			assertThat( count, is( 0 ) );
		} );
	}

	@Test
	void testFilteredJoinedSubclassHqlUpdateLeaf(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "sex" ).setParameter( "sexCode", 'M' );
			final int count = session.createQuery( "update Customer c set c.company = 'XYZ'" ).executeUpdate();
			assertThat( count, is( 1 ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete Person" ).executeUpdate();
		} );
	}

	@Entity( name = "Person" )
	@Table( name = "FILTER_HQL_JOINED_PERSON" )
	@Inheritance(strategy = InheritanceType.JOINED)
	@FilterDef(
			name = "sex",
			parameters = @ParamDef(
					name= "sexCode",
					type = "char"
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
