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
package org.hibernate.test.join;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Chris Jones and Gail Badner
 */
public class OptionalJoinTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "join/Thing.hbm.xml" };
	}

	@Test
	public void testUpdateNonNullOptionalJoinToDiffNonNull() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// create a new thing with a non-null name
		Thing thing = new Thing();
		thing.setName("one");
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one", thing.getName());
		// give it a new non-null name and save it
		thing.setName("one_changed");
		s.update(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one_changed", thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateNonNullOptionalJoinToDiffNonNullDetached() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// create a new thing with a non-null name
		Thing thing = new Thing();
		thing.setName("one");
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one", thing.getName());
		t.commit();
		s.close();
				
		// change detached thing name to a new non-null name and save it
		thing.setName("one_changed");

		s = openSession();
		t = s.beginTransaction();
		s.update(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one_changed", thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testMergeNonNullOptionalJoinToDiffNonNullDetached() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// create a new thing with a non-null name
		Thing thing = new Thing();
		thing.setName("one");
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one", thing.getName());
		t.commit();
		s.close();

		// change detached thing name to a new non-null name and save it
		thing.setName("one_changed");

		s = openSession();
		t = s.beginTransaction();
		s.merge(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one_changed", thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateNonNullOptionalJoinToNull() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// create a new thing with a non-null name
		Thing thing = new Thing();
		thing.setName("one");
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one", thing.getName());
		// give it a null name and save it
		thing.setName(null);
		s.update(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertNull(thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateNonNullOptionalJoinToNullDetached() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// create a new thing with a non-null name
		Thing thing = new Thing();
		thing.setName("one");
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one", thing.getName());
		t.commit();
		s.close();

		// give detached thing a null name and save it
		thing.setName(null);

		s = openSession();
		t = s.beginTransaction();
		s.update(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertNull(thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testMergeNonNullOptionalJoinToNullDetached() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// create a new thing with a non-null name
		Thing thing = new Thing();
		thing.setName("one");
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertEquals("one", thing.getName());
		t.commit();
		s.close();

		// give detached thing a null name and save it
		thing.setName(null);

		s = openSession();
		t = s.beginTransaction();
		s.merge(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertNull(thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateNullOptionalJoinToNonNull() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		// create a new thing with a null name
		Thing thing = new Thing();
		thing.setName(null);
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertNull(thing.getName());
		// change name to a non-null value
		thing.setName("two");
		s.update(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = ((Thing) things.get(0));
		assertEquals("two", thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateNullOptionalJoinToNonNullDetached() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		// create a new thing with a null name
		Thing thing = new Thing();
		thing.setName(null);
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertNull(thing.getName());
		t.commit();
		s.close();

		// change detached thing name to a non-null value
		thing.setName("two");

		s = openSession();
		t = s.beginTransaction();
		s.update(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = ((Thing) things.get(0));
		assertEquals("two", thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}

	@Test
	public void testMergeNullOptionalJoinToNonNullDetached() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		// create a new thing with a null name
		Thing thing = new Thing();
		thing.setName(null);
		s.save(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = (Thing)things.get(0);
		assertNull(thing.getName());
		t.commit();
		s.close();

		// change detached thing name to a non-null value
		thing.setName("two");

		s = openSession();
		t = s.beginTransaction();
		s.merge(thing);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		things = s.createQuery("from Thing").list();
		assertEquals(1, things.size());
		thing = ((Thing) things.get(0));
		assertEquals("two", thing.getName());
		s.delete(thing);
		t.commit();
		s.close();
	}
}
