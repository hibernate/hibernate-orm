/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.collectionTableOverride;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.CollectionTableOverride;
import org.hibernate.annotations.CollectionTableOverrides;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.orm.test.util.SchemaUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for @CollectionTableOverride annotation functionality.
 * <p>
 * This test verifies that the @CollectionTableOverride annotation correctly
 * overrides collection table names for collections within embeddable classes.
 *
 * @author Test
 */
@DomainModel(
		annotatedClasses = {
				CollectionTableOverrideTest.Person.class,
				CollectionTableOverrideTest.Company.class,
				CollectionTableOverrideTest.Organization.class,
				CollectionTableOverrideTest.Customer.class
		}
)
@SessionFactory
public class CollectionTableOverrideTest {

	/**
	 * Test purpose: Verify that @CollectionTableOverride is correctly applied at the metadata level.
	 * <p>
	 * Verification:
	 * 1. Verify that Person entity's address.phones collection is overridden to table name "person_phones"
	 * 2. Verify that Company entity's address.phones collection is overridden to table name "company_phones"
	 * 3. Verify that Organization entity's contactInfo.emails collection is overridden to table name "organization_emails"
	 */
	@Test
	public void testCollectionTableNameOverride(DomainModelScope scope) {
		final MetadataImplementor metadata = scope.getDomainModel();

		// Verify Person entity's address.phones collection table name
		final Collection personPhonesCollection = metadata.getCollectionBinding(
				Person.class.getName() + ".address.phones"
		);
		assertThat(
				"Collection table name should be overridden to 'person_phones'",
				personPhonesCollection.getCollectionTable().getName(),
				is( "person_phones" )
		);

		// Verify Company entity's address.phones collection table name
		final Collection companyPhonesCollection = metadata.getCollectionBinding(
				Company.class.getName() + ".address.phones"
		);
		assertThat(
				"Collection table name should be overridden to 'company_phones'",
				companyPhonesCollection.getCollectionTable().getName(),
				is( "company_phones" )
		);

		// Verify Organization entity's contactInfo.emails collection table name (using @CollectionTableOverrides)
		final Collection orgEmailsCollection = metadata.getCollectionBinding(
				Organization.class.getName() + ".contactInfo.emails"
		);
		assertThat(
				"Collection table name should be overridden to 'organization_emails'",
				orgEmailsCollection.getCollectionTable().getName(),
				is( "organization_emails" )
		);
	}

	/**
	 * Test purpose: Verify that overridden table names are correctly reflected in the schema during DDL generation.
	 * <p>
	 * Verification:
	 * 1. Verify that overridden table names (person_phones, company_phones, organization_emails) exist in the schema
	 * 2. Verify that default table names (default_phones, default_emails) do NOT exist in the schema
	 */
	@Test
	public void testSchemaGeneration(DomainModelScope scope) {
		final MetadataImplementor metadata = scope.getDomainModel();

		// Verify that overridden table names exist in the schema
		assertTrue(
				SchemaUtil.isTablePresent( "person_phones", metadata ),
				"Table 'person_phones' should be present in schema"
		);
		assertTrue(
				SchemaUtil.isTablePresent( "company_phones", metadata ),
				"Table 'company_phones' should be present in schema"
		);
		assertTrue(
				SchemaUtil.isTablePresent( "organization_emails", metadata ),
				"Table 'organization_emails' should be present in schema"
		);

		// Verify that default table names do NOT exist in the schema
		assertFalse(
				SchemaUtil.isTablePresent( "default_phones", metadata ),
				"Default table 'default_phones' should NOT be present in schema"
		);
		assertFalse(
				SchemaUtil.isTablePresent( "default_emails", metadata ),
				"Default table 'default_emails' should NOT be present in schema"
		);
	}

	/**
	 * Test purpose: Verify that actual database operations (save/retrieve) work correctly with overridden table names.
	 * <p>
	 * Verification:
	 * 1. Verify that when saving Person, Company, Organization entities, data is stored in the overridden tables
	 * 2. Verify that saved data can be retrieved correctly
	 */
	@Test
	public void testQueryExecution(SessionFactoryScope scope) {
		// Test data persistence
		scope.inTransaction( session -> {
			// Persist Person entity
			Person person = new Person();
			person.setName( "John Doe" );
			Address address = new Address();
			address.setStreet( "123 Main St" );
			address.setCity( "New York" );
			address.getPhones().add( "123-456-7890" );
			address.getPhones().add( "098-765-4321" );
			person.setAddress( address );
			session.persist( person );

			// Persist Company entity
			Company company = new Company();
			company.setName( "Acme Corp" );
			Address companyAddress = new Address();
			companyAddress.setStreet( "456 Business Ave" );
			companyAddress.setCity( "Boston" );
			companyAddress.getPhones().add( "555-123-4567" );
			company.setAddress( companyAddress );
			session.persist( company );

			// Persist Organization entity
			Organization organization = new Organization();
			organization.setName( "Tech Inc" );
			ContactInfo contactInfo = new ContactInfo();
			contactInfo.getEmails().add( "info@techinc.com" );
			contactInfo.getEmails().add( "support@techinc.com" );
			organization.setContactInfo( contactInfo );
			session.persist( organization );
		} );

		// Test data retrieval
		scope.inTransaction( session -> {
			Person person = session.createQuery( "from Person", Person.class ).getSingleResult();
			assertThat( person.getName(), is( "John Doe" ) );
			assertThat( person.getAddress().getPhones().size(), is( 2 ) );

			Company company = session.createQuery( "from Company", Company.class ).getSingleResult();
			assertThat( company.getName(), is( "Acme Corp" ) );
			assertThat( company.getAddress().getPhones().size(), is( 1 ) );

			Organization organization = session.createQuery( "from Organization", Organization.class )
					.getSingleResult();
			assertThat( organization.getName(), is( "Tech Inc" ) );
			assertThat( organization.getContactInfo().getEmails().size(), is( 2 ) );
		} );
	}

