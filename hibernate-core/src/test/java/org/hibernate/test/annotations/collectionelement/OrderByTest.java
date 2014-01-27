/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Iterator;

import junit.framework.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

public class OrderByTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOrderByName() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Products p = new Products();
		HashSet<Widgets> set = new HashSet<Widgets>();

		Widgets widget = new Widgets();
		widget.setName("hammer");
		set.add(widget);

		widget = new Widgets();
		widget.setName("axel");
		set.add(widget);

		widget = new Widgets();
		widget.setName("screwdriver");
		set.add(widget);

		p.setWidgets(set);
		s.persist(p);
		tx.commit();

		tx = s.beginTransaction();
		s.clear();
		p = (Products) s.get(Products.class,p.getId());
		Assert.assertTrue("has three Widgets", p.getWidgets().size() == 3);
		Iterator iter = p.getWidgets().iterator();
		Assert.assertEquals( "axel", ((Widgets)iter.next()).getName() );
		Assert.assertEquals( "hammer", ((Widgets)iter.next()).getName() );
		Assert.assertEquals( "screwdriver", ((Widgets)iter.next()).getName() );
		tx.commit();
		s.close();
	}

	@Test
	public void testOrderByWithDottedNotation() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		BugSystem bs = new BugSystem();
		HashSet<Bug> set = new HashSet<Bug>();

		Bug bug = new Bug();
		bug.setDescription("JPA-2 locking");
		bug.setSummary("JPA-2 impl locking");
		Person p = new Person();
		p.setFirstName("Scott");
		p.setLastName("Marlow");
		bug.setReportedBy(p);
		set.add(bug);

		bug = new Bug();
		bug.setDescription("JPA-2 annotations");
		bug.setSummary("JPA-2 impl annotations");
		p = new Person();
		p.setFirstName("Emmanuel");
		p.setLastName("Bernard");
		bug.setReportedBy(p);
		set.add(bug);

		bug = new Bug();
		bug.setDescription("Implement JPA-2 criteria");
		bug.setSummary("JPA-2 impl criteria");
		p = new Person();
		p.setFirstName("Steve");
		p.setLastName("Ebersole");
		bug.setReportedBy(p);
		set.add(bug);

		bs.setBugs(set);
		s.persist(bs);
		tx.commit();

		tx = s.beginTransaction();
		s.clear();
		bs = (BugSystem) s.get(BugSystem.class,bs.getId());
		Assert.assertTrue("has three bugs", bs.getBugs().size() == 3);
		Iterator iter = bs.getBugs().iterator();
		Assert.assertEquals( "Emmanuel", ((Bug)iter.next()).getReportedBy().getFirstName() );
		Assert.assertEquals( "Steve", ((Bug)iter.next()).getReportedBy().getFirstName() );
		Assert.assertEquals( "Scott", ((Bug)iter.next()).getReportedBy().getFirstName() );
		tx.commit();
		s.close();

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			Products.class,
			BugSystem.class,
			Bug.class,
			Person.class,
			Widgets.class
		};
	}

}
