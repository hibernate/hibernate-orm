/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2.mapkeycolumn;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		MapKeyColumnManyToManyTest.AddressCapable.class,
		MapKeyColumnManyToManyTest.AddressCapable2.class,
		MapKeyColumnManyToManyTest.Address.class,
		MapKeyColumnManyToManyTest.Address2.class
})
@SessionFactory
public class MapKeyColumnManyToManyTest {

	@Test
	@JiraKey( value = "HHH-12150" )
	public void testReferenceToAlreadyMappedColumn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					AddressCapable2 holder = new AddressCapable2( 1, "osd");
					Address2 address = new Address2( 1, "123 Main St" );

					session.persist( holder );
					session.persist( address );
				}
		);
		scope.inTransaction(
				session -> {
					AddressCapable2 holder = session.find( AddressCapable2.class, 1 );
					Address2 address = session.find( Address2.class, 1 );

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
					assertNull( entry.getValue().type );
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
					Address address = new Address( 1, "123 Main St" );

					session.persist( holder );
					session.persist( address );
				}
		);
		scope.inTransaction(
				session -> {
					AddressCapable holder = session.find( AddressCapable.class, 1 );
					Address address = session.find( Address.class, 1 );

					holder.addresses.put( "work", address );

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
		@ManyToMany( cascade = {CascadeType.PERSIST, CascadeType.REMOVE} )
		public Map<String,Address> addresses = new HashMap<>();

		public AddressCapable() {
		}

		public AddressCapable(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "Address" )
	@Table( name = "addresses" )
	public static class Address {
		@Id
		public Integer id;
		public String street;

		public Address() {
		}

		public Address(Integer id, String street) {
			this.id = id;
			this.street = street;
		}
	}

	@Entity( name = "AddressCapable2" )
	@Table( name = "address_capables2" )
	public static class AddressCapable2 {
		@Id
		public Integer id;
		public String name;
		@MapKeyColumn( name = "a_type" )
		@ManyToMany( cascade = {CascadeType.PERSIST, CascadeType.REMOVE} )
		public Map<String,Address2> addresses = new HashMap<>();

		public AddressCapable2() {
		}

		public AddressCapable2(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "Address2" )
	@Table( name = "addresses2" )
	public static class Address2 {
		@Id
		public Integer id;
		public String street;
		@Column( name = "a_type" )
		public String type;

		public Address2() {
		}

		public Address2(Integer id, String street) {
			this.id = id;
			this.street = street;
		}
	}
}
