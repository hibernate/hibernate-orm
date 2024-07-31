/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Oleksander Dukhno
 */
@DomainModel(
		annotatedClasses = {
				Parent.class,
				Child.class,
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

	private static final int CHILDREN_SIZE = 3;
	private Long parentID;
	private Long lastChildID;

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent();
					for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
						final Child child = p.makeChild();
						session.persist( child );
						lastChildID = child.getId();
					}
					session.persist( p );
					parentID = p.getId();

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-7971")
	public void testLazyCollectionLoadingAfterEndTransaction(SessionFactoryScope scope) {
		Parent loadedParent = scope.fromTransaction(
				session ->
						session.load( Parent.class, parentID )
		);

		assertFalse( Hibernate.isInitialized( loadedParent.getChildren() ) );

		int i = 0;
		for ( Child child : loadedParent.getChildren() ) {
			i++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, i );

		Child loadedChild = scope.fromTransaction(
				sesison ->
						sesison.load( Child.class, lastChildID )
		);

		Parent p = loadedChild.getParent();
		int j = 0;
		for ( Child child : p.getChildren() ) {
			j++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, j );
	}

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
					Account account = session.load( Account.class, accountId );
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
