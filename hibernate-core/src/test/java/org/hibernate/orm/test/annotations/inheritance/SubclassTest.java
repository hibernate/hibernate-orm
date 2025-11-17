/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance;

import java.util.List;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.annotations.inheritance.singletable.Funk;
import org.hibernate.orm.test.annotations.inheritance.singletable.Music;
import org.hibernate.orm.test.annotations.inheritance.singletable.Noise;
import org.hibernate.orm.test.annotations.inheritance.singletable.Rock;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.A320;
import org.hibernate.orm.test.annotations.A320b;
import org.hibernate.orm.test.annotations.Plane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				A320b.class, //subclasses should be properly reordered
				Plane.class,
				A320.class,
				Apple.class,
				Music.class,
				Rock.class,
				Funk.class,
				Noise.class
		}
)
@SessionFactory
public class SubclassTest {

	@Test
	public void testPolymorphism(SessionFactoryScope scope) {
		scope.inTransaction(
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

		scope.inTransaction(
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
	public void test2ndLevelSubClass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					A320b a = new A320b();
					a.setJavaEmbeddedVersion( "Elephant" );
					a.setNbrOfSeats( 300 );
					s.persist( a );
				}
		);

		scope.inTransaction(
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
	public void testEmbeddedSuperclass(SessionFactoryScope scope) {
		Plane plane = new Plane();
		scope.inTransaction(
				s -> {
					plane.setAlive( true ); //sic
					plane.setAltitude( 10000 );
					plane.setMetricAltitude( 3000 );
					plane.setNbrOfSeats( 150 );
					plane.setSerial( "0123456789" );
					s.persist( plane );
				}
		);

		scope.inTransaction(
				s -> {
					Plane p = s.get( Plane.class, plane.getId() );
					assertNotNull( p );
					assertTrue( p.isAlive() );
					assertEquals( 150, p.getNbrOfSeats() );
					assertEquals( 10000, p.getAltitude() );
					assertEquals( "0123456789", p.getSerial() );
					assertNotEquals( 3000, p.getMetricAltitude() );
					s.remove( p );

				}
		);
	}

	@Test
	public void testFormula(SessionFactoryScope scope) {
		scope.inTransaction(
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

		scope.inTransaction(
				s -> {
					List result = createQueryForClass( s, Noise.class ).list();
					assertNotNull( result );
					assertEquals( 1, result.size() );
					Noise w = (Noise) result.get( 0 );
					assertNull( w.getType() );
					s.remove( w );
					result = createQueryForClass( s, Rock.class ).list();
					assertEquals( 1, result.size() );
					s.remove( result.get( 0 ) );
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

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
