/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis.hhh14523;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.junit.Assert.assertEquals;

@JiraKey(value = "HHH-14523")
@RequiresDialect(PostgreSQLDialect.class)
public class DirtyCheckingTest extends BaseEntityManagerFunctionalTestCase {

	private GeometryFactory gfact = new GeometryFactory();

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				TestEntity.class
		};
	}

	public void createtestEntity() {
		Point pnt = (Point) JTS.to( Wkt.fromWkt( "POINT Z( 3.41127795 8.11062269 2.611)", Wkt.Dialect.SFA_1_2_1 ) );
		EntityManager entityManager = createEntityManager();
		TestEntity test1 = new TestEntity( "radar 5", pnt );

		entityManager.getTransaction().begin();
		entityManager.persist( test1 );
		entityManager.getTransaction().commit();

		entityManager.close();
	}

	// Entities are auto-discovered, so just add them anywhere on class-path
	// Add your tests, using standard JUnit.
	@Test
	public void hhh14523() throws Exception {

		createtestEntity();

		EntityManager entityManager = createEntityManager();
		entityManager.getTransaction().begin();
		Query query = entityManager.createQuery( "select t from TestEntity t" );
		TestEntity ent = (TestEntity) query.getResultList().get( 0 );
		Point newPnt = (Point) JTS.to( Wkt.fromWkt( "POINT Z( 3.41127795 8.11062269 8.611)", Wkt.Dialect.SFA_1_2_1 ) );
		ent.setGeom( newPnt );
		entityManager.getTransaction().commit();
		entityManager.close();


		entityManager = createEntityManager();
		entityManager.getTransaction().begin();
		List<TestEntity> entities = entityManager.createQuery( "select t from TestEntity t" ).getResultList();
		TestEntity ent2 = entities.get( 0 );
		try {
			assertEquals( 8.611, ent2.getGeom().getCoordinate().getZ(), 0.00001 );
		}
		finally {
			entityManager.getTransaction().commit();
		}
		entityManager.close();
	}
}

@Entity
@Table(name = "test")
@SequenceGenerator(name = "test_id_seq", sequenceName = "test_id_seq", allocationSize = 1)
class TestEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_id_seq")
	@Column(name = "id")
	private Long id;

	@Column(name = "uid", unique = true)
	private String uid;

	@Column(name = "geom")
	private Point geom;

	public TestEntity() {
	}

	public TestEntity(String uid, Point geom) {
		this.uid = uid;
		this.geom = geom;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public Point getGeom() {
		return geom;
	}

	public void setGeom(Point geom) {
		this.geom = geom;
	}
}
