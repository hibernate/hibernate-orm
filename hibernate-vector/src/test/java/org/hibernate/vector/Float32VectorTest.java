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
import org.hibernate.vector.internal.VectorHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@DomainModel(annotatedClasses = Float32VectorTest.VectorEntity.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFloatVectorType.class)
public class Float32VectorTest extends FloatVectorTest {

	@BeforeEach
	@Override
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new VectorEntity( 1L, V1 ) );
			em.persist( new VectorEntity( 2L, V2 ) );
		} );
	}

	@Test
	@Override
	public void testRead(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			VectorEntity tableRecord;
			tableRecord = em.find( VectorEntity.class, 1L );
			assertArrayEquals( new float[] { 1, 2, 3 }, tableRecord.getTheVector(), 0 );

			tableRecord = em.find( VectorEntity.class, 2L );
			assertArrayEquals( new float[] { 4, 5, 6 }, tableRecord.getTheVector(), 0 );
		} );
	}

	@Test
	@Override
	public void testCast(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final Tuple vector = em.createSelectionQuery( "select cast(e.theVector as string), cast('[1, 1, 1]' as float_vector(3)) from VectorEntity e where e.id = 1", Tuple.class )
					.getSingleResult();
			assertArrayEquals( new float[]{ 1, 2, 3 }, VectorHelper.parseFloatVector( vector.get( 0, String.class ) ) );
			assertArrayEquals( new float[]{ 1, 1, 1 }, vector.get( 1, float[].class ) );
		} );
	}

	@Entity(name = "VectorEntity")
	public static class VectorEntity {

		@Id
		private Long id;

		@Column(name = "the_vector")
		@JdbcTypeCode(SqlTypes.VECTOR_FLOAT32)
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
