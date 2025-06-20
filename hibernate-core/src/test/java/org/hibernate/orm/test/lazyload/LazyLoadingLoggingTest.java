/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazyload;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.LazyInitializationException;
import org.hibernate.internal.AbstractSharedSessionContract;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				LazyLoadingLoggingTest.Client.class,
				LazyLoadingLoggingTest.Address.class
		}
)
@SessionFactory
public class LazyLoadingLoggingTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Address address = new Address();
			address.setId( 1L );
			address.setStreet( "Marea albastra" );
			session.persist( address );

			Client client = new Client();
			client.setId( 1L );
			client.setName( "Dorian" );
			client.setAddress( address );
			session.persist( client );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12484")
	public void testNoSession(SessionFactoryScope scope) {
		Address address = scope.fromTransaction(
				session ->
						session.getReference( Address.class, 1L )
		);

		try {
			address.getClient().getName();
			fail( "Should throw LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {
			assertEquals(
					"Could not initialize proxy " +
							"[org.hibernate.orm.test.lazyload.LazyLoadingLoggingTest$Address#1] " +
							"- no session",
					expected.getMessage()
			);
		}
	}

	@Test
	@JiraKey(value = "HHH-12484")
	public void testDisconnect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Address address = session.getReference( Address.class, 1L );
					AbstractSharedSessionContract sessionContract = (AbstractSharedSessionContract) session;
					sessionContract.getJdbcCoordinator().close();

					try {
						address.getClient().getName();
						fail( "Should throw LazyInitializationException" );
					}
					catch (LazyInitializationException expected) {
						assertEquals(
								"Could not initialize proxy " +
										"[org.hibernate.orm.test.lazyload.LazyLoadingLoggingTest$Address#1] " +
										"- the owning session is disconnected",
								expected.getMessage()
						);
					}
					session.getTransaction().markRollbackOnly();
				} );
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
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
		private Long id;

		@Column
		private String name;

		@OneToOne(mappedBy = "client", fetch = FetchType.LAZY)
		private Address address;

		public Client() {
		}

		public void setName(String name) {
			this.name = name;
		}

		public Client(Address address) {
			this.address = address;
			address.setClient( this );
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
