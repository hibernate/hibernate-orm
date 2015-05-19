/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unionsubclass;

import java.util.List;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@SuppressWarnings("unchecked")
public class UnionSubclassTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "unionsubclass/Beings.hbm.xml" };
	}

	@Test
	public void testUnionSubclassCollection() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Location mel = new Location("Earth");
		s.save(mel);
		
		Human gavin = new Human();
		gavin.setIdentity("gavin");
		gavin.setSex('M');
		gavin.setLocation(mel);
		mel.addBeing(gavin);
		
		gavin.getInfo().put("foo", "bar");
		gavin.getInfo().put("x", "y");
		
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		gavin = (Human) s.createCriteria(Human.class).uniqueResult();
		assertEquals( gavin.getInfo().size(), 2 );
		s.delete(gavin);
		s.delete( gavin.getLocation() );
		t.commit();
		s.close();
	}

	@Test
	public void testUnionSubclassFetchMode() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Location mel = new Location("Earth");
		s.save(mel);
		
		Human gavin = new Human();
		gavin.setIdentity("gavin");
		gavin.setSex('M');
		gavin.setLocation(mel);
		mel.addBeing(gavin);
		Human max = new Human();
		max.setIdentity("max");
		max.setSex('M');
		max.setLocation(mel);
		mel.addBeing(gavin);
		
		s.flush();
		s.clear();
		
		List list = s.createCriteria(Human.class)
			.setFetchMode("location", FetchMode.JOIN)
			.setFetchMode("location.beings", FetchMode.JOIN)
			.list();

		for ( Object aList : list ) {
			Human h = (Human) aList;
			assertTrue( Hibernate.isInitialized( h.getLocation() ) );
			assertTrue( Hibernate.isInitialized( h.getLocation().getBeings() ) );
			s.delete( h );
		}
		s.delete( s.get( Location.class, mel.getId() ) );
		t.commit();
		s.close();
	}
	
	@Test
	public void testUnionSubclassOneToMany() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Location mel = new Location("Melbourne, Australia");
		Location mars = new Location("Mars");
		s.save(mel);
		s.save(mars);
		
		Human gavin = new Human();
		gavin.setIdentity("gavin");
		gavin.setSex('M');
		gavin.setLocation(mel);
		mel.addBeing(gavin);
		
		Alien x23y4 = new Alien();
		x23y4.setIdentity("x23y4$$hu%3");
		x23y4.setLocation(mars);
		x23y4.setSpecies("martian");
		mars.addBeing(x23y4);
		
		Alien yy3dk = new Alien();
		yy3dk.setIdentity("yy3dk&*!!!");
		yy3dk.setLocation(mars);
		yy3dk.setSpecies("martian");
		mars.addBeing(yy3dk);
		
		Hive hive = new Hive();
		hive.setLocation(mars);
		hive.getMembers().add(x23y4);
		x23y4.setHive(hive);
		hive.getMembers().add(yy3dk);
		yy3dk.setHive(hive);
		s.persist(hive);
		
		yy3dk.getHivemates().add(x23y4);
		x23y4.getHivemates().add(yy3dk);
		
		s.flush();
		s.clear();
		
		hive = (Hive) s.createQuery("from Hive h").uniqueResult();
		assertFalse( Hibernate.isInitialized( hive.getMembers() ) );
		assertEquals( hive.getMembers().size(), 2 );
		
		s.clear();
		
		hive = (Hive) s.createQuery("from Hive h left join fetch h.location left join fetch h.members").uniqueResult();
		assertTrue( Hibernate.isInitialized( hive.getMembers() ) );
		assertEquals( hive.getMembers().size(), 2 );
		
		s.clear();
		
		x23y4 = (Alien) s.createQuery("from Alien a left join fetch a.hivemates where a.identity like 'x%'").uniqueResult();
		assertTrue( Hibernate.isInitialized( x23y4.getHivemates() ) );
		assertEquals( x23y4.getHivemates().size(), 1 );
		
		s.clear();
		
		x23y4 = (Alien) s.createQuery("from Alien a where a.identity like 'x%'").uniqueResult();
		assertFalse( Hibernate.isInitialized( x23y4.getHivemates() ) );
		assertEquals( x23y4.getHivemates().size(), 1 );
		
		s.clear();
		
		x23y4 = (Alien) s.createCriteria(Alien.class).addOrder( Order.asc("identity") ).list().get(0);
		s.delete( x23y4.getHive() );
		s.delete( s.get(Location.class, mel.getId() ) );
		s.delete( s.get(Location.class, mars.getId() ) );
		assertTrue( s.createQuery("from Being").list().isEmpty() );
		t.commit();
		s.close();
	}

	@Test
	public void testUnionSubclassManyToOne() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Location mel = new Location("Melbourne, Australia");
		Location mars = new Location("Mars");
		s.save(mel);
		s.save(mars);
		
		Human gavin = new Human();
		gavin.setIdentity("gavin");
		gavin.setSex('M');
		gavin.setLocation(mel);
		mel.addBeing(gavin);
		
		Alien x23y4 = new Alien();
		x23y4.setIdentity("x23y4$$hu%3");
		x23y4.setLocation(mars);
		x23y4.setSpecies("martian");
		mars.addBeing(x23y4);
		
		Hive hive = new Hive();
		hive.setLocation(mars);
		hive.getMembers().add(x23y4);
		x23y4.setHive(hive);
		s.persist(hive);
		
		Thing thing = new Thing();
		thing.setDescription("some thing");
		thing.setOwner(gavin);
		gavin.getThings().add(thing);
		s.save(thing);
		s.flush();
		
		s.clear();
		
		thing = (Thing) s.createQuery("from Thing t left join fetch t.owner").uniqueResult();
		assertTrue( Hibernate.isInitialized( thing.getOwner() ) );
		assertEquals( thing.getOwner().getIdentity(), "gavin" );
		s.clear();
		
		thing = (Thing) s.createQuery("select t from Thing t left join t.owner where t.owner.identity='gavin'").uniqueResult();
		assertFalse( Hibernate.isInitialized( thing.getOwner() ) );
		assertEquals( thing.getOwner().getIdentity(), "gavin" );
		s.clear();
		
		gavin = (Human) s.createQuery("from Human h left join fetch h.things").uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getThings() ) );
		assertEquals( ( (Thing) gavin.getThings().get(0) ).getDescription(), "some thing" );
		s.clear();
		
		assertTrue( s.createQuery("from Being b left join fetch b.things").list().size()==2 );
		s.clear();
		
		gavin = (Human) s.createQuery("from Being b join fetch b.things").uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getThings() ) );
		assertEquals( ( (Thing) gavin.getThings().get(0) ).getDescription(), "some thing" );
		s.clear();
		
		gavin = (Human) s.createQuery("select h from Human h join h.things t where t.description='some thing'").uniqueResult();
		assertFalse( Hibernate.isInitialized( gavin.getThings() ) );
		assertEquals( ( (Thing) gavin.getThings().get(0) ).getDescription(), "some thing" );
		s.clear();
		
		gavin = (Human) s.createQuery("select b from Being b join b.things t where t.description='some thing'").uniqueResult();
		assertFalse( Hibernate.isInitialized( gavin.getThings() ) );
		assertEquals( ( (Thing) gavin.getThings().get(0) ).getDescription(), "some thing" );
		s.clear();
		
		thing = (Thing) s.get( Thing.class, thing.getId() );
		assertFalse( Hibernate.isInitialized( thing.getOwner() ) );
		assertEquals( thing.getOwner().getIdentity(), "gavin" );
		
		thing.getOwner().getThings().remove(thing);
		thing.setOwner(x23y4);
		x23y4.getThings().add(thing);
		
		s.flush();
		
		s.clear();

		thing = (Thing) s.get( Thing.class, thing.getId() );
		assertFalse( Hibernate.isInitialized( thing.getOwner() ) );
		assertEquals( thing.getOwner().getIdentity(), "x23y4$$hu%3" );
		
		s.delete(thing);
		x23y4 = (Alien) s.createCriteria(Alien.class).uniqueResult();
		s.delete( x23y4.getHive() );
		s.delete( s.get(Location.class, mel.getId() ) );
		s.delete( s.get(Location.class, mars.getId() ) );
		assertTrue( s.createQuery("from Being").list().isEmpty() );
		t.commit();
		s.close();
	}

	@Test
	public void testUnionSubclass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Location mel = new Location("Melbourne, Australia");
		Location atl = new Location("Atlanta, GA");
		Location mars = new Location("Mars");
		s.save(mel); 
		s.save(atl); 
		s.save(mars);
		
		Human gavin = new Human();
		gavin.setIdentity("gavin");
		gavin.setSex('M');
		gavin.setLocation(mel);
		mel.addBeing(gavin);
		
		Alien x23y4 = new Alien();
		x23y4.setIdentity("x23y4$$hu%3");
		x23y4.setLocation(mars);
		x23y4.setSpecies("martian");
		mars.addBeing(x23y4);
		
		Hive hive = new Hive();
		hive.setLocation(mars);
		hive.getMembers().add(x23y4);
		x23y4.setHive(hive);
		s.persist(hive);
		
		assertEquals( s.createQuery("from Being").list().size(), 2 );
		assertEquals( s.createQuery("from Being b where b.class = Alien").list().size(), 1 );
		assertEquals( s.createQuery("from Being b where type(b) = :what").setParameter("what", Alien.class).list().size(), 1 );
		assertEquals( s.createQuery("from Being b where type(b) in :what").setParameterList("what", new Class[] { Alien.class, Human.class }).list().size(), 2 );
		assertEquals( s.createQuery("from Alien").list().size(), 1 );
		s.clear();

		List beings = s.createQuery("from Being b left join fetch b.location").list();
		for ( Object being : beings ) {
			Being b = (Being) being;
			assertTrue( Hibernate.isInitialized( b.getLocation() ) );
			assertNotNull( b.getLocation().getName() );
			assertNotNull( b.getIdentity() );
			assertNotNull( b.getSpecies() );
		}
		assertEquals( beings.size(), 2 );
		s.clear();
		
		beings = s.createQuery("from Being").list();
		for ( Object being : beings ) {
			Being b = (Being) being;
			assertFalse( Hibernate.isInitialized( b.getLocation() ) );
			assertNotNull( b.getLocation().getName() );
			assertNotNull( b.getIdentity() );
			assertNotNull( b.getSpecies() );
		}
		assertEquals( beings.size(), 2 );
		s.clear();
		
		List locations = s.createQuery("from Location").list(); 
		int count = 0;
		for ( Object location : locations ) {
			Location l = (Location) location;
			assertNotNull( l.getName() );
			for ( Object o : l.getBeings() ) {
				count++;
				assertSame( ( (Being) o ).getLocation(), l );
			}
		}
		assertEquals(count, 2);
		assertEquals( locations.size(), 3 );
		s.clear();

		locations = s.createQuery("from Location loc left join fetch loc.beings").list(); 
		count = 0;
		for ( Object location : locations ) {
			Location l = (Location) location;
			assertNotNull( l.getName() );
			for ( Object o : l.getBeings() ) {
				count++;
				assertSame( ( (Being) o ).getLocation(), l );
			}
		}
		assertEquals(count, 2);
		assertEquals( locations.size(), 3 );
		s.clear();

		gavin = (Human) s.get( Human.class, gavin.getId() );
		atl = (Location) s.get( Location.class, atl.getId() );
		
 		atl.addBeing(gavin);
		assertEquals( s.createQuery("from Human h where h.location.name like '%GA'").list().size(), 1 );
		s.delete(gavin);
		x23y4 = (Alien) s.createCriteria(Alien.class).uniqueResult();
		s.delete( x23y4.getHive() );
		assertTrue( s.createQuery("from Being").list().isEmpty() );
		
		s.createQuery("delete from Location").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testNestedUnionedSubclasses() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Location mel = new Location("Earth");
		Human marcf = new Human();
		marcf.setIdentity("marc");
		marcf.setSex('M');
		mel.addBeing(marcf);
		Employee steve = new Employee();
		steve.setIdentity("steve");
		steve.setSex('M');
		steve.setSalary( (double) 0 );
		mel.addBeing(steve);
		s.persist(mel);
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from Being h where h.identity = :name1 or h.identity = :name2" );
		q.setString("name1", "marc");
		q.setString("name2", "steve");
		final List result = q.list();
		assertEquals( 2, result.size() );
		s.delete( result.get(0) );
		s.delete( result.get(1) );
		s.delete( ( (Human) result.get(0) ).getLocation() );
		tx.commit();
		s.close();
	}

}

