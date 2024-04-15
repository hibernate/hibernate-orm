/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.integration.jts.hhh14523;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.geolatte.geom.codec.Wkt;
import org.geolatte.geom.jts.JTS;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-14523")
@RequiresDialect(PostgisPG95Dialect.class)
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
