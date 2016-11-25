/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ParamDef;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FilterJoinTableTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( FilterJoinTableTest.class );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Client.class,
			Account.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::mapping-filter-join-table-persistence-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {

			Client client = new Client();
			client.setId( 1L );
			client.setName( "John Doe" );
			entityManager.persist( client );

			Account account1 = new Account( );
			account1.setId( 1L );
			account1.setType( AccountType.CREDIT );
			account1.setAmount( 5000d );
			account1.setRate( 1.25 / 100 );
			account1.setActive( true );
			client.getAccounts().add( account1 );
			entityManager.persist( account1 );

			Account account2 = new Account( );
			account2.setId( 2L );
			account2.setType( AccountType.DEBIT );
			account2.setAmount( 0d );
			account2.setRate( 1.05 / 100 );
			account2.setActive( false );
			client.getAccounts().add( account2 );
			entityManager.persist( account2 );

			Account account3 = new Account( );
			account3.setType( AccountType.DEBIT );
			account3.setId( 3L );
			account3.setAmount( 250d );
			account3.setRate( 1.05 / 100 );
			account3.setActive( true );
			client.getAccounts().add( account3 );
			entityManager.persist( account3 );
		} );
		//end::mapping-filter-join-table-persistence-example[]

		//tag::mapping-filter-join-table-collection-query-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			Client client = entityManager.find( Client.class, 1L );
			assertEquals( 3, client.getAccounts().size());
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.infof( "Activate filter [%s]", "firstAccounts");

			Client client = entityManager.find( Client.class, 1L );

			entityManager
				.unwrap( Session.class )
				.enableFilter( "firstAccounts" )
				.setParameter( "maxOrderId", 1);

			assertEquals( 2, client.getAccounts().size());
		} );
		//end::mapping-filter-join-table-collection-query-example[]
	}

	//tag::mapping-filter-join-table-example[]
	public enum AccountType {
		DEBIT,
		CREDIT
	}

	@Entity(name = "Client")
	@FilterDef(name="firstAccounts", parameters=@ParamDef( name="maxOrderId", type="int" ) )
	@Filter(name="firstAccounts", condition="order_id <= :maxOrderId")
	public static class Client {

		@Id
		private Long id;

		private String name;

		@OneToMany
		@OrderColumn(name = "order_id")
		@FilterJoinTable(name="firstAccounts", condition="order_id <= :maxOrderId")
		private List<Account> accounts = new ArrayList<>( );

		//Getters and setters omitted for brevity

		//end::mapping-filter-join-table-example[]
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

		public List<Account> getAccounts() {
			return accounts;
		}
		//tag::mapping-filter-join-table-example[]
	}

	@Entity(name = "Account")
	public static class Account {

		@Id
		private Long id;

		@Column(name = "account_type")
		@Enumerated(EnumType.STRING)
		private AccountType type;

		private Double amount;

		private Double rate;

		private boolean active;

		//Getters and setters omitted for brevity

	//end::mapping-filter-join-table-example[]
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public AccountType getType() {
			return type;
		}

		public void setType(AccountType type) {
			this.type = type;
		}

		public Double getAmount() {
			return amount;
		}

		public void setAmount(Double amount) {
			this.amount = amount;
		}

		public Double getRate() {
			return rate;
		}

		public void setRate(Double rate) {
			this.rate = rate;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		//tag::mapping-filter-join-table-example[]
	}
	//end::mapping-filter-join-table-example[]
}
