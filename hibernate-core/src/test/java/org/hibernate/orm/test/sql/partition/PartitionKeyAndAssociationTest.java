/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.partition;

import org.hibernate.annotations.PartitionKey;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		PartitionKeyAndAssociationTest.SalesContact.class,
		PartitionKeyAndAssociationTest.ContactEmail.class,
		PartitionKeyAndAssociationTest.ContactAddress.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16849" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16942" )
public class PartitionKeyAndAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new SalesContact( 1L, 1L, "name_1" ) );
			session.persist( new SalesContact( 2L, 2L, "name_2" ) );
			session.persist( new ContactAddress( 1L, "address_1", 1L, null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ContactAddress" ).executeUpdate() );
		scope.inTransaction( session -> session.createMutationQuery( "delete from SalesContact" ).executeUpdate() );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SalesContact contact = session.find( SalesContact.class, 1L );
			contact.setName( "updated_name" );
		} );
		scope.inTransaction( session -> assertThat( session.find(
				SalesContact.class,
				1L
		).getName() ).isEqualTo( "updated_name" ) );
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final SalesContact contact = session.find( SalesContact.class, 2L );
			session.remove( contact );
		} );
		scope.inTransaction( session -> assertThat( session.find(
				SalesContact.class,
				2L
		) ).isNull() );
	}

	@Test
	public void testNullPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ContactAddress address = session.find( ContactAddress.class, 1L );
			assertThat( address.contact ).isNull();
		} );
	}

	@Entity( name = "SalesContact" )
	public static class SalesContact {
		@Id
		private Long id;

		@PartitionKey
		private Long accountId;

		@OneToOne( mappedBy = "contact", cascade = CascadeType.ALL )
		private ContactAddress contactAddress;

		private String name;

		public SalesContact() {
		}

		public SalesContact(Long id, Long accountId, String name) {
			this.id = id;
			this.accountId = accountId;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "ContactEmail" )
	public static class ContactEmail {
		@Id
		private Long id;

		private String email;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "account_id", referencedColumnName = "accountId", nullable = false )
		@JoinColumn( name = "contact_id", referencedColumnName = "id", nullable = false )
		private SalesContact contact;
	}

	@Entity( name = "ContactAddress" )
	public static class ContactAddress {
		@Id
		private Long id;

		private String address;

		@Column(name = "account_id")
		private Long accountId;
		@Column(name = "contact_id")
		private Long contactId;
		@OneToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "account_id", referencedColumnName = "accountId", insertable = false, updatable = false )
		@JoinColumn( name = "contact_id", referencedColumnName = "id", insertable = false, updatable = false )
		private SalesContact contact;

		public ContactAddress() {
		}

		public ContactAddress(Long id, String address, Long accountId, Long contactId) {
			this.id = id;
			this.address = address;
			this.accountId = accountId;
			this.contactId = contactId;
		}
	}
}
