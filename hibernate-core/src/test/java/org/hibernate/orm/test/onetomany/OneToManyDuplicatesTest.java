/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {
		OneToManyDuplicatesTest.UserContact.class,
		OneToManyDuplicatesTest.ContactInfo.class
})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@JiraKey( "HHH-14078" )
public class OneToManyDuplicatesTest {

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ContactInfo" ).executeUpdate();
			session.createMutationQuery( "delete from UserContact" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		Long kevinId = scope.fromTransaction( session -> {
			UserContact userContact = new UserContact();
			userContact.setName( "Kevin" );
			session.persist( userContact );
			return userContact.getId();
		} );
		scope.inTransaction( session -> {
			UserContact userContact = session.find( UserContact.class, kevinId );
			ContactInfo contactInfo = new ContactInfo();
			contactInfo.setPhoneNumber( "123" );

			contactInfo.setUserContact( userContact );
			userContact.getContactInfos().add( contactInfo );
			session.merge( userContact );

			assertEquals( 1, userContact.getContactInfos().size() );
		} );

		scope.inTransaction( session -> {
			UserContact userContact = session.find( UserContact.class, 1L );
			assertEquals( 1, userContact.getContactInfos().size() );
		});
	}

	@Entity(name = "UserContact")
	public static class UserContact {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "userContact")
		private List<ContactInfo> contactInfos = new ArrayList<>();

		public UserContact() {
		}

		public UserContact(Long id, String name) {
			this.id = id;
			this.name = name;
		}

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

		public List<ContactInfo> getContactInfos() {
			return contactInfos;
		}

		public void setContactInfos(List<ContactInfo> contactInfos) {
			this.contactInfos = contactInfos;
		}
	}

	@Entity(name = "ContactInfo")
	public static class ContactInfo {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String phoneNumber;

		private String address;

		@ManyToOne
		private UserContact userContact;

		public ContactInfo() {
		}

		public ContactInfo(Long id, String phoneNumber, String address, UserContact userContact) {
			this.id = id;
			this.phoneNumber = phoneNumber;
			this.address = address;
			this.userContact = userContact;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getPhoneNumber() {
			return phoneNumber;
		}

		public void setPhoneNumber(String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public UserContact getUserContact() {
			return userContact;
		}

		public void setUserContact(UserContact userContact) {
			this.userContact = userContact;
		}
	}
}
