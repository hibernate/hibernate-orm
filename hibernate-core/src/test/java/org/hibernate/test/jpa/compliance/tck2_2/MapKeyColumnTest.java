/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class MapKeyColumnTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testReferenceToAlreadyMappedColumn() {
		inTransaction(
				session -> {
					AddressCapable2 holder = new AddressCapable2( 1, "osd");
					holder.addresses.put( "work", new Address2( 1, "123 Main St" ) );

					session.persist( holder );
				}
		);
		inTransaction(
				session -> {
					session.remove(
							session.byId( AddressCapable2.class ).load( 1 )
					);
				}
		);
	}

	@Test
	public void testReferenceToNonMappedColumn() {
		inTransaction(
				session -> {
					AddressCapable holder = new AddressCapable( 1, "osd");
					holder.addresses.put( "work", new Address( 1, "123 Main St" ) );

					session.persist( holder );
				}
		);
		inTransaction(
				session -> {
					session.remove(
							session.byId( AddressCapable.class ).load( 1 )
					);
				}
		);
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );

		sources.addAnnotatedClass( AddressCapable.class );
		sources.addAnnotatedClass( AddressCapable2.class );
		sources.addAnnotatedClass( Address.class );
		sources.addAnnotatedClass( Address2.class );
	}

	@Entity( name = "AddressCapable" )
	@Table( name = "address_capables" )
	public static class AddressCapable {
		@Id
		public Integer id;
		public String name;
		@JoinColumn
		@MapKeyColumn( name = "a_type" )
		@OneToMany( cascade = {CascadeType.PERSIST, CascadeType.REMOVE} )
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
		@JoinColumn
		@MapKeyColumn( name = "a_type" )
		@OneToMany( cascade = {CascadeType.PERSIST, CascadeType.REMOVE} )
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
