/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 */
public class OneToOneLinkTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "onetoone/link/Person.hbm.xml" };
	}

	@Test
	@SkipForDialect(value = Oracle10gDialect.class, comment = "oracle12c returns time in getDate.  For now, skip.")
	@SkipForDialect(value = AbstractHANADialect.class, comment = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testOneToOneViaAssociationTable() {
		Person p = new Person();
		p.setName("Gavin King");
		p.setDob( new Date() );
		Employee e = new Employee();
		p.setEmployee(e);
		e.setPerson(p);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
	
		s = openSession();
		t = s.beginTransaction();
		e = (Employee) s.createQuery("from Employee e where e.person.name like 'Gavin%'").uniqueResult();
		assertEquals( e.getPerson().getName(), "Gavin King" );
		assertFalse( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		s.clear();

		e = (Employee) s.createQuery("from Employee e where e.person.dob = :date")
			.setDate("date", new Date() )
			.uniqueResult();
		assertEquals( e.getPerson().getName(), "Gavin King" );
		assertFalse( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		s.clear();
		
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();

		e = (Employee) s.createQuery("from Employee e join fetch e.person p left join fetch p.customer").uniqueResult();
		assertTrue( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		Customer c = new Customer();
		e.getPerson().setCustomer(c);
		c.setPerson( e.getPerson() );
		
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();

		e = (Employee) s.createQuery("from Employee e join fetch e.person p left join fetch p.customer").uniqueResult();
		assertTrue( Hibernate.isInitialized( e.getPerson() ) );
		assertTrue( Hibernate.isInitialized( e.getPerson().getCustomer() ) );
		assertNotNull( e.getPerson().getCustomer() );
		s.delete(e);
		t.commit();
		s.close();
		
	}

}

