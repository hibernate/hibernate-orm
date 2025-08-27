/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;


@Jpa(
		annotatedClasses = {
				SingleTableInheritanceLazyAssociationTest.Address.class,
				SingleTableInheritanceLazyAssociationTest.AddressA.class,
				SingleTableInheritanceLazyAssociationTest.AddressB.class,
				SingleTableInheritanceLazyAssociationTest.User.class,
				SingleTableInheritanceLazyAssociationTest.UserA.class,
				SingleTableInheritanceLazyAssociationTest.UserB.class,
				SingleTableInheritanceLazyAssociationTest.Message.class,
		}
)
@JiraKey(value = "HHH-15969")
public class SingleTableInheritanceLazyAssociationTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					// Create Object of Type A
					AddressA addressA1 = new AddressA( "A1" );
					UserA userA1 = new UserA( "1" );
					addressA1.setUserA( userA1 );
					Message messageA1 = new Message( "MA1", addressA1 );
					entityManager.persist( messageA1 );

					// Create Object of Type B
					AddressB addressB1 = new AddressB( "B1" );
					UserB userB1 = new UserB( "2" );
					addressB1.setUserB( userB1 );
					Message messageB1 = new Message( "MB1", addressB1 );
					entityManager.persist( messageB1 );
				}
		);
	}

	@Test
	public void testQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Query selectUFromUserU = entityManager.createQuery( "select u from Message u" );
					List<Message> resultList = selectUFromUserU.getResultList();

					for ( Message message : resultList ) {
						Address address = message.getAddress();
						assertThat( Hibernate.isInitialized( address ) ).isFalse();
						address.getId();
						assertThat( Hibernate.isInitialized( address ) ).isTrue();
						User user = address.getUser();
						assertThat( Hibernate.isInitialized( user ) ).isTrue();
						assertThat( Hibernate.isInitialized( user.getAddress() ) ).isTrue();
					}
				}
		);
	}

	@Entity(name = "Message")
	@Table(name = "T_MESSAGE")
	public static class Message {

		@Id
		private final String messageId;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@JoinColumn(name = "SENDER_ADDRESS_ID")
		private final Address address;

		private int version;

		protected Message() {
			this.messageId = null;
			this.address = null;
		}

		public Message(String messageId, Address address) {
			this.messageId = messageId;
			this.address = address;
		}

		public String getId() {
			return this.messageId;
		}

		public Address getAddress() {
			return address;
		}
	}

	@Entity(name = "Address")
	@Table(name = "T_ADDRESS")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "USER_TYPE", discriminatorType = DiscriminatorType.STRING)
	public static abstract class Address {

		@Id
		private String addressId;

		private int version;

		protected Address() {
		}

		public Address(String addressId) {
			this.addressId = addressId;
		}

		public String getId() {
			return this.addressId;
		}

		public abstract User getUser();
	}

	@Entity(name = "AddressA")
	@DiscriminatorValue("ADDRESS_A")
	public static class AddressA extends Address {

		protected AddressA() {
		}

		public AddressA(String addressId) {
			super( addressId );
		}

		@Override
		public User getUser() {
			return this.userA;
		}

		@OneToOne(mappedBy = "addressA", cascade = CascadeType.ALL)
		private UserA userA;

		public void setUserA(UserA userA) {
			this.userA = userA;
			userA.setAddressA( this );
		}
	}

	@Entity(name = "AddressB")
	@DiscriminatorValue("ADDRESS_B")
	public static class AddressB extends Address {

		@OneToOne(mappedBy = "addressB", cascade = CascadeType.ALL)
		private UserB userB;

		protected AddressB() {
		}

		public AddressB(String addressId) {
			super( addressId );
		}

		public void setUserB(UserB userB) {
			this.userB = userB;
			userB.setAddressB( this );
		}

		@Override
		public User getUser() {
			return this.userB;
		}
	}

	@Entity(name = "User")
	@Table(name = "T_USER")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "USER_TYPE", discriminatorType = DiscriminatorType.STRING)
	public static abstract class User {

		@Id
		private final String userId;

		@Version
		private int version;

		protected User() {
			this.userId = null;
		}

		public User(String id) {
			this.userId = id;
		}

		public String getId() {
			return this.userId;
		}

		public abstract Address getAddress();
	}

	@Entity(name = "UserA")
	@DiscriminatorValue("USER_A")
	public static class UserA extends User {

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "ADDRESS_ID")
		private AddressA addressA;

		protected UserA() {
		}

		public UserA(String userId) {
			super( userId );
		}

		@Override
		public AddressA getAddress() {
			return addressA;
		}

		public void setAddressA(AddressA addressA) {
			this.addressA = addressA;
		}
	}

	@Entity(name = "UserB")
	@DiscriminatorValue("USER_B")
	public static class UserB extends User {

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "ADDRESS_ID")
		private AddressB addressB;

		protected UserB() {
		}

		public UserB(String userId) {
			super( userId );
		}

		@Override
		public AddressB getAddress() {
			return addressB;
		}

		public void setAddressB(AddressB addressB) {
			this.addressB = addressB;
		}
	}

}
