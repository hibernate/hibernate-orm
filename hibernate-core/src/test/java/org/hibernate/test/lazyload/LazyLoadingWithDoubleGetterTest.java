/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Vlad Mihalcea
 */
public class LazyLoadingWithDoubleGetterTest
		extends BaseCoreFunctionalTestCase {

	private static final int CHILDREN_SIZE = 3;
	private Long parentID;
	private Long lastChildID;

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}


	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class,
				Client.class,
				Address.class,
				Account.class
		};
	}

	protected void prepareTest() {
		doInHibernate( this::sessionFactory, s -> {
			Parent p = new Parent();
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				final Child child = p.makeChild();
				s.persist( child );
				lastChildID = child.getId();
			}
			s.persist( p );
			parentID = p.getId();
		} );
	}

	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11838")
	public void test() {
		Serializable accountId = doInHibernate( this::sessionFactory, s -> {
			Address address = new Address();
			s.save( address );

			Client client = new Client( address );

			Account account = new Account();
			client.addAccount( account );

			s.save( account );
			s.save( client );

			return account.getId();
		} );

		doInHibernate( this::sessionFactory, s -> {
			Account account = s.load( Account.class, accountId );
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

		private boolean active;

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

		public boolean isName() {
			return name != null;
		}

		public boolean isActive() {
			return active;
		}

		public String getActive() {
			return String.valueOf( active );
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
