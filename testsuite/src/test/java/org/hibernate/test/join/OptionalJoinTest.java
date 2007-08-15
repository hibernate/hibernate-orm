//$Id: $
package org.hibernate.test.join;

/**
 * @author Chris Jones and Gail Badner
 */
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.Session;
import org.hibernate.Transaction;
import junit.framework.Test;

import java.sql.ResultSet;
import java.util.List;

public class OptionalJoinTest extends FunctionalTestCase {

	public OptionalJoinTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OptionalJoinTest.class );
	}

	public String[] getMappings() {
		return new String[] { "join/Thing.hbm.xml" };
	}

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
