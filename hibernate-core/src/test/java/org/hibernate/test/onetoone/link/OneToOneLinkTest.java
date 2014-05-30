/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 */
@FailureExpectedWithNewUnifiedXsd(message = "null rows attempted on secondary table, even though optional <join> " +
		"is handled in EntityMocker.")
public class OneToOneLinkTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "onetoone/link/Person.hbm.xml" };
	}

	@Test
	@SkipForDialect(value = Oracle10gDialect.class, comment = "oracle12c returns time in getDate.  For now, skip.")
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

