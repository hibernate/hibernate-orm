/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance;

import java.util.List;

import org.junit.Test;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.A320;
import org.hibernate.test.annotations.A320b;
import org.hibernate.test.annotations.Plane;
import org.hibernate.test.annotations.inheritance.singletable.Funk;
import org.hibernate.test.annotations.inheritance.singletable.Music;
import org.hibernate.test.annotations.inheritance.singletable.Noise;
import org.hibernate.test.annotations.inheritance.singletable.Rock;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class SubclassTest extends BaseCoreFunctionalTestCase {
	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
	@Test
	public void testPolymorphism() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Plane p = new Plane();
		p.setNbrOfSeats( 10 );
		A320 a = new A320();
		a.setJavaEmbeddedVersion( "5.0" );
		a.setNbrOfSeats( 300 );
		s.persist( a );
		s.persist( p );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from " + A320.class.getName() );
		List a320s = q.list();
		assertNotNull( a320s );
		assertEquals( 1, a320s.size() );
		assertTrue( a320s.get( 0 ) instanceof A320 );
		assertEquals( "5.0", ( (A320) a320s.get( 0 ) ).getJavaEmbeddedVersion() );
		q = s.createQuery( "from " + Plane.class.getName() );
		List planes = q.list();
		assertNotNull( planes );
		assertEquals( 2, planes.size() );
		tx.commit();
		s.close();
	}

	@Test
	public void test2ndLevelSubClass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		A320b a = new A320b();
		a.setJavaEmbeddedVersion( "Elephant" );
		a.setNbrOfSeats( 300 );
		s.persist( a );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "from " + A320.class.getName() + " as a where a.javaEmbeddedVersion = :version" );
		q.setString( "version", "Elephant" );
		List a320s = q.list();
		assertNotNull( a320s );
		assertEquals( 1, a320s.size() );
		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddedSuperclass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Plane p = new Plane();
		p.setAlive( true ); //sic
		p.setAltitude( 10000 );
		p.setMetricAltitude( 3000 );
		p.setNbrOfSeats( 150 );
		p.setSerial( "0123456789" );
		s.persist( p );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		p = (Plane) s.get( Plane.class, p.getId() );
		assertNotNull( p );
		assertEquals( true, p.isAlive() );
		assertEquals( 150, p.getNbrOfSeats() );
		assertEquals( 10000, p.getAltitude() );
		assertEquals( "0123456789", p.getSerial() );
		assertFalse( 3000 == p.getMetricAltitude() );
		s.delete( p );
		tx.commit();
		s.close();
	}

	@Test
	public void testFormula() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Rock guns = new Rock();
		guns.setAvgBeat( 90 );
		guns.setType( 2 );
		Noise white = new Noise();
		white.setAvgBeat( 0 );
		white.setType( null );

		s.persist( guns );
		s.persist( white );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		List result = s.createCriteria( Noise.class ).list();
		assertNotNull( result );
		assertEquals( 1, result.size() );
		white = (Noise) result.get( 0 );
		assertNull( white.getType() );
		s.delete( white );
		result = s.createCriteria( Rock.class ).list();
		assertEquals( 1, result.size() );
		s.delete( result.get( 0 ) );
		result = s.createCriteria( Funk.class ).list();
		assertEquals( 0, result.size() );

		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				A320b.class, //subclasses should be properly reordered
				Plane.class,
				A320.class,
				Fruit.class,
				//FlyingObject.class, //had to declare embedded superclasses
				//Thing.class,
				Apple.class,
				Music.class,
				Rock.class,
				Funk.class,
				Noise.class
		};
	}

}
