/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test originally written to help verify/diagnose HHH-10125
 *
 * @author Steve Ebersole
 */
@SuppressWarnings({"JUnitMalformedDeclaration", "deprecation"})
@JiraKey("HHH-10125")
@DomainModel(annotatedClasses = {
		MapFunctionExpressionsTest.Address.class,
		MapFunctionExpressionsTest.AddressType.class,
		MapFunctionExpressionsTest.Contact.class
})
@SessionFactory
public class MapFunctionExpressionsTest {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( s -> {
			AddressType homeType = new AddressType( 1, "home" );
			s.persist( homeType );

			Address address = new Address( 1, "Main St.", "Somewhere, USA" );
			s.persist( address );

			Contact contact = new Contact( 1, "John" );
			contact.addresses.put( homeType, address );
			s.persist( contact );
		} );
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testMapKeyExpressionInWhere(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			// NOTE : JPA requires that an alias be used in the key() expression.
			// 		Hibernate allows path or alias.

			// JPA form
			var results = s.createQuery( "select c from Contact c join c.addresses a where key(a) is not null" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( Contact.class );

			// Hibernate additional form
			results = s.createQuery( "select c from Contact c where key(c.addresses) is not null" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( Contact.class );
		} );
	}

	@Test
	public void testMapKeyExpressionInSelect(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			// NOTE : JPA requires that an alias be used in the key() expression.
			// 		Hibernate allows path or alias.

			// JPA form
			var results = s.createQuery( "select key(a) from Contact c join c.addresses a" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( AddressType.class );

			// Hibernate additional form
			results = s.createQuery( "select key(c.addresses) from Contact c" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( AddressType.class );
		} );
	}

	@Test
	public void testMapValueExpressionInSelect(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			var results = s.createQuery( "select value(a) from Contact c join c.addresses a" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( Address.class );

			results = s.createQuery( "select value(c.addresses) from Contact c" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( Address.class );
		} );
	}

	@Test
	public void testMapEntryExpressionInSelect(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			var results = s.createQuery( "select entry(a) from Contact c join c.addresses a" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( Map.Entry.class );

			results = s.createQuery( "select entry(c.addresses) from Contact c" ).list();
			assertEquals( 1, results.size() );
			assertThat( results.get(0) ).isInstanceOf( Map.Entry.class );
		} );
	}

	@Entity(name = "AddressType")
	@Table(name = "address_type")
	public static class AddressType {
		@Id
		public Integer id;
		String name;

		public AddressType() {
		}

		public AddressType(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Address")
	@Table(name = "address")
	public static class Address {
		@Id
		public Integer id;
		String street;
		String city;

		public Address() {
		}

		public Address(Integer id, String street, String city) {
			this.id = id;
			this.street = street;
			this.city = city;
		}
	}

	@Entity(name = "Contact")
	@Table(name = "contact")
	public static class Contact {
		@Id
		public Integer id;
		String name;
		@OneToMany
		@JoinTable(name = "contact_address")
		@MapKeyJoinColumn(name="address_type_id", referencedColumnName="id")
		Map<AddressType, Address> addresses = new HashMap<>();

		public Contact() {
		}

		public Contact(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
