/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.Skip;
import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jonathan Bregler
 */
public class EntityWithUnusualTableNameJoinTest extends EntityJoinTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ FinancialRecord.class, User.class, Customer.class, Account.class };
	}

	@Override
	@Before
	public void prepare() {
		createTestData();
	}

	@Override
	@After
	public void cleanup() {
		deleteTestData();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11816")
	public void testInnerEntityJoinsWithVariable() {
		doInHibernate( this::sessionFactory, session -> {

			// this should get financial records which have a lastUpdateBy user set
			List<Object[]> result = session.createQuery(
					"select r.id, c.name, u.id, u.username " +
							"from FinancialRecord r " +
							"   inner join r.customer c " +
							"	inner join User u on r.lastUpdateBy = u.username and u.username=:username" )
					.setParameter( "username", "steve" ).list();

			assertThat( Integer.valueOf( result.size() ), is( Integer.valueOf( 1 ) ) );
			Object[] steveAndAcme = result.get( 0 );
			assertThat( steveAndAcme[0], is( Integer.valueOf( 1 ) ) );
			assertThat( steveAndAcme[1], is( "Acme" ) );
			assertThat( steveAndAcme[3], is( "steve" ) );

		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11816")
	public void testInnerEntityJoinsWithVariableSingleQuoted() {
		doInHibernate( this::sessionFactory, session -> {

			// this should get financial records which have a lastUpdateBy user set
			List<Object[]> result = session.createQuery(
					"select r.id, c.name, a.id, a.accountname, r.lastUpdateBy " +
							"from FinancialRecord r " +
							"   inner join r.customer c " +
							"	inner join Account a on a.customer = c and a.accountname!='test:account' and a.accountname=:accountname and r.lastUpdateBy != null" )
					.setParameter( "accountname", "DEBIT" ).list();

			assertThat( Integer.valueOf( result.size() ), is( Integer.valueOf( 1 ) ) );
			Object[] steveAndAcmeAndDebit = result.get( 0 );
			assertThat( steveAndAcmeAndDebit[0], is( Integer.valueOf( 1 ) ) );
			assertThat( steveAndAcmeAndDebit[1], is( "Acme" ) );
			assertThat( steveAndAcmeAndDebit[3], is( "DEBIT" ) );
			assertThat( steveAndAcmeAndDebit[4], is( "steve" ) );
		} );
	}

	@Override
	@Skip(message = "The superclass test checks for the table name which is different in this test case and causes the test to fail", condition = Skip.AlwaysSkip.class)
	public void testNoImpliedJoinGeneratedForEqualityComparison() {
	}

	private void createTestData() {
		doInHibernate( this::sessionFactory, session -> {

			final Customer customer = new Customer( Integer.valueOf( 1 ), "Acme" );
			session.save( customer );
			session.save( new User( Integer.valueOf( 1 ), "steve", customer ) );
			session.save( new User( Integer.valueOf( 2 ), "jane" ) );
			session.save( new FinancialRecord( Integer.valueOf( 1 ), customer, "steve" ) );
			session.save( new FinancialRecord( Integer.valueOf( 2 ), customer, null ) );
			session.save( new Account( Integer.valueOf( 1 ), "DEBIT", customer ) );
			session.save( new Account( Integer.valueOf( 2 ), "CREDIT" ) );
		} );
	}

	private void deleteTestData() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete FinancialRecord" ).executeUpdate();
			session.createQuery( "delete User" ).executeUpdate();
			session.createQuery( "delete Account" ).executeUpdate();
			session.createQuery( "delete Customer" ).executeUpdate();

		} );
	}

	@Entity(name = "Customer")
	@Table(name = "`my::customer`")
	public static class Customer {

		private Integer id;
		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "FinancialRecord")
	@Table(name = "`financial?record`")
	public static class FinancialRecord {

		private Integer id;
		private Customer customer;
		private String lastUpdateBy;

		public FinancialRecord() {
		}

		public FinancialRecord(Integer id, Customer customer, String lastUpdateBy) {
			this.id = id;
			this.customer = customer;
			this.lastUpdateBy = lastUpdateBy;
		}

		@Id
		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne
		@JoinColumn
		public Customer getCustomer() {
			return this.customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public String getLastUpdateBy() {
			return this.lastUpdateBy;
		}

		public void setLastUpdateBy(String lastUpdateBy) {
			this.lastUpdateBy = lastUpdateBy;
		}
	}

	@Entity(name = "User")
	@Table(name = "`my::user`")
	public static class User {

		private Integer id;
		private String username;
		private Customer customer;

		public User() {
		}

		public User(Integer id, String username) {
			this.id = id;
			this.username = username;
		}

		public User(Integer id, String username, Customer customer) {
			this.id = id;
			this.username = username;
			this.customer = customer;
		}

		@Id
		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@NaturalId
		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public Customer getCustomer() {
			return this.customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	@Entity(name = "Account")
	@Table(name = "`account`")
	public static class Account {

		private Integer id;
		private String accountname;
		private Customer customer;

		public Account() {
		}

		public Account(Integer id, String accountname) {
			this.id = id;
			this.accountname = accountname;
		}

		public Account(Integer id, String accountname, Customer customer) {
			this.id = id;
			this.accountname = accountname;
			this.customer = customer;
		}

		@Id
		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@NaturalId
		public String getAccountname() {
			return this.accountname;
		}

		public void setAccountname(String accountname) {
			this.accountname = accountname;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public Customer getCustomer() {
			return this.customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

}
