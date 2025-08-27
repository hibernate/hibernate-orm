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
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.vector.VectorTestHelper.cosineDistanceBinary;
import static org.hibernate.vector.VectorTestHelper.euclideanDistanceBinary;
import static org.hibernate.vector.VectorTestHelper.euclideanNormBinary;
import static org.hibernate.vector.VectorTestHelper.euclideanSquaredDistanceBinary;
import static org.hibernate.vector.VectorTestHelper.hammingDistanceBinary;
import static org.hibernate.vector.VectorTestHelper.innerProductBinary;
import static org.hibernate.vector.VectorTestHelper.jaccardDistanceBinary;
import static org.hibernate.vector.VectorTestHelper.taxicabDistanceBinary;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = BinaryVectorTest.VectorEntity.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsBinaryVectorType.class)
@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "Test database does not have the extension enabled")
public class BinaryVectorTest {

	private static final byte[] V1 = new byte[]{ 1, 2, 3 };
	private static final byte[] V2 = new byte[]{ 4, 5, 6 };

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
			assertArrayEquals( new byte[]{ 1, 2, 3 }, tableRecord.getTheVector() );

			tableRecord = em.find( VectorEntity.class, 2L );
			assertArrayEquals( new byte[]{ 4, 5, 6 }, tableRecord.getTheVector() );
		} );
	}

	@Test
	public void testCast(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final String literal = VectorTestHelper.vectorBinaryStringLiteral( new byte[] {1, 1, 1}, em );
			final Tuple vector = em.createSelectionQuery( "select cast(e.theVector as string), cast('" + literal + "' as binary_vector(3)) from VectorEntity e where e.id = 1", Tuple.class )
					.getSingleResult();
			assertEquals( VectorTestHelper.vectorBinaryStringLiteral( V1, em ), vector.get( 0, String.class ) );
			assertArrayEquals( new byte[]{ 1, 1, 1 }, vector.get( 1, byte[].class ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCosineDistance.class)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "Not supported with bit vectors")
	public void testCosineDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, cosine_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( cosineDistanceBinary( V1, vector ), results.get( 0 ).get( 1, double.class ), 0.0000001D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( cosineDistanceBinary( V2, vector ), results.get( 1 ).get( 1, double.class ), 0.0000001D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsEuclideanSquaredDistance.class)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "Not supported with bit vectors")
	public void testEuclideanDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, euclidean_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( euclideanDistanceBinary( V1, vector ), results.get( 0 ).get( 1, double.class ), 0.000001D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( euclideanDistanceBinary( V2, vector ), results.get( 1 ).get( 1, double.class ), 0.000001D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsEuclideanDistance.class)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "Not supported with bit vectors")
	public void testEuclideanSquaredDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, euclidean_squared_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( euclideanSquaredDistanceBinary( V1, vector ), results.get( 0 ).get( 1, double.class ), 0.000001D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( euclideanSquaredDistanceBinary( V2, vector ), results.get( 1 ).get( 1, double.class ), 0.000001D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTaxicabDistance.class)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "Not supported with bit vectors")
	public void testTaxicabDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, taxicab_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( taxicabDistanceBinary( V1, vector ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( taxicabDistanceBinary( V2, vector ), results.get( 1 ).get( 1, double.class ), 0D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsInnerProduct.class)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "Not supported with bit vectors")
	public void testInnerProduct(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, inner_product(e.theVector, :vec), negative_inner_product(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( innerProductBinary( V1, vector ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( innerProductBinary( V1, vector ) * -1, results.get( 0 ).get( 2, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( innerProductBinary( V2, vector ), results.get( 1 ).get( 1, double.class ), 0D );
			assertEquals( innerProductBinary( V2, vector ) * -1, results.get( 1 ).get( 2, double.class ), 0D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHammingDistance.class)
	public void testHammingDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::hamming-distance-example[]
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, hamming_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			//end::hamming-distance-example[]
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( hammingDistanceBinary( V1, vector ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( hammingDistanceBinary( V2, vector ), results.get( 1 ).get( 1, double.class ), 0D );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJaccardDistance.class)
	public void testJaccardDistance(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			//tag::jaccard-distance-example[]
			final byte[] vector = new byte[]{ 1, 1, 1 };
			final List<Tuple> results = em.createSelectionQuery( "select e.id, jaccard_distance(e.theVector, :vec) from VectorEntity e order by e.id", Tuple.class )
					.setParameter( "vec", vector )
					.getResultList();
			//end::jaccard-distance-example[]
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( jaccardDistanceBinary( V1, vector ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( jaccardDistanceBinary( V2, vector ), results.get( 1 ).get( 1, double.class ), 0D );
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
			assertEquals( V1.length * 8, results.get( 0 ).get( 1 ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( V2.length * 8, results.get( 1 ).get( 1 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsVectorNorm.class)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "Not supported with bit vectors")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle 23.9 bug")
	public void testVectorNorm(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final List<Tuple> results = em.createSelectionQuery( "select e.id, vector_norm(e.theVector) from VectorEntity e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( euclideanNormBinary( V1 ), results.get( 0 ).get( 1, double.class ), 0D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( euclideanNormBinary( V2 ), results.get( 1 ).get( 1, double.class ), 0D );
		} );
	}

	@Entity( name = "VectorEntity" )
	public static class VectorEntity {

		@Id
		private Long id;

		@Column( name = "the_vector" )
		@JdbcTypeCode(SqlTypes.VECTOR_BINARY)
		@Array(length = 24)
		private byte[] theVector;

		public VectorEntity() {
		}

		public VectorEntity(Long id, byte[] theVector) {
			this.id = id;
			this.theVector = theVector;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public byte[] getTheVector() {
			return theVector;
		}

		public void setTheVector(byte[] theVector) {
			this.theVector = theVector;
		}
	}
}
