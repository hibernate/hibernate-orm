/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * Test originally written to help verify/diagnose HHH-10125
 *
 * @author Steve Ebersole
 */
public class MapFunctionExpressionsTest extends BaseNonConfigCoreFunctionalTestCase {
	private final ASTQueryTranslatorFactory queryTranslatorFactory = new ASTQueryTranslatorFactory();

	@Before
	public void prepareTestData() {
		Session s = openSession();
		s.getTransaction().begin();

		AddressType homeType = new AddressType( 1, "home" );
		s.persist( homeType );

		Address address = new Address( 1, "Main St.", "Somewhere, USA" );
		s.persist( address );

		Contact contact = new Contact( 1, "John" );
		contact.addresses.put( homeType, address );
		s.persist( contact );

		s.getTransaction().commit();
		s.close();
	}

	@After
	public void cleanUpTestData() {
		Session s = openSession();
		s.getTransaction().begin();

		s.delete( s.get( Contact.class, 1 ) );

		s.delete( s.get( Address.class, 1 ) );
		s.delete( s.get( AddressType.class, 1 ) );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMapKeyExpressionInWhere() {
		// NOTE : JPA requires that an alias be used in the key() expression.  Hibernate allows
		//		path or alias.

		Session s = openSession();
		s.getTransaction().begin();

		// JPA form
		List contacts = s.createQuery( "select c from Contact c join c.addresses a where key(a) is not null" ).list();
		assertEquals( 1, contacts.size() );
		Contact contact = assertTyping( Contact.class, contacts.get( 0 ) );

		// Hibernate additional form
		contacts = s.createQuery( "select c from Contact c where key(c.addresses) is not null" ).list();
		assertEquals( 1, contacts.size() );
		contact = assertTyping( Contact.class, contacts.get( 0 ) );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMapKeyExpressionInSelect() {
		// NOTE : JPA requires that an alias be used in the key() expression.  Hibernate allows
		//		path or alias.

		Session s = openSession();
		s.getTransaction().begin();

		// JPA form
		List types = s.createQuery( "select key(a) from Contact c join c.addresses a" ).list();
		assertEquals( 1, types.size() );
		assertTyping( AddressType.class, types.get( 0 ) );

		// Hibernate additional form
		types = s.createQuery( "select key(c.addresses) from Contact c" ).list();
		assertEquals( 1, types.size() );
		assertTyping( AddressType.class, types.get( 0 ) );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMapValueExpressionInSelect() {
		Session s = openSession();
		s.getTransaction().begin();

		List addresses = s.createQuery( "select value(a) from Contact c join c.addresses a" ).list();
		assertEquals( 1, addresses.size() );
		assertTyping( Address.class, addresses.get( 0 ) );

		addresses = s.createQuery( "select value(c.addresses) from Contact c" ).list();
		assertEquals( 1, addresses.size() );
		assertTyping( Address.class, addresses.get( 0 ) );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testMapEntryExpressionInSelect() {
		Session s = openSession();
		s.getTransaction().begin();

		List addresses = s.createQuery( "select entry(a) from Contact c join c.addresses a" ).list();
		assertEquals( 1, addresses.size() );
		assertTyping( Map.Entry.class, addresses.get( 0 ) );

		addresses = s.createQuery( "select entry(c.addresses) from Contact c" ).list();
		assertEquals( 1, addresses.size() );
		assertTyping( Map.Entry.class, addresses.get( 0 ) );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Address.class, AddressType.class, Contact.class };
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
//	@Embeddable
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
//		@JoinColumn
//		@ElementCollection
//		@MapKeyEnumerated(EnumType.STRING)
//		@MapKeyColumn(name = "addr_type")
		Map<AddressType, Address> addresses = new HashMap<AddressType, Address>();

		public Contact() {
		}

		public Contact(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
