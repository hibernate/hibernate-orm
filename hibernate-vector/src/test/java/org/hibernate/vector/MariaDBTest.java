/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Diego Dupin
 */
@DomainModel(annotatedClasses = MariaDBTest.VectorEntity.class)
@SessionFactory
@RequiresDialect(value = MariaDBDialect.class, matchSubTypes = false, majorVersion = 11, minorVersion = 7)
public class MariaDBTest {

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
	public void testRead(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			VectorEntity tableRecord;
			tableRecord = em.find( VectorEntity.class, 1L );
			assertArrayEquals( new float[]{ 1, 2, 3 }, tableRecord.getTheVector(), 0 );

			tableRecord = em.find( VectorEntity.class, 2L );
			assertArrayEquals( new float[]{ 4, 5, 6 }, tableRecord.getTheVector(), 0 );
		} );
	}

	@Test
	public void testCosineDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::cosine-distance-example[]
			final float[] vector = new float[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, cosine_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			//end::cosine-distance-example[]
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( cosineDistance( V1, vector ), results.get( 0 ).get( 1, Double.class ), 0.0000000000000002D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( cosineDistance( V2, vector ), results.get( 1 ).get( 1, Double.class ), 0.0000000000000002D );
		} );
	}

	@Test
	public void testEuclideanDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::euclidean-distance-example[]
			final float[] vector = new float[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, euclidean_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			//end::euclidean-distance-example[]
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( euclideanDistance( V1, vector ), results.get( 0 ).get( 1, Double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( euclideanDistance( V2, vector ), results.get( 1 ).get( 1, Double.class ), 0D );
		} );
	}

	private static double cosineDistance(float[] f1, float[] f2) {
		return 1D - innerProduct( f1, f2 ) / ( euclideanNorm( f1 ) * euclideanNorm( f2 ) );
	}

	private static double euclideanDistance(float[] f1, float[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += Math.pow( (double) f1[i] - f2[i], 2 );
		}
		return Math.sqrt( result );
	}

	private static double innerProduct(float[] f1, float[] f2) {
		assert f1.length == f2.length;
		double result = 0;
		for ( int i = 0; i < f1.length; i++ ) {
			result += ( (double) f1[i] ) * ( (double) f2[i] );
		}
		return result;
	}

	private static double euclideanNorm(float[] f) {
		double result = 0;
		for ( float v : f ) {
			result += Math.pow( v, 2 );
		}
		return Math.sqrt( result );
	}

	@Entity( name = "VectorEntity" )
	public static class VectorEntity {

		@Id
		private Long id;

		//tag::usage-example[]
		@Column( name = "the_vector" )
		@JdbcTypeCode(SqlTypes.VECTOR)
		@Array(length = 3)
		private float[] theVector;
		//end::usage-example[]

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
