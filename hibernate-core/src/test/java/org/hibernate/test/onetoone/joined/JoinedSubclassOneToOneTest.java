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
package org.hibernate.test.onetoone.joined;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gavin King
 */
public class JoinedSubclassOneToOneTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "onetoone/joined/Person.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	@Test
	public void testOneToOneOnSubclass() {
		Person p = new Person();
		p.name = "Gavin";
		Address a = new Address();
		a.entityName = "Gavin";
		a.zip = "3181";
		a.state = "VIC";
		a.street = "Karbarook Ave";
		p.address = a;
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		EntityStatistics addressStats = sessionFactory().getStatistics().getEntityStatistics( Address.class.getName() );
		EntityStatistics mailingAddressStats = sessionFactory().getStatistics().getEntityStatistics("MailingAddress");

		p = (Person) s.createQuery("from Person p join fetch p.address left join fetch p.mailingAddress").uniqueResult();
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();

		p = (Person) s.createQuery("select p from Person p join fetch p.address left join fetch p.mailingAddress").uniqueResult();
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();

		Object[] stuff = (Object[]) s.createQuery("select p.name, p from Person p join fetch p.address left join fetch p.mailingAddress").uniqueResult();
		assertEquals(stuff.length, 2);
		p = (Person) stuff[1];
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();

		assertEquals( addressStats.getFetchCount(), 0 );
		assertEquals( mailingAddressStats.getFetchCount(), 0 );
		
		p = (Person) s.createQuery("from Person p join fetch p.address").uniqueResult();
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();
		
		assertEquals( addressStats.getFetchCount(), 0 );
		assertEquals( mailingAddressStats.getFetchCount(), 1 );

		p = (Person) s.createQuery("from Person").uniqueResult();
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();
		
		assertEquals( addressStats.getFetchCount(), 0 );
		assertEquals( mailingAddressStats.getFetchCount(), 2 );

		p = (Person) s.createQuery("from Entity").uniqueResult();
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();
		
		assertEquals( addressStats.getFetchCount(), 0 );
		assertEquals( mailingAddressStats.getFetchCount(), 3 );

		//note that in here join fetch is used for the nullable
		//one-to-one, due to a very special case of default
		p = (Person) s.get(Person.class, "Gavin");
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();
		
		assertEquals( addressStats.getFetchCount(), 0 );
		assertEquals( mailingAddressStats.getFetchCount(), 3 );

		p = (Person) s.get(Entity.class, "Gavin");
		assertNotNull(p.address); assertNull(p.mailingAddress);
		s.clear();
		
		assertEquals( addressStats.getFetchCount(), 0 );
		assertEquals( mailingAddressStats.getFetchCount(), 3 );
		
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		Org org = new Org();
		org.name = "IFA";
		Address a2 = new Address();
		a2.entityName = "IFA";
		a2.zip = "3181";
		a2.state = "VIC";
		a2.street = "Orrong Rd";
		s.persist(org);
		s.persist(a2);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.get(Entity.class, "IFA");
		s.clear();
		
		List list = s.createQuery("from Entity e order by e.name").list();
		p = (Person) list.get(0);
		assertNotNull(p.address); assertNull(p.mailingAddress);
		list.get(1);
		s.clear();
		
		list = s.createQuery("from Entity e left join fetch e.address left join fetch e.mailingAddress order by e.name").list();
		p = (Person) list.get(0);
		org = (Org) list.get(1);
		assertNotNull(p.address); assertNull(p.mailingAddress);
		
		s.clear();
		s.delete(p);
		s.delete( p.address );
		s.delete( org );
		s.delete( a2 );
		s.flush();
		t.commit();
		s.close();
		
	}

}

