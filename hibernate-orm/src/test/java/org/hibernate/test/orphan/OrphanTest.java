/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class OrphanTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "orphan/Product.hbm.xml" };
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteOnDelete() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		session.flush();
		
		prod.getParts().remove(part);
		
		session.delete(prod);
		
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNull( session.get(Part.class, "Get") );
		assertNull( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteAfterPersist() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		
		prod.getParts().remove(part);
		
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteAfterPersistAndFlush() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		session.flush();
		
		prod.getParts().remove(part);
		
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteAfterLock() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		t.commit();
		session.close();
		
		
		session = openSession();
		t = session.beginTransaction();
		session.lock( prod, LockMode.READ );
		prod.getParts().remove(part);
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteOnSaveOrUpdate() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		t.commit();
		session.close();
		
		prod.getParts().remove(part);
		
		session = openSession();
		t = session.beginTransaction();
		session.saveOrUpdate(prod);
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteOnSaveOrUpdateAfterSerialization() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		t.commit();
		session.close();
		
		prod.getParts().remove(part);
		
		prod = (Product) SerializationHelper.clone( prod );
		
		session = openSession();
		t = session.beginTransaction();
		session.saveOrUpdate(prod);
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDelete() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		t.commit();
		session.close();
		
		sessionFactory().getCache().evictEntityRegion( Product.class );
		sessionFactory().getCache().evictEntityRegion( Part.class );

		
		session = openSession();
		t = session.beginTransaction();
		prod = (Product) session.get(Product.class, "Widget");
		assertTrue( Hibernate.isInitialized( prod.getParts() ) );
		part = (Part) session.get(Part.class, "Widge");
		prod.getParts().remove(part);
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteOnMerge() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName("Widget");
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		Part part2 = new Part();
		part2.setName("Get");
		part2.setDescription("another part if a Widget");
		prod.getParts().add(part2);
		session.persist(prod);
		t.commit();
		session.close();
		
		prod.getParts().remove(part);
		
		session = openSession();
		t = session.beginTransaction();
		session.merge(prod);
		t.commit();
		session.close();
		
		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get(Part.class, "Widge") );
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	public void testOrphanDeleteOnMergeRemoveElementMerge() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName( "Widget" );
		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);
		session.persist(prod);
		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();
		session.merge(prod);
		prod.getParts().remove( part );
		session.merge( prod );
		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();
		assertNull( session.get( Part.class, "Widge" ) );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	@TestForIssue(jiraKey = "HHH-9171")
	public void testOrphanDeleteOnAddElementMergeRemoveElementMerge() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product();
		prod.setName( "Widget" );
		session.persist(prod);
		t.commit();
		session.close();

		Part part = new Part();
		part.setName("Widge");
		part.setDescription("part if a Widget");
		prod.getParts().add(part);

		session = openSession();
		t = session.beginTransaction();
		session.merge(prod);
		// In Section 2.9, Entity Relationships, the JPA 2.1 spec says:
		// "If the entity being orphaned is a detached, new, or removed entity,
		// the semantics of orphanRemoval do not apply."
		// In other words, since part is a new entity, it will not be deleted when removed
		// from prod.parts, even though cascade for the association includes "delete-orphan".
		prod.getParts().remove(part);
		session.merge( prod );
		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();
		assertNotNull( session.get( Part.class, "Widge" ) );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

}

