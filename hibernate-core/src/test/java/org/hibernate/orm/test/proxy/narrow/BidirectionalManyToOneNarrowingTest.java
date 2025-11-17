/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.narrow;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BidirectionalManyToOneNarrowingTest.Address.class,
		BidirectionalManyToOneNarrowingTest.MyAddress.class,
		BidirectionalManyToOneNarrowingTest.Message.class,
		BidirectionalManyToOneNarrowingTest.AddressContainer.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17594" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17665" )
public class BidirectionalManyToOneNarrowingTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyAddress address = new MyAddress( 1L );
			session.persist( address );
			session.persist( new Message( 2L, address ) );
			final AddressContainer relation = new AddressContainer();
			relation.setId( 3L );
			relation.setMyAddress( address );
			session.persist( relation );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Message" ).executeUpdate();
			session.createMutationQuery( "delete from AddressContainer" ).executeUpdate();
			session.createMutationQuery( "delete from Address" ).executeUpdate();
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Message result = session.createQuery( "from Message", Message.class ).getSingleResult();
			assertResult( result );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Message result = session.find( Message.class, 2L );
			assertResult( result );
		} );
	}

	private void assertResult(Message result) {
		final Address address = result.getReceiverAddress();
		assertThat( Hibernate.isInitialized( address ) ).isFalse();
		assertThat( Hibernate.getClass( address ) ).isEqualTo( MyAddress.class );
		final MyAddress myAddress = (MyAddress) Hibernate.unproxy( address );
		final Set<AddressContainer> relations = myAddress.getAddressUserRelations();
		assertThat( relations ).hasSize( 1 );
		final MyAddress relatedAddress = relations.iterator().next().getMyAddress();
		assertThat( relatedAddress.getId() ).isEqualTo( 1L );
		assertThat( relatedAddress ).isSameAs( myAddress );
	}

	@Test
	public void testProxyReuse(SessionFactoryScope scope) {
		// uninitialized proxy
		scope.inTransaction( session -> {
			final Address address = session.getReference( Address.class, 1L );
			assertThat( Hibernate.isInitialized( address ) ).isFalse();
			final AddressContainer addressContainer = session.find( AddressContainer.class, 3L );
			final MyAddress myAddress = addressContainer.getMyAddress();
			assertThat( Hibernate.isInitialized( myAddress ) ).isFalse();
			assertThat( myAddress.getId() ).isEqualTo( address.getId() );
		} );
		// initialized proxy
		scope.inTransaction( session -> {
			final Address address = session.getReference( Address.class, 1L );
			assertThat( Hibernate.getClass( address ) ).isEqualTo( MyAddress.class );
			assertThat( Hibernate.isInitialized( address ) ).isTrue();
			final AddressContainer addressContainer = session.find( AddressContainer.class, 3L );
			final MyAddress myAddress = addressContainer.getMyAddress();
			assertThat( Hibernate.isInitialized( myAddress ) ).isTrue();
			assertThat( myAddress.getId() ).isEqualTo( address.getId() );
		} );
	}

	@Entity( name = "Address" )
	public static abstract class Address {
		@Id
		private Long id;

		public Address() {
		}

		public Address(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Long id;

		@ManyToOne( fetch = FetchType.LAZY )
		@JoinColumn( name = "receiver_address_id" )
		private Address receiverAddress;

		public Message() {
		}

		public Message(Long id, Address receiverAddress) {
			this.id = id;
			this.receiverAddress = receiverAddress;
		}

		public Address getReceiverAddress() {
			return receiverAddress;
		}
	}

	@Entity( name = "MyAddress" )
	public static class MyAddress extends Address {
		@OneToMany( mappedBy = "myAddress" )
		private Set<AddressContainer> addressContainers = new HashSet<>();

		public MyAddress() {
		}

		public MyAddress(Long id) {
			super( id );
		}

		public Set<AddressContainer> getAddressUserRelations() {
			return addressContainers;
		}
	}

	@Entity( name = "AddressContainer" )
	public static class AddressContainer {
		@Id
		private Long id;

		@ManyToOne( fetch = LAZY )
		@JoinColumn( name = "address_id" )
		private MyAddress myAddress;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MyAddress getMyAddress() {
			return myAddress;
		}

		public void setMyAddress(MyAddress myAddress) {
			this.myAddress = myAddress;
		}
	}
}
