/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = PGVectorTest.VectorEntity.class)
@SessionFactory
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@RequiresDialect(value = CockroachDialect.class, majorVersion = 24, minorVersion = 2)
public class PGVectorTest {

	private static final float[] V1 = new float[]{ 1, 2, 3 };
	private static final float[] V2 = new float[]{ 4, 5, 6 };

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new VectorEntity( 1L, V1 ) );
			em.persist( new VectorEntity( 2L, V2 ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createMutationQuery( "delete from VectorEntity" ).executeUpdate();
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB does not currently support the sum() function on vector type" )
	public void testVectorSum(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::vector-sum-example[]
			final List<float[]> results = em.createSelectionQuery( "select sum(e.theVector) from VectorEntity e", float[].class )
					.getResultList();
			//end::vector-sum-example[]
			assertEquals( 1, results.size() );
			assertArrayEquals( new float[]{ 5, 7, 9 }, results.get( 0 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB does not currently support the avg() function on vector type" )
	public void testVectorAvg(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::vector-avg-example[]
			final List<float[]> results = em.createSelectionQuery( "select avg(e.theVector) from VectorEntity e", float[].class )
					.getResultList();
			//end::vector-avg-example[]
			assertEquals( 1, results.size() );
			assertArrayEquals( new float[]{ 2.5f, 3.5f, 4.5f }, results.get( 0 ) );
		} );
	}

	@Test
	public void testAddition(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::vector-addition-example[]
			final List<Tuple> results = em.createSelectionQuery( "select e.id, e.theVector + cast('[1, 1, 1]' as vector) from VectorEntity e order by e.id", Tuple.class )
					.getResultList();
			//end::vector-addition-example[]
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new float[]{ 2, 3, 4 }, results.get( 0 ).get( 1, float[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new float[]{ 5, 6, 7 }, results.get( 1 ).get( 1, float[].class ) );
		} );
	}

	@Test
	public void testMultiplication(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::vector-multiplication-example[]
			final List<Tuple> results = em.createSelectionQuery( "select e.id, e.theVector * cast('[2, 2, 2]' as vector) from VectorEntity e order by e.id", Tuple.class )
					.getResultList();
			//end::vector-multiplication-example[]
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new float[]{ 2, 4, 6 }, results.get( 0 ).get( 1, float[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new float[]{ 8, 10, 12 }, results.get( 1 ).get( 1, float[].class ) );
		} );
	}

	@Entity( name = "VectorEntity" )
	public static class VectorEntity {

		@Id
		private Long id;

		@Column( name = "the_vector" )
		@JdbcTypeCode(SqlTypes.VECTOR)
		@Array(length = 3)
		private float[] theVector;

		public VectorEntity() {
		}

		public VectorEntity(Long id, float[] theVector) {
			this.id = id;
			this.theVector = theVector;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public float[] getTheVector() {
			return theVector;
		}

		public void setTheVector(float[] theVector) {
			this.theVector = theVector;
		}
	}
}
