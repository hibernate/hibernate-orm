/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazyload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Oleksander Dukhno
 */
@DomainModel(
		annotatedClasses = {
				LazyLoadingTest.Client.class,
				LazyLoadingTest.Address.class,
				LazyLoadingTest.Account.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"),
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "false"),
		}
)
public class LazyLoadingTest {

	@Test
	@JiraKey(value = "HHH-11838")
	public void testGetIdOneToOne(SessionFactoryScope scope) {
		final Object clientId = scope.fromTransaction(
				session -> {
					Address address = new Address();
					session.persist( address );
					Client client = new Client( address );
					session.persist( client );
					return client.getId();
				}
		);

		final Long addressId = scope.fromTransaction(
				session -> {
					Client client = session.get( Client.class, clientId );
					Address address = client.getAddress();
					address.getId();
					assertThat( Hibernate.isInitialized( address ), is( true ) );
					address.getStreet();
					assertThat( Hibernate.isInitialized( address ), is( true ) );
					return address.getId();
				}
		);

		scope.inTransaction( session -> {
			Address address = session.get( Address.class, addressId );
			Client client = address.getClient();
			client.getId();
			assertThat( Hibernate.isInitialized( client ), is( false ) );
			client.getName();
			assertThat( Hibernate.isInitialized( client ), is( true ) );
		} );
	}

	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Test
	@JiraKey(value = "HHH-11838")
	public void testGetIdManyToOne(SessionFactoryScope scope) {
		Serializable accountId = scope.fromTransaction( session -> {
			Address address = new Address();
			session.persist( address );
			Client client = new Client( address );
			Account account = new Account();
			client.addAccount( account );
			session.persist( account );
			session.persist( client );
			return account.getId();
		} );

		scope.inTransaction(
				session -> {
					Account account = session.getReference( Account.class, accountId );
					Client client = account.getClient();
					client.getId();
					assertThat( Hibernate.isInitialized( client ), is( false ) );
					client.getName();
					assertThat( Hibernate.isInitialized( client ), is( true ) );
				} );

	}

	@Entity(name = "Account")
	public static class Account {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "id_client")
		private Client client;

		public Client getClient() {
			return client;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setClient(Client client) {
			this.client = client;
		}
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		@GeneratedValue
		private Long id;

		@Column
		private String street;

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "id_client")
		private Client client;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}
	}

	@Entity(name = "Client")
	public static class Client {
		@Id
		@GeneratedValue
		private Long id;

		@Column
		private String name;

		@OneToMany(mappedBy = "client")
		private List<Account> accounts = new ArrayList<>();

		@OneToOne(mappedBy = "client", fetch = FetchType.LAZY)
		private Address address;

		public Client() {
		}

		public Client(Address address) {
			this.address = address;
			address.setClient( this );
		}

		public void addAccount(Account account) {
			accounts.add( account );
			account.setClient( this );
		}

		public String getName() {
			return name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}
}
