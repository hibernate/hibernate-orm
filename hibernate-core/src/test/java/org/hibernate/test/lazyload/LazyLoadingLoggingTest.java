/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.LazyInitializationException;
import org.hibernate.internal.AbstractSharedSessionContract;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class LazyLoadingLoggingTest
		extends BaseCoreFunctionalTestCase {

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Client.class,
				Address.class
		};
	}

	@Override
	protected void afterSessionFactoryBuilt() {
		doInHibernate( this::sessionFactory, session -> {
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

	@Test
	@TestForIssue(jiraKey = "HHH-12484")
	public void testNoSession() {
		Address address = doInHibernate( this::sessionFactory, s -> {
			return s.load( Address.class, 1L );
		} );

		try {
			address.getClient().getName();
			fail( "Should throw LazyInitializationException" );
		}
		catch (LazyInitializationException expected) {
			assertEquals(
					"could not initialize proxy " +
							"[org.hibernate.test.lazyload.LazyLoadingLoggingTest$Address#1] " +
							"- no Session",
					expected.getMessage()
			);
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12484")
	public void testDisconnect() {
		doInHibernate( this::sessionFactory, session -> {
			Address address = session.load( Address.class, 1L );
			AbstractSharedSessionContract sessionContract = (AbstractSharedSessionContract) session;
			sessionContract.getJdbcCoordinator().close();

			try {
				address.getClient().getName();
				fail( "Should throw LazyInitializationException" );
			}
			catch (LazyInitializationException expected) {
				assertEquals(
						"could not initialize proxy " +
								"[org.hibernate.test.lazyload.LazyLoadingLoggingTest$Address#1] " +
								"- the owning Session is disconnected",
						expected.getMessage()
				);
			}
			session.getTransaction().markRollbackOnly();
		} );
	}

	protected boolean rebuildSessionFactoryOnError() {
		return false;
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