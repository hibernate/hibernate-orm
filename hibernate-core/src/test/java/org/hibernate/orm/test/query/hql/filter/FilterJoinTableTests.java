/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				FilterJoinTableTests.Client.class,
				FilterJoinTableTests.Account.class
		}
)
@SessionFactory
public class FilterJoinTableTests {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Client client = new Client()
					.setId( 1L )
					.setName( "John Doe" );

			client.addAccount(
					new Account()
							.setId( 1L )
							.setType( AccountType.CREDIT )
							.setAmount( 5000d )
							.setRate( 1.25 / 100 )
			);

			client.addAccount(
					new Account()
							.setId( 2L )
							.setType( AccountType.DEBIT )
							.setAmount( 0d )
							.setRate( 1.05 / 100 )
			);

			client.addAccount(
					new Account()
							.setType( AccountType.DEBIT )
							.setId( 3L )
							.setAmount( 250d )
							.setRate( 1.05 / 100 )
			);

			session.persist( client );
		} );
	}

	@Test
	void testFilterJoinableOnCollectionField(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "firstAccounts" ).setParameter( "maxOrderId", 1 );
			final Client client = session.createQuery( "select c from Client c where c.id = :id", Client.class )
					.setParameter( "id", 1L ).uniqueResult();
			assertThat( client.getAccounts().stream().map( Account::getId ).collect( Collectors.toSet() ),
						equalTo( new HashSet<>( Arrays.asList( 1L, 2L ) ) ) );
		} );
	}

	public enum AccountType {
		DEBIT,
		CREDIT
	}

	@Entity(name = "Client")
	@FilterDef(
			name="firstAccounts",
			parameters=@ParamDef(
					name="maxOrderId",
					type="int"
			)
	)
	public static class Client {

		@Id
		private Long id;

		private String name;

		@ManyToMany(cascade = CascadeType.ALL)
		@JoinTable
		@OrderColumn(name = "order_id")
		@FilterJoinTable(
				name="firstAccounts",
				condition="order_id <= :maxOrderId"
		)
		private List<Account> accounts = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public Client setId(Long id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public Client setName(String name) {
			this.name = name;
			return this;
		}

		public List<Account> getAccounts() {
			return accounts;
		}

		public void addAccount(Account account) {
			this.accounts.add( account );
		}
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

		public Long getId() {
			return id;
		}

		public Account setId(Long id) {
			this.id = id;
			return this;
		}

		public AccountType getType() {
			return type;
		}

		public Account setType(AccountType type) {
			this.type = type;
			return this;
		}

		public Double getAmount() {
			return amount;
		}

		public Account setAmount(Double amount) {
			this.amount = amount;
			return this;
		}

		public Double getRate() {
			return rate;
		}

		public Account setRate(Double rate) {
			this.rate = rate;
			return this;
		}
	}

}
