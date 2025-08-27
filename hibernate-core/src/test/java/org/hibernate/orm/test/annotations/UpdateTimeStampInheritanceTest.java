/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.internal.CurrentTimestampGeneration;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey("HHH-11867")
@Jpa(
		annotatedClasses = {
				UpdateTimeStampInheritanceTest.Customer.class,
				UpdateTimeStampInheritanceTest.AbstractPerson.class,
				UpdateTimeStampInheritanceTest.Address.class
		},
		settingProviders = @SettingProvider(settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME, provider = UpdateTimeStampInheritanceTest.ClockProvider.class)
)
public class UpdateTimeStampInheritanceTest {
	private static final String customerId = "1";
	private static final MutableClock clock = new MutableClock();

	public static class ClockProvider implements SettingProvider.Provider<MutableClock> {

		@Override
		public MutableClock getSetting() {
			return clock;
		}
	}

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		clock.reset();
		scope.inTransaction( entityManager -> {
			Customer customer = new Customer();
			customer.setId( customerId );
			customer.addAddress( "address" );
			entityManager.persist( customer );
		} );
		clock.tick();
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void updateParentClassProperty(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasNotUpdated( customer );
			customer.setName( "xyz" );
		} );
		clock.tick();

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void updateSubClassProperty(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasNotUpdated( customer );
			customer.setEmail( "xyz@" );
		} );
		clock.tick();

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void updateParentClassOneToOneAssociation(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasNotUpdated( customer );
			Address a = new Address();
			a.setStreet( "Lollard street" );
			customer.setWorkAddress( a );
		} );
		clock.tick();

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void updateSubClassOnrToOneAssociation(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasNotUpdated( customer );
			Address a = new Address();
			a.setStreet( "Lollard Street" );
			customer.setHomeAddress( a );
		} );
		clock.tick();

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void replaceParentClassElementCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasNotUpdated( customer );
			Set<String> adresses = new HashSet<>();
			adresses.add( "another address" );
			customer.setAdresses( adresses );
		} );
		clock.tick();

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void replaceSubClassElementCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() ).isNotNull();
			assertThat( customer.getModifiedAt() ).isNotNull();
			assertModifiedAtWasNotUpdated( customer );
			Set<String> books = new HashSet<>();
			customer.setBooks( books );
		} );
		clock.tick();

		scope.inTransaction( entityManager -> {
			Customer customer = entityManager.find( Customer.class, customerId );
			assertThat( customer.getCreatedAt() );
			assertThat( customer.getModifiedAt() );
			assertModifiedAtWasUpdated( customer );
		} );
	}

	@Test
	public void mergeDetachedEntity(EntityManagerFactoryScope scope) {

		Customer customer = scope.fromTransaction(
				entityManager ->
						entityManager.find( Customer.class, customerId )
		);

		assertModifiedAtWasNotUpdated( customer );

		scope.inTransaction( entityManager -> {
			entityManager.unwrap( Session.class ).merge( customer );
		} );

		scope.inTransaction( entityManager -> {
			assertModifiedAtWasNotUpdated( entityManager.find( Customer.class, customerId ) );
		} );
	}

	private void assertModifiedAtWasNotUpdated(Customer customer) {
		assertThat( ( customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime() ) ).isLessThan( 10 );
	}

	private void assertModifiedAtWasUpdated(Customer customer) {
		assertThat( ( customer.getModifiedAt().getTime() - customer.getCreatedAt().getTime() ) ).isGreaterThan( 10 );
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
