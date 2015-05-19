/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.backref.map.compkey;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.internal.util.SerializationHelper;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * BackrefCompositeMapKeyTest implementation.  Test access to a composite map-key
 * backref via a number of different access methods.
 *
 * @author Steve Ebersole
 */
public class BackrefCompositeMapKeyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "collection/backref/map/compkey/Mappings.hbm.xml" };
	}

	@Test
	public void testOrphanDeleteOnDelete() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey( "Bottom" ), part2 );
		session.persist( prod );
		session.flush();

		prod.getParts().remove( mapKey );

		session.delete( prod );

		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();
		assertNull( "Orphan 'Widge' was not deleted", session.get(Part.class, "Widge") );
		assertNull( "Orphan 'Get' was not deleted", session.get(Part.class, "Get") );
		assertNull( "Orphan 'Widget' was not deleted", session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	public void testOrphanDeleteAfterPersist() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey( "Bottom" ), part2 );
		session.persist( prod );

		prod.getParts().remove( mapKey );

		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	public void testOrphanDeleteAfterPersistAndFlush() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey( "Bottom" ), part2 );
		session.persist( prod );
		session.flush();

		prod.getParts().remove( mapKey );

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
	public void testOrphanDeleteAfterLock() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey( "Bottom" ),part2 );
		session.persist( prod );
		t.commit();
		session.close();


		session = openSession();
		t = session.beginTransaction();
		session.lock(prod, LockMode.READ);
		prod.getParts().remove(mapKey);
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
	public void testOrphanDeleteOnSaveOrUpdate() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey( "Bottom" ), part2 );
		session.persist( prod );
		t.commit();
		session.close();

		prod.getParts().remove( mapKey );

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
	public void testOrphanDeleteOnSaveOrUpdateAfterSerialization() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey( "Bottom" ), part2 );
		session.persist( prod );
		t.commit();
		session.close();

		prod.getParts().remove( mapKey );

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
	public void testOrphanDelete() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey( "Bottom" ), part2 );
		session.persist( prod );
		t.commit();
		session.close();

		sessionFactory().getCache().evictEntityRegion(Product.class);
		sessionFactory().getCache().evictEntityRegion(Part.class);

		session = openSession();
		t = session.beginTransaction();
		prod = (Product) session.get(Product.class, "Widget");
		assertTrue( Hibernate.isInitialized( prod.getParts() ) );
		part = (Part) session.get(Part.class, "Widge");
		prod.getParts().remove(mapKey);
		t.commit();
		session.close();

		sessionFactory().getCache().evictEntityRegion( Product.class );
		sessionFactory().getCache().evictEntityRegion(Part.class);

		session = openSession();
		t = session.beginTransaction();
		prod = (Product) session.get(Product.class, "Widget");
		assertTrue( Hibernate.isInitialized( prod.getParts() ) );
		assertNull( prod.getParts().get(new MapKey("Top")));
		assertNotNull( session.get(Part.class, "Get") );
		session.delete( session.get(Product.class, "Widget") );
		t.commit();
		session.close();
	}

	@Test
	public void testOrphanDeleteOnMerge() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Product prod = new Product( "Widget" );
		Part part = new Part( "Widge", "part if a Widget" );
		MapKey mapKey = new MapKey( "Top" );
		prod.getParts().put( mapKey, part );
		Part part2 = new Part( "Get", "another part if a Widget" );
		prod.getParts().put( new MapKey("Bottom"), part2 );
		session.persist( prod );
		t.commit();
		session.close();

		prod.getParts().remove( mapKey );

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
}