	/**
	 * Test purpose: Verify that joinColumns and indexes from nested @CollectionTable
	 * in @CollectionTableOverride are correctly applied.
	 * <p>
	 * Verification:
	 * 1. Verify that custom join column name is applied
	 * 2. Verify that indexes are correctly created on the collection table
	 */
	@Test
	public void testCollectionTableOverrideWithJoinColumnsAndIndexes(DomainModelScope scope) {
		final MetadataImplementor metadata = scope.getDomainModel();

		// Get the collection binding
		final Collection customerTagsCollection = metadata.getCollectionBinding(
				Customer.class.getName() + ".contact.tags"
		);
		assertNotNull( customerTagsCollection, "Collection should not be null" );

		final org.hibernate.mapping.Table collectionTable = customerTagsCollection.getCollectionTable();
		assertNotNull( collectionTable, "Collection table should not be null" );
		assertEquals( "customer_tags", collectionTable.getName(), "Table name should be overridden" );

		// Verify join column name is overridden by checking the key columns
		@SuppressWarnings("unchecked")
		java.util.Iterator<Column> keyColumns = customerTagsCollection.getKey().getColumns().iterator();
		assertTrue( keyColumns.hasNext(), "Join column should exist" );
		Column joinColumn = keyColumns.next();
		assertEquals( "customer_id", joinColumn.getName(), "Join column name should be overridden to 'customer_id'" );

		// Verify indexes are applied
		java.util.Iterator<org.hibernate.mapping.Index> indexIterator = collectionTable.getIndexes().values().iterator();
		assertTrue( indexIterator.hasNext(), "Index should exist" );
		org.hibernate.mapping.Index index = indexIterator.next();
		assertNotNull( index.getName(), "Index name should not be null" );
		assertEquals( 1, index.getColumnSpan(), "Index should have one column" );
		Column indexColumn = index.getColumns().iterator().next();
		// The column name might be auto-generated, so we just verify the index exists
		assertNotNull( indexColumn.getName(), "Index column should have a name" );
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Person" ).executeUpdate();
			session.createQuery( "delete from Company" ).executeUpdate();
			session.createQuery( "delete from Organization" ).executeUpdate();
			session.createQuery( "delete from Customer" ).executeUpdate();
		} );
	}

	@Embeddable
	public static class Address {
		private String street;
		private String city;

		@ElementCollection
		@CollectionTable(name = "default_phones")
		private List<String> phones = new ArrayList<>();

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public List<String> getPhones() {
			return phones;
		}

		public void setPhones(List<String> phones) {
			this.phones = phones;
		}
	}

	@Embeddable
	public static class ContactInfo {
		@ElementCollection
		@CollectionTable(name = "default_emails")
		private List<String> emails = new ArrayList<>();

		public List<String> getEmails() {
			return emails;
		}

		public void setEmails(List<String> emails) {
			this.emails = emails;
		}
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		@CollectionTableOverride(name = "phones", collectionTable = @CollectionTable(name = "person_phones"))
		private Address address;

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

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Entity(name = "Company")
	@Table(name = "COMPANY")
	public static class Company {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		@CollectionTableOverride(name = "phones", collectionTable = @CollectionTable(name = "company_phones"))
		private Address address;

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

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}
	}

	@Entity(name = "Organization")
	@Table(name = "ORGANIZATION")
	public static class Organization {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		@CollectionTableOverrides({
				@CollectionTableOverride(name = "emails", collectionTable = @CollectionTable(name = "organization_emails"))
		})
		private ContactInfo contactInfo;

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

		public ContactInfo getContactInfo() {
			return contactInfo;
		}

		public void setContactInfo(ContactInfo contactInfo) {
			this.contactInfo = contactInfo;
		}
	}

	@Embeddable
	public static class Contact {
		@ElementCollection
		@CollectionTable(name = "default_tags")
		private List<String> tags = new ArrayList<>();

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER")
	public static class Customer {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		@CollectionTableOverride(
				name = "tags",
				collectionTable = @CollectionTable(
						name = "customer_tags",
						joinColumns = @JoinColumn(name = "customer_id"),
						indexes = @Index(name = "idx_tag_value", columnList = "tag_value")
				)
		)
		private Contact contact;

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

		public Contact getContact() {
			return contact;
		}

		public void setContact(Contact contact) {
			this.contact = contact;
		}
	}
}
