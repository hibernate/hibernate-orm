/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.formula;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Property;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.TextType;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class OneToOneFormulaTest extends BaseCoreFunctionalTestCase {
	private static class TextAsMaterializedClobType extends AbstractSingleColumnStandardBasicType<String> {
		public final static TextAsMaterializedClobType INSTANCE = new TextAsMaterializedClobType();
		public TextAsMaterializedClobType() {
			super(  ClobTypeDescriptor.DEFAULT, TextType.INSTANCE.getJavaTypeDescriptor() );
		}
		public String getName() {
			return TextType.INSTANCE.getName();
		}
	}

	public String[] getMappings() {
		return new String[] { "onetoone/formula/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		if ( Oracle8iDialect.class.isInstance( getDialect() ) ) {
			cfg.registerTypeOverride( TextAsMaterializedClobType.INSTANCE );
		}
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty(Environment.DEFAULT_BATCH_FETCH_SIZE, "2");
	}

	@Test
	public void testOneToOneFormula() {
		Person p = new Person();
		p.setName("Gavin King");
		Address a = new Address();
		a.setPerson(p);
		a.setType("HOME");
		a.setZip("3181");
		a.setState("VIC");
		a.setStreet("Karbarook Ave");
		p.setAddress(a);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		p = (Person) s.createQuery("from Person").uniqueResult();
		
		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );

		s.clear();

		p = (Person) s.createQuery("from Person p left join fetch p.mailingAddress left join fetch p.address").uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );

		s.clear();

		p = (Person) s.createQuery("from Person p left join fetch p.address").uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );

		s.clear();

		p = (Person) s.createCriteria(Person.class)
			.createCriteria("address")
				.add( Property.forName("zip").eq("3181") )
			.uniqueResult();
		assertNotNull(p);
		
		s.clear();

		p = (Person) s.createCriteria(Person.class)
			.setFetchMode("address", FetchMode.JOIN)
			.uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );
		
		s.clear();

		p = (Person) s.createCriteria(Person.class)
			.setFetchMode("mailingAddress", FetchMode.JOIN)
			.uniqueResult();

		assertNotNull( p.getAddress() );
		assertTrue( Hibernate.isInitialized( p.getAddress() ) );
		assertNull( p.getMailingAddress() );
		
		s.delete(p);
		
		t.commit();
		s.close();
	}

	@Test
	public void testOneToOneEmbeddedCompositeKey() {
		Person p = new Person();
		p.setName("Gavin King");
		Address a = new Address();
		a.setPerson(p);
		a.setType("HOME");
		a.setZip("3181");
		a.setState("VIC");
		a.setStreet("Karbarook Ave");
		p.setAddress(a);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		a = new Address();
		a.setType("HOME");
		a.setPerson(p);
		a = (Address) s.load(Address.class, a);
		assertFalse( Hibernate.isInitialized(a) );
		a.getPerson();
		a.getType();
		assertFalse( Hibernate.isInitialized(a) );
		assertEquals(a.getZip(), "3181");
		
		s.clear();
		
		a = new Address();
		a.setType("HOME");
		a.setPerson(p);
		Address a2 = (Address) s.get(Address.class, a);
		assertTrue( Hibernate.isInitialized(a) );
		assertSame(a2, a);
		assertSame(a2.getPerson(), p); //this is a little bit desirable
		assertEquals(a.getZip(), "3181");
		
		s.delete(a2);
		s.delete( s.get( Person.class, p.getName() ) ); //this is certainly undesirable! oh well...
		
		t.commit();
		s.close();
		
	}

}
