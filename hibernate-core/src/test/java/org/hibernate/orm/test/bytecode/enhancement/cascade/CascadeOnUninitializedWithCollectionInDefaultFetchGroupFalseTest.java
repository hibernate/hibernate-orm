/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.jdbc.SQLStatementInspector.extractFromSession;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Same as {@link CascadeDeleteCollectionTest},
 * but with {@code collectionInDefaultFetchGroup} set to {@code false} explicitly.
 * <p>
 * Kept here for <a href="https://github.com/hibernate/hibernate-orm/pull/5252#pullrequestreview-1095843220">historical reasons</a>.
 *
 * @author Bolek Ziobrowski
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-13129")
@DomainModel(
		annotatedClasses = {
			CascadeOnUninitializedWithCollectionInDefaultFetchGroupFalseTest.Person.class, CascadeOnUninitializedWithCollectionInDefaultFetchGroupFalseTest.Address.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "true" ),
		}
)
@SessionFactory(
		// We want to test with this setting set to false explicitly,
		// because another test already takes care of the default.
		applyCollectionsInDefaultFetchGroup = false
)
@BytecodeEnhanced
public class CascadeOnUninitializedWithCollectionInDefaultFetchGroupFalseTest {

	@Test
	public void testMergeDetachedEnhancedEntityWithUninitializedManyToOne(SessionFactoryScope scope) {
		final Person person = persistPersonWithManyToOne(scope);

		// get a detached Person
		final Person detachedPerson = scope.fromTransaction(
				session -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					Person p = session.get( Person.class, person.getId() );

					// loading Person should lead to one SQL
					statementInspector.assertExecutedCount( 1 );
					return p;
				}
		);

		// primaryAddress should be "initialized" as an enhanced-proxy
		assertTrue( Hibernate.isPropertyInitialized( detachedPerson, "primaryAddress" ) );
		assertThat( detachedPerson.getPrimaryAddress() ).isNotInstanceOf( HibernateProxy.class );
		assertFalse( Hibernate.isInitialized( detachedPerson.getPrimaryAddress() ) );

		// alter the detached reference
		detachedPerson.setName( "newName" );

		final Person mergedPerson = scope.fromTransaction(
				session -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();
					Person merged = session.merge( detachedPerson );

					// 1) select Person#addresses
					// 2) select Person#primaryAddress
					// 3) update Person
					session.flush();
					statementInspector.assertExecutedCount( 2 );
					return merged;
				}
		);

		// primaryAddress should not be initialized
		assertTrue( Hibernate.isPropertyInitialized( detachedPerson, "primaryAddress" ) );
		assertThat( detachedPerson.getPrimaryAddress() ).isNotInstanceOf( HibernateProxy.class );
		assertFalse( Hibernate.isInitialized( detachedPerson.getPrimaryAddress() ) );

		assertEquals( "newName", mergedPerson.getName() );
	}

	@Test
	public void testDeleteEnhancedEntityWithUninitializedManyToOne(SessionFactoryScope scope) {
		Person person = persistPersonWithManyToOne(scope);

		// get a detached Person
		Person detachedPerson = scope.fromTransaction(
				session -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();
					Person p = session.get( Person.class, person.getId() );

					// loading Person should lead to one SQL
					statementInspector.assertExecutedCount( 1 );

					return p;
				}
		);

		// primaryAddress should be initialized as an enhance-proxy
		assertTrue( Hibernate.isPropertyInitialized( detachedPerson, "primaryAddress" ) );
		assertThat( detachedPerson ).isNotInstanceOf( HibernateProxy.class );
		assertFalse( Hibernate.isInitialized( detachedPerson.getPrimaryAddress() ) );

		// deleting detachedPerson should result in detachedPerson.primaryAddress being initialized,
		// so that the DELETE operation can be cascaded to it.
		scope.inTransaction(
				session -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					session.remove( detachedPerson );

					// 1) select Person#addresses
					// 2) select Person#primaryAddress
					// 3) delete Person
					// 4) select primary Address
					session.flush();
					statementInspector.assertExecutedCount( 4 );
				}
		);

		// both the Person and its Address should be deleted
		scope.inTransaction(
				session -> {
					assertNull( session.get( Person.class, person.getId() ) );
					assertNull( session.get( Person.class, person.getPrimaryAddress().getId() ) );
				}
		);
	}

	@Test
	public void testMergeDetachedEnhancedEntityWithUninitializedOneToMany(SessionFactoryScope scope) {

		Person person = persistPersonWithOneToMany(scope);

		// get a detached Person
		Person detachedPerson = scope.fromTransaction( session -> session.get( Person.class, person.getId() ) );

		// address should not be initialized
		assertFalse( Hibernate.isPropertyInitialized( detachedPerson, "addresses" ) );
		detachedPerson.setName( "newName" );

		Person mergedPerson = scope.fromTransaction( session -> session.merge( detachedPerson ) );

		// address should be initialized
		assertTrue( Hibernate.isPropertyInitialized( mergedPerson, "addresses" ) );
		assertEquals( "newName", mergedPerson.getName() );
	}

	@Test
	public void testDeleteEnhancedEntityWithUninitializedOneToMany(SessionFactoryScope scope) {
		Person person = persistPersonWithOneToMany(scope);

		// get a detached Person
		Person detachedPerson = scope.fromTransaction( session -> {
					return session.get( Person.class, person.getId() );
				}
		);

		// address should not be initialized
		assertFalse( Hibernate.isPropertyInitialized( detachedPerson, "addresses" ) );

		// deleting detachedPerson should result in detachedPerson.address being initialized,
		// so that the DELETE operation can be cascaded to it.
		scope.inTransaction( session -> session.remove( detachedPerson ) );

		// both the Person and its Address should be deleted
		scope.inTransaction( session -> {
					assertNull( session.get( Person.class, person.getId() ) );
					assertNull( session.get( Person.class, person.getAddresses().iterator().next().getId() ) );
				}
		);
	}

	public Person persistPersonWithManyToOne(SessionFactoryScope scope) {
		Address address = new Address();
		address.setDescription( "ABC" );

		final Person person = new Person();
		person.setName( "John Doe" );
		person.setPrimaryAddress( address );

		scope.inTransaction( session -> session.persist( person ) );

		return person;
	}

	public Person persistPersonWithOneToMany(SessionFactoryScope scope) {
		Address address = new Address();
		address.setDescription( "ABC" );

		final Person person = new Person();
		person.setName( "John Doe" );
		person.getAddresses().add( address );

		scope.inTransaction( session -> session.persist( person ) );

		return person;
	}

	@Entity
	@Table(name = "TEST_PERSON")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "NAME", length = 300, nullable = true)
		private String name;

		@ManyToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY)
		@JoinColumn(name = "primary_address_id")
		private Address primaryAddress;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn( name = "person_id" )
		private Set<Address> addresses = new HashSet<>();

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

		public Address getPrimaryAddress() {
			return primaryAddress;
		}

		public void setPrimaryAddress(Address primaryAddress) {
			this.primaryAddress = primaryAddress;
		}

		public Set<Address> getAddresses() {
			return addresses;
		}

		public void setAddresses(Set<Address> addresses) {
			this.addresses = addresses;
		}
	}

	@Entity
	@Table(name = "TEST_ADDRESS")
	public static class Address {
		@Id
		@GeneratedValue
		private Long id;

		@Column(name = "DESCRIPTION", length = 300, nullable = true)
		private String description;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
