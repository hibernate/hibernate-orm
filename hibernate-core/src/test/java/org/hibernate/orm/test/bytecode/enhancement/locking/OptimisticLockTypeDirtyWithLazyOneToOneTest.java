/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.locking;

import java.util.List;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@JiraKey("HHH-16839")
public class OptimisticLockTypeDirtyWithLazyOneToOneTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Address.class,
				Person.class
		};
	}

	@Override
	protected void afterConfigurationBuilt(Configuration configuration) {
		super.afterConfigurationBuilt( configuration );
		configuration.setStatementInspector( new SQLStatementInspector() );
	}

	SQLStatementInspector statementInspector() {
		return (SQLStatementInspector) sessionFactory().getSessionFactoryOptions().getStatementInspector();
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from Person" ).executeUpdate();
					session.createMutationQuery( "delete from Address" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate() {
		inTransaction(
				session -> {
					Person person = new Person( 1L, "Name", new Address( 10L, "Street" ) );
					session.persist( person );
				}
		);
		inTransaction(
				session -> {
					Person person = session.find( Person.class, 1L );
					person.getAddress().setStreet( "new Street" );
				}
		);
		inTransaction(
				session -> {
					Address address = session.find( Address.class, 10L );
					assertThat( address.getStreet() ).isEqualTo( "new Street" );
				}
		);
	}

	@Test
	public void testUpdate2() {
		inTransaction(
				session -> {
					Person person = new Person( 1L, "Name", new Address( 10L, null ) );
					session.persist( person );
				}
		);

		SQLStatementInspector statementInspector = statementInspector();
		statementInspector.clear();

		inTransaction(
				session -> {
					Address address = session.find( Address.class, 10L );
					address.setStreet( "new Street" );
				}
		);

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 2 );

		statementInspector.assertIsUpdate( 1 );
		String updateQuery = sqlQueries.get( 1 );
		updateQuery.contains( "where id=? and street is null" );

		inTransaction(
				session -> {
					Address address = session.find( Address.class, 10L );
					assertThat( address.getStreet() ).isEqualTo( "new Street" );
				}
		);
	}

	@Test
	public void testUpdate3() {
		inTransaction(
				session -> {
					Person person = new Person( 1L, "Name", new Address( 10L, null ) );
					session.persist( person );
				}
		);

		SQLStatementInspector statementInspector = statementInspector();
		statementInspector.clear();
		inTransaction(
				session -> {
					Address address = session.find( Address.class, 10L );
					address.setStreet( "new Street" );
				}
		);

		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertThat( sqlQueries.size() ).isEqualTo( 2 );

		statementInspector.assertIsUpdate( 1 );
		String updateQuery = sqlQueries.get( 1 );
		updateQuery.contains( "where id=? and street=?" );

		inTransaction(
				session -> {
					Address address = session.find( Address.class, 10L );
					assertThat( address.getStreet() ).isEqualTo( "new Street" );
				}
		);
	}

	@Entity(name = "Person")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class Person {

		@Id
		Long id;

		@Column
		String name;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		Address address;

		public Person() {
		}

		public Person(Long id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Entity(name = "Address")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class Address {
		@Id
		Long id;

		@Column
		String street;

		public Address() {
		}

		public Address(Long id, String street) {
			this.id = id;
			this.street = street;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}
	}
}
