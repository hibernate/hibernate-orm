/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2.mapkeycolumn;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		MapKeyColumnElementCollectionTest.AddressCapable.class,
		MapKeyColumnElementCollectionTest.AddressCapable2.class
})
@SessionFactory
public class MapKeyColumnElementCollectionTest {

	@Test
	@JiraKey( value = "HHH-12150" )
	public void testReferenceToAlreadyMappedColumn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					AddressCapable2 holder = new AddressCapable2( 1, "osd");

					session.persist( holder );
				}
		);
		scope.inTransaction(
				session -> {
					AddressCapable2 holder = session.find( AddressCapable2.class, 1 );
					Address2 address = new Address2( 1, "123 Main St" );
					address.type = "work";

					holder.addresses.put( "work", address );

					session.persist( holder );
				}
		);
		scope.inTransaction(
				session -> {
					AddressCapable2 holder = session.find( AddressCapable2.class, 1 );
					assertEquals( 1, holder.addresses.size() );
					final Map.Entry<String,Address2> entry = holder.addresses.entrySet().iterator().next();
					assertEquals( "work", entry.getKey() );
					assertEquals( "work", entry.getValue().type );
					session.remove( holder );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-12150" )
	public void testReferenceToNonMappedColumn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					AddressCapable holder = new AddressCapable( 1, "osd");

					session.persist( holder );
				}
		);
		scope.inTransaction(
				session -> {
					AddressCapable holder = session.find( AddressCapable.class, 1 );

					holder.addresses.put( "work", new Address( 1, "123 Main St" ) );

					session.persist( holder );
				}
		);
		scope.inTransaction(
				session -> {
					AddressCapable holder = session.find( AddressCapable.class, 1 );
					assertEquals( 1, holder.addresses.size() );
					final Map.Entry<String,Address> entry = holder.addresses.entrySet().iterator().next();
					assertEquals( "work", entry.getKey() );
					session.remove( holder );
				}
		);
	}

	@Entity( name = "AddressCapable" )
	@Table( name = "address_capables" )
	public static class AddressCapable {
		@Id
		public Integer id;
		public String name;
		@MapKeyColumn( name = "a_type" )
		@ElementCollection
		public Map<String,Address> addresses = new HashMap<>();

		public AddressCapable() {
		}

		public AddressCapable(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Embeddable
	public static class Address {
		public Integer buildingNumber;
		public String street;

		public Address() {
		}

		public Address(Integer buildingNumber, String street) {
			this.buildingNumber = buildingNumber;
			this.street = street;
		}
	}

	@Entity( name = "AddressCapable2" )
	@Table( name = "address_capables2" )
	public static class AddressCapable2 {
		@Id
		public Integer id;
		public String name;
		@MapKeyColumn( name = "a_type", insertable = false, updatable = false)
		@ElementCollection
		public Map<String,Address2> addresses = new HashMap<>();

		public AddressCapable2() {
		}

		public AddressCapable2(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Embeddable
	public static class Address2 {
		public Integer buildingNumber;
		public String street;
		@Column( name = "a_type" )
		public String type;

		public Address2() {
		}

		public Address2(Integer buildingNumber, String street) {
			this.buildingNumber = buildingNumber;
			this.street = street;
		}
	}
}
