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
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.vector.VectorTestHelper.cosineDistance;
import static org.hibernate.vector.VectorTestHelper.euclideanDistance;
import static org.hibernate.vector.VectorTestHelper.euclideanNorm;
import static org.hibernate.vector.VectorTestHelper.hammingDistance;
import static org.hibernate.vector.VectorTestHelper.innerProduct;
import static org.hibernate.vector.VectorTestHelper.taxicabDistance;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = SparseByteVectorTest.VectorEntity.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSparseByteVectorType.class)
public class SparseByteVectorTest {

	private static final byte[] V1 = new byte[]{ 0, 2, 3 };
	private static final byte[] V2 = new byte[]{ 0, 5, 6 };

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new VectorEntity( 1L, new SparseByteVector( V1 ) ) );
			em.persist( new VectorEntity( 2L, new SparseByteVector( V2 ) ) );
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
			assertArrayEquals( new byte[]{ 0, 2, 3 }, tableRecord.getTheVector().toDenseVector() );

			tableRecord = em.find( VectorEntity.class, 2L );
			assertArrayEquals( new byte[]{ 0, 5, 6 }, tableRecord.getTheVector().toDenseVector()  );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCosineDistance.class)
	public void testCosineDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, cosine_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", new SparseByteVector( vector ) )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( cosineDistance( V1, vector ), results.get( 0 ).get( 1, double.class ), 0.0000001D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( cosineDistance( V2, vector ), results.get( 1 ).get( 1, double.class ), 0.0000001D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsEuclideanDistance.class)
	public void testEuclideanDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, euclidean_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", new SparseByteVector( vector ) )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( euclideanDistance( V1, vector ), results.get( 0 ).get( 1, double.class ), 0.000001D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( euclideanDistance( V2, vector ), results.get( 1 ).get( 1, double.class ), 0.000001D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTaxicabDistance.class)
	public void testTaxicabDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, taxicab_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", new SparseByteVector( vector ) )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( taxicabDistance( V1, vector ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( taxicabDistance( V2, vector ), results.get( 1 ).get( 1, double.class ), 0D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsInnerProduct.class)
	public void testInnerProduct(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, inner_product(e.theVector, :vec), negative_inner_product(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", new SparseByteVector( vector ) )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( innerProduct( V1, vector ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( innerProduct( V1, vector ) * -1, results.get( 0 ).get( 2, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( innerProduct( V2, vector ), results.get( 1 ).get( 1, double.class ), 0D );
			assertEquals( innerProduct( V2, vector ) * -1, results.get( 1 ).get( 2, double.class ), 0D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHammingDistance.class)
	public void testHammingDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, hamming_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", new SparseByteVector( vector ) )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( hammingDistance( V1, vector ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( hammingDistance( V2, vector ), results.get( 1 ).get( 1, double.class ), 0D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsVectorDims.class)
	public void testVectorDims(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final List<Tuple> results = em.createSelectionQuery( "select e.id, vector_dims(e.theVector) from VectorEntity e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( V1.length, results.get( 0 ).get( 1 ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( V2.length, results.get( 1 ).get( 1 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsVectorNorm.class)
	public void testVectorNorm(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final List<Tuple> results = em.createSelectionQuery( "select e.id, vector_norm(e.theVector) from VectorEntity e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( euclideanNorm( V1 ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( euclideanNorm( V2 ), results.get( 1 ).get( 1, double.class ), 0D );
		} );
	}

	@Entity( name = "VectorEntity" )
	public static class VectorEntity {

		@Id
		private Long id;

		@Column( name = "the_vector" )
		@JdbcTypeCode(SqlTypes.SPARSE_VECTOR_INT8)
		@Array(length = 3)
		private SparseByteVector theVector;

		public VectorEntity() {
		}

		public VectorEntity(Long id, SparseByteVector theVector) {
			this.id = id;
			this.theVector = theVector;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SparseByteVector getTheVector() {
			return theVector;
		}

		public void setTheVector(SparseByteVector theVector) {
			this.theVector = theVector;
		}
	}
}
