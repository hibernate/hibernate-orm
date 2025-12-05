/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.postgis.hhh14523;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;

import java.io.Serializable;
import java.util.List;

import static org.junit.Assert.assertEquals;

@JiraKey(value = "HHH-14523")
@RequiresDialect(PostgreSQLDialect.class)
@Jpa(
		annotatedClasses = {
				TestEntity.class
		}
)
public class DirtyCheckingTest {

	@BeforeAll
	public void createtestEntity(EntityManagerFactoryScope scope) {
		Point pnt = createPoint( "POINT Z( 3.41127795 8.11062269 2.611)" );
		scope.inTransaction(
				entityManager ->
						entityManager.persist( new TestEntity( "radar 5", pnt ) )
		);
	}

	// Entities are auto-discovered, so just add them anywhere on class-path
	// Add your tests, using standard JUnit.
	@Test
	public void hhh14523(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TypedQuery<TestEntity> query = entityManager
							.createQuery( "select t from TestEntity t", TestEntity.class );
					TestEntity ent = query.getResultList().get( 0 );
					ent.setGeom( createPoint( "POINT Z( 3.41127795 8.11062269 8.611)" ) );
				}
		);

		scope.inTransaction(
				entityManager -> {
					List<TestEntity> entities = entityManager
							.createQuery( "select t from TestEntity t", TestEntity.class )
							.getResultList();
					TestEntity ent2 = entities.get( 0 );
					assertEquals( 8.611, ent2.getGeom().getCoordinate().getZ(), 0.00001 );
				}
		);
	}

	private static Point createPoint(String wkt) {
		return (Point) JTS.to( Wkt.fromWkt( wkt, Wkt.Dialect.SFA_1_2_1 ) );
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
