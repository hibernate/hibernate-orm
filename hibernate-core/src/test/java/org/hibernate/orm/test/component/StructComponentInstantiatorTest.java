/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component;

import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Instantiator;
import org.hibernate.annotations.Struct;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = StructComponentInstantiatorTest.RecordStructHolder.class)
@SessionFactory
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
public class StructComponentInstantiatorTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new RecordStructHolder( 1L, Point.createAggregate1() ) );
			session.persist( new RecordStructHolder( 2L, Point.createAggregate2() ) );
		} );
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			RecordStructHolder RecordStructHolder = entityManager.find( RecordStructHolder.class, 1L );
			RecordStructHolder.setThePoint( Point.createAggregate2() );
			entityManager.flush();
			entityManager.clear();
			assertStructEquals(
					Point.createAggregate2(),
					entityManager.find( RecordStructHolder.class, 1L ).getThePoint()
			);
		} );
	}

	@Test
	public void testFetch(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<RecordStructHolder> RecordStructHolders = entityManager.createQuery(
					"from RecordStructHolder b where b.id = 1",
					RecordStructHolder.class
			).getResultList();
			assertEquals( 1, RecordStructHolders.size() );
			assertEquals( 1L, RecordStructHolders.get( 0 ).getId() );
			assertStructEquals( Point.createAggregate1(), RecordStructHolders.get( 0 ).getThePoint() );
		} );
	}

	@Test
	public void testFetchNull(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<RecordStructHolder> RecordStructHolders = entityManager.createQuery(
					"from RecordStructHolder b where b.id = 2",
					RecordStructHolder.class
			).getResultList();
			assertEquals( 1, RecordStructHolders.size() );
			assertEquals( 2L, RecordStructHolders.get( 0 ).getId() );
			assertStructEquals( Point.createAggregate2(), RecordStructHolders.get( 0 ).getThePoint() );
		} );
	}

	@Test
	public void testDomainResult(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Point> structs = entityManager.createQuery(
					"select b.thePoint from RecordStructHolder b where b.id = 1",
					Point.class
			).getResultList();
			assertEquals( 1, structs.size() );
			assertStructEquals( Point.createAggregate1(), structs.get( 0 ) );
		} );
	}

	@Test
	public void testSelectionItems(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			List<Tuple> tuples = entityManager.createQuery(
					"select " +
							"b.thePoint.x," +
							"b.thePoint.y," +
							"b.thePoint.z " +
							"from RecordStructHolder b where b.id = 1",
					Tuple.class
			).getResultList();
			assertEquals( 1, tuples.size() );
			final Tuple tuple = tuples.get( 0 );
			assertStructEquals(
					Point.createAggregate1(),
					new Point(
							tuple.get( 1, String.class ),
							tuple.get( 2, long.class ),
							tuple.get( 0, int.class )
					)
			);
		} );
	}

	@Test
	public void testDeleteWhere(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete RecordStructHolder b where b.thePoint is not null" ).executeUpdate();
			assertNull( entityManager.find( RecordStructHolder.class, 1L ) );
		} );
	}

	@Test
	public void testUpdateAggregate(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "update RecordStructHolder b set b.thePoint = null" ).executeUpdate();
			assertNull( entityManager.find( RecordStructHolder.class, 1L ).getThePoint() );
		} );
	}

	@Test
	public void testUpdateAggregateMember(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "update RecordStructHolder b set b.thePoint.x = null" ).executeUpdate();
			Point struct = Point.createAggregate1().withX( null );
			assertStructEquals( struct, entityManager.find( RecordStructHolder.class, 1L ).getThePoint() );
		} );
	}

	@Test
	public void testUpdateMultipleAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "update RecordStructHolder b set b.thePoint.y = null, b.thePoint.z = 0" )
					.executeUpdate();
			Point struct = Point.createAggregate1().withY( null ).withZ( 0 );
			assertStructEquals( struct, entityManager.find( RecordStructHolder.class, 1L ).getThePoint() );
		} );
	}

	@Test
	public void testUpdateAllAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Point struct = Point.createAggregate1();
			entityManager.createQuery(
							"update RecordStructHolder b set " +
									"b.thePoint.x = :x," +
									"b.thePoint.y = :y," +
									"b.thePoint.z = :z " +
									"where b.id = 2"
					)
					.setParameter( "x", struct.getX() )
					.setParameter( "y", struct.getY() )
					.setParameter( "z", struct.getZ() )
					.executeUpdate();
			assertStructEquals(
					Point.createAggregate1(),
					entityManager.find( RecordStructHolder.class, 2L ).getThePoint()
			);
		} );
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//noinspection unchecked
			List<Object> resultList = entityManager.createNativeQuery(
							"select b.thePoint from RecordStructHolder b where b.id = 1",
							Object.class
					)
					.getResultList();
			assertEquals( 1, resultList.size() );
			assertInstanceOf( Point.class, resultList.get( 0 ) );
			Point struct = (Point) resultList.get( 0 );
			assertStructEquals( Point.createAggregate1(), struct );
		} );
	}

	private void assertStructEquals(Point point1, Point point2) {
		assertEquals( point1.getX(), point2.getX() );
		assertEquals( point1.getY(), point2.getY() );
		assertEquals( point1.getZ(), point2.getZ() );
	}

	@Entity(name = "RecordStructHolder")
	public static class RecordStructHolder {

		@Id
		private Long id;
		private Point thePoint;

		public RecordStructHolder() {
		}

		public RecordStructHolder(Long id, Point thePoint) {
			this.id = id;
			this.thePoint = thePoint;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Point getThePoint() {
			return thePoint;
		}

		public void setThePoint(Point aggregate) {
			this.thePoint = aggregate;
		}

	}

	@Embeddable
	@Struct(name = "my_point_type", attributes = { "y", "x", "z" })
	public static class Point {

		private final String y;
		private final long z;
		private final Integer x;

		@Instantiator({ "y", "z", "x" })
		public Point(String y, long z, Integer x) {
			this.y = y;
			this.x = x;
			this.z = z;
		}

		public String getY() {
			return y;
		}

		public Integer getX() {
			return x;
		}

		public long getZ() {
			return z;
		}

		public Point withX(Integer x) {
			return new Point( y, z, x );
		}

		public Point withY(String y) {
			return new Point( y, z, x );
		}

		public Point withZ(long z) {
			return new Point( y, z, x );
		}

		public static Point createAggregate1() {
			return new Point( "1", -100, 10 );
		}

		public static Point createAggregate2() {
			return new Point( "20", -200, 2 );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Point point = (Point) o;

			if ( !Objects.equals( y, point.y ) ) {
				return false;
			}
			if ( !Objects.equals( x, point.x ) ) {
				return false;
			}
			return Objects.equals( z, point.z );
		}

		@Override
		public int hashCode() {
			int result = y != null ? y.hashCode() : 0;
			result = 31 * result + (int) ( z ^ ( z >>> 32 ) );
			result = 31 * result + ( x != null ? x.hashCode() : 0 );
			return result;
		}
	}
}
