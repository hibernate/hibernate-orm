/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.Test;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;

import org.hibernate.test.annotations.A320;
import org.hibernate.test.annotations.A320b;
import org.hibernate.test.annotations.Plane;
import org.hibernate.test.annotations.inheritance.singletable.Funk;
import org.hibernate.test.annotations.inheritance.singletable.Music;
import org.hibernate.test.annotations.inheritance.singletable.Noise;
import org.hibernate.test.annotations.inheritance.singletable.Rock;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
	public void testPolymorphism() {
		inTransaction(
				s -> {
					Plane p = new Plane();
					p.setNbrOfSeats( 10 );
					A320 a = new A320();
					a.setJavaEmbeddedVersion( "5.0" );
					a.setNbrOfSeats( 300 );
					s.persist( a );
					s.persist( p );
				}
		);

		inTransaction(
				s -> {
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
				}
		);
	}

	@Test
	public void test2ndLevelSubClass() {
		inTransaction(
				s -> {
					A320b a = new A320b();
					a.setJavaEmbeddedVersion( "Elephant" );
					a.setNbrOfSeats( 300 );
					s.persist( a );
				}
		);

		inTransaction(
				s -> {
					Query q = s.createQuery( "from " + A320.class.getName() + " as a where a.javaEmbeddedVersion = :version" );
					q.setParameter( "version", "Elephant" );
					List a320s = q.list();
					assertNotNull( a320s );
					assertEquals( 1, a320s.size() );

				}
		);
	}

	@Test
	public void testEmbeddedSuperclass() {
		Plane plane = new Plane();
		inTransaction(
				s -> {
					plane.setAlive( true ); //sic
					plane.setAltitude( 10000 );
					plane.setMetricAltitude( 3000 );
					plane.setNbrOfSeats( 150 );
					plane.setSerial( "0123456789" );
					s.persist( plane );
				}
		);

		inTransaction(
				s -> {
					Plane p = s.get( Plane.class, plane.getId() );
					assertNotNull( p );
					assertTrue( p.isAlive() );
					assertEquals( 150, p.getNbrOfSeats() );
					assertEquals( 10000, p.getAltitude() );
					assertEquals( "0123456789", p.getSerial() );
					assertNotEquals( 3000, p.getMetricAltitude() );
					s.delete( p );

				}
		);
	}

	@Test
	public void testFormula() {
		inTransaction(
				s -> {
					Rock guns = new Rock();
					guns.setAvgBeat( 90 );
					guns.setType( 2 );
					Noise white = new Noise();
					white.setAvgBeat( 0 );
					white.setType( null );

					s.persist( guns );
					s.persist( white );
				}
		);

		inTransaction(
				s -> {
					List result = createQueryForClass( s, Noise.class ).list();
					assertNotNull( result );
					assertEquals( 1, result.size() );
					Noise w = (Noise) result.get( 0 );
					assertNull( w.getType() );
					s.delete( w );
					result = createQueryForClass( s, Rock.class ).list();
					assertEquals( 1, result.size() );
					s.delete( result.get( 0 ) );
					result = createQueryForClass( s, Funk.class ).list();
					assertEquals( 0, result.size() );
				}
		);
	}

	private Query createQueryForClass(SessionImplementor session, Class clazz) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery criteria = criteriaBuilder.createQuery( clazz );
		criteria.from( clazz );
		return session.createQuery( criteria );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
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
