package org.hibernate.test.formulajoin;

import static org.junit.Assert.*;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AssociationFormulaTest extends BaseCoreFunctionalTestCase {
	
	public AssociationFormulaTest() {
		super();
	}
	
	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
	
	@Override
	protected boolean rebuildSessionFactoryOnError() {
		return true;
	}

	@Override
	public String[] getMappings() {
		return new String[] { "formulajoin/Mapping.hbm.xml" };
	}
	
	@Before
	public void fillDb() {
		Session s;
		Transaction t;

		Entity entity = new Entity();
		entity.setId(new Id("test", 1));
		entity.setOther(new OtherEntity());
		entity.getOther().setId(new Id("test", 2));
		
		Entity otherNull = new Entity();
		otherNull.setId(new Id("null", 3));

		s = openSession();
		t = s.beginTransaction();
		s.merge(entity);
		s.merge(otherNull);
		t.commit();
		s.close();
	}

	@Test
	public void testJoin() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		Entity loaded = (Entity) s.createQuery("select e from Entity e inner join e.other o").uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(1, loaded.getId().getId());
		assertEquals(2, loaded.getOther().getId().getId());
		assertFalse(Hibernate.isInitialized(loaded.getOther()));
		Hibernate.initialize(loaded.getOther());
		t.commit();
		s.close();
	}

	@Test
	public void testJoinFetch() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		Entity loaded = (Entity) s.createQuery("select e from Entity e inner join fetch e.other o").uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(1, loaded.getId().getId());
		assertTrue(Hibernate.isInitialized(loaded.getOther()));
		assertEquals(2, loaded.getOther().getId().getId());
		t.commit();
		s.close();
	}

	@Test
	public void testSelectFullNull() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		Entity loaded = (Entity) s.createQuery("from Entity e where e.other is null").uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(3, loaded.getId().getId());
		assertNull(loaded.getOther());
		t.commit();
		s.close();
	}

	@Test
	public void testSelectPartialNull() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		Entity loaded = (Entity) s.createQuery("from Entity e where e.other.id.id is null").uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(3, loaded.getId().getId());
		assertNull(loaded.getOther());
		t.commit();
		s.close();
	}

	@Test
	public void testSelectFull() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		OtherEntity other = new OtherEntity();
		other.setId(new Id("test", 2));
		Entity loaded = (Entity) s.createQuery("from Entity e where e.other = :other").setParameter("other", other).uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(1, loaded.getId().getId());
		assertNotNull(loaded.getOther());
		assertEquals(2, loaded.getOther().getId().getId());
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateFromExisting() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		Entity loaded = (Entity) s.createQuery("from Entity e where e.id.id = 1").uniqueResult();
		assertNotNull("loaded", loaded);
		assertNotNull(loaded.getOther());
		loaded.setOther(new OtherEntity());
		loaded.getOther().setId(new Id("test", 3));
		t.commit();
		s.close();
	}

	@Test
	public void testUpdateFromNull() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		Entity loaded = (Entity) s.createQuery("from Entity e where e.id.id = 3").uniqueResult();
		assertNotNull("loaded", loaded);
		assertNull(loaded.getOther());
		loaded.setOther(new OtherEntity());
		loaded.getOther().setId(new Id("test", 3));
		t.commit();
		s.close();
	}

	@Test
	@Ignore("multi-column updates don't work!")
	public void testUpdateHql() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		OtherEntity other = new OtherEntity();
		other.setId(new Id("null", 4));
		assertEquals("execute", 1, s.createQuery("update Entity e set e.other = :other where e.id.id = 3").setParameter("other", other).executeUpdate());
		Entity loaded = (Entity) s.createQuery("from Entity e where e.id.id = 3").uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(4, loaded.getOther().getId().getId());
		t.commit();
		s.close();
	}

	@Test
	@Ignore("multi-column updates don't work!")
	public void testUpdateHqlNull() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		assertEquals("execute", 1, s.createQuery("update Entity e set e.other = null where e.id.id = 1").executeUpdate());
		Entity loaded = (Entity) s.createQuery("from Entity e where e.id.id = 1").uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(4, loaded.getOther().getId().getId());
		t.commit();
		s.close();
	}

	@Test
	public void testDeleteHql() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		OtherEntity other = new OtherEntity();
		other.setId(new Id("test", 2));
		assertEquals("execute", 1, s.createQuery("delete Entity e where e.other = :other").setParameter("other", other).executeUpdate());
		Entity loaded = (Entity) s.createQuery("from Entity e where e.id.id = 1").uniqueResult();
		assertNull("loaded", loaded);
		t.commit();
		s.close();
	}

	@Test
	public void testDeleteHqlNull() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		assertEquals("execute", 1, s.createQuery("delete Entity e where e.other is null").executeUpdate());
		Entity loaded = (Entity) s.createQuery("from Entity e where e.id.id = 3").uniqueResult();
		assertNull("loaded", loaded);
		t.commit();
		s.close();
	}

	@Test
	public void testPersist() {
		Session s;
		Transaction t;

		s = openSession();
		t = s.beginTransaction();
		Entity entity = new Entity();
		entity.setId(new Id("new", 5));
		entity.setOther(new OtherEntity());
		entity.getOther().setId(new Id("new", 6));
		s.persist(entity);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		Entity loaded = (Entity) s.createQuery("from Entity e where e.id.id = 5").uniqueResult();
		assertNotNull("loaded", loaded);
		assertEquals(6, loaded.getOther().getId().getId());
		t.commit();
		s.close();
	}
}
