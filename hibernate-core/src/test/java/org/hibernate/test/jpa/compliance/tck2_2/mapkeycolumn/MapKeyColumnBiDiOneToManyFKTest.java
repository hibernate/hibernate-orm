/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.compliance.tck2_2.mapkeycolumn;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class MapKeyColumnBiDiOneToManyFKTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-12150" )
	public void testReferenceToAlreadyMappedColumn() {
		inTransaction(
				session -> {
					AddressCapable2 holder = new AddressCapable2( 1, "osd");
					Address2 address = new Address2( 1, "123 Main St" );

					session.persist( holder );
					session.persist( address );
				}
		);
		inTransaction(
				session -> {
					AddressCapable2 holder = session.get( AddressCapable2.class, 1 );
					Address2 address = session.get( Address2.class, 1 );

					address.holder = holder;
					holder.addresses.put( "work", address );

					session.persist( holder );
				}
		);
		inTransaction(
				session -> {
					AddressCapable2 holder = session.get( AddressCapable2.class, 1 );
					assertEquals( 1, holder.addresses.size() );
					final Map.Entry<String,Address2> entry = holder.addresses.entrySet().iterator().next();
					assertEquals( "work", entry.getKey() );
					assertEquals( "work", entry.getValue().type );
					session.remove( holder );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12150" )
	public void testReferenceToNonMappedColumn() {
		inTransaction(
				session -> {
					AddressCapable holder = new AddressCapable( 1, "osd");
					Address address = new Address( 1, "123 Main St" );

					session.persist( holder );
					session.persist( address );
				}
		);
		inTransaction(
				session -> {
					AddressCapable holder = session.get( AddressCapable.class, 1 );
					Address address = session.get( Address.class, 1 );

					address.holder = holder;
					holder.addresses.put( "work", address );

					session.persist( holder );
				}
		);
		inTransaction(
				session -> {
					AddressCapable holder = session.get( AddressCapable.class, 1 );
					assertEquals( 1, holder.addresses.size() );
					final Map.Entry<String,Address> entry = holder.addresses.entrySet().iterator().next();
					assertEquals( "work", entry.getKey() );
					session.remove( holder );
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
		@MapKeyColumn( name = "a_type" )
		@OneToMany( mappedBy = "holder", cascade = {CascadeType.PERSIST, CascadeType.REMOVE} )
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
		@ManyToOne
		public AddressCapable holder;

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
		@OneToMany( mappedBy = "holder", cascade = {CascadeType.PERSIST, CascadeType.REMOVE} )
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
		@ManyToOne
		public AddressCapable2 holder;

		public Address2() {
		}

		public Address2(Integer id, String street) {
			this.id = id;
			this.street = street;
		}
	}
}
