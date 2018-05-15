/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11867")
public class UpdateTimeStampInheritanceTest extends BaseEntityManagerFunctionalTestCase {
	private static final long SLEEP_MILLIS = 25;

	private static final String customerId = "1";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class, AbstractPerson.class, Address.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( customerId );
			customer.addAddress( "address" );
			entityManager.persist( customer );
		} );
		sleep( SLEEP_MILLIS );
	}

	@Test
	public void updateParentClassProperty() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasNotUpdated( customer );
			customer.setName( "xyz" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void updateSubClassProperty() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasNotUpdated( customer );
			customer.setEmail( "xyz@" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void updateParentClassOneToOneAssociation() throws Exception {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasNotUpdated( customer );
			Address a = new Address();
			a.setStreet( "Lollard street" );
			customer.setWorkAddress( a );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void updateSubClassOnrToOneAssociation() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasNotUpdated( customer );
			Address a = new Address();
			a.setStreet( "Lollard Street" );
			customer.setHomeAddress( a );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void replaceParentClassElementCollection() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasNotUpdated( customer );
			Set<String> adresses = new HashSet<>();
			adresses.add( "another address" );
			customer.setAdresses( adresses );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void replaceSubClassElementCollection() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasNotUpdated( customer );
			Set<String> books = new HashSet<>();
			customer.setBooks( books );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt(), is( not( nullValue() ) ) );
			assertThat( customer.getModifiedAt(), is( not( nullValue() ) ) );
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void updateDetachedEntity() {

		Customer customer = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.find( Customer.class, customerId );
		} );

		assertModifiedAtWasNotUpdated( customer );

		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.unwrap( Session.class ).update( customer );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			assertModifiedAtWasUpdated( entityManager.find( Customer.class, customerId ) );
		} );
	}

	private void assertModifiedAtWasNotUpdated(Customer customer) {
		assertTrue( (customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime()) < 10 );
	}

	private void assertModifiedAtWasUpdated(Customer customer) {
		assertTrue( (customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime()) > 10 );
	}

	@Entity(name = "person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class AbstractPerson {
		@Id
		@Column(name = "id")
		private String id;

		private String name;

		@CreationTimestamp
		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "created_at", updatable = false)
		private Date createdAt;

		@UpdateTimestamp
		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "modified_at")
		private Date modifiedAt;

		@ElementCollection
		private Set<String> adresses = new HashSet<>();

		@OneToOne(cascade = CascadeType.ALL)
		private Address workAddress;


		public void setId(String id) {
			this.id = id;
		}

		public Date getCreatedAt() {
			return createdAt;
		}

		public Date getModifiedAt() {
			return modifiedAt;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void addAddress(String address) {
			this.adresses.add( address );
		}

		public void setWorkAddress(Address workAddress) {
			this.workAddress = workAddress;
		}

		public void setAdresses(Set<String> adresses) {
			this.adresses = adresses;
		}
	}

	@Entity(name = "Address")
	@Table(name = "address")
	public static class Address {
		@Id
		@GeneratedValue
		private Long id;

		private String street;

		public void setStreet(String street) {
			this.street = street;
		}
	}

	@Entity(name = "Customer")
	@Table(name = "customer")
	public static class Customer extends AbstractPerson {
		private String email;

		@ElementCollection
		private Set<String> books = new HashSet<>();

		@OneToOne(cascade = CascadeType.ALL)
		private Address homeAddress;

		public void setEmail(String email) {
			this.email = email;
		}

		public void addBook(String book) {
			this.books.add( book );
		}

		public void setHomeAddress(Address homeAddress) {
			this.homeAddress = homeAddress;
		}

		public void setBooks(Set<String> books) {
			this.books = books;
		}
	}
}
