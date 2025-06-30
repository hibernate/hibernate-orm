/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

/**
 * Test mapping arrays with {@code @JdbcTypeCode(Type.VARBINARY)},
 * which is useful to revert to pre-6.1 behavior for array mapping in particular.
 */
@DomainModel(
		annotatedClasses = VarbinaryArrayTest.EntityWithArrays.class
)
@SessionFactory
@JiraKey(value = "HHH-16085")
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true)
public class VarbinaryArrayTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityWithArrays entity;

			// boolean[]
			entity = new EntityWithArrays();
			entity.setId( 1L );
			entity.setBoolArray( new boolean[] { true, false } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 2L );
			entity.setBoolArray( new boolean[] { false, true } );
			session.persist( entity );

			// byte[]
			entity = new EntityWithArrays();
			entity.setId( 3L );
			entity.setByteArray( new byte[] { 1, 2 } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 4L );
			entity.setByteArray( new byte[] { 2, 1 } );
			session.persist( entity );

			// char[]
			// Does not work:
			// org.hibernate.HibernateException: Unknown unwrap conversion requested: [C to [B : `org.hibernate.type.descriptor.java.PrimitiveCharacterArrayJavaType` ([C)
			// https://hibernate.atlassian.net/browse/HHH-16087
//			entity = new EntityWithArrays();
//			entity.setId( 5L );
//			entity.setCharArray( new char[] { 'a', 'b' } );
//			session.persist( entity );
//			entity = new EntityWithArrays();
//			entity.setId( 6L );
//			entity.setCharArray( new char[] { 'b', 'a' } );
//			session.persist( entity );

			// double[]
			entity = new EntityWithArrays();
			entity.setId( 7L );
			entity.setDoubleArray( new double[] { 1.01, 1.02 } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 8L );
			entity.setDoubleArray( new double[] { 1.02, 1.01 } );
			session.persist( entity );

			// float[]
			entity = new EntityWithArrays();
			entity.setId( 9L );
			entity.setFloatArray( new float[] { 4.01f, 5.02f } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 10L );
			entity.setFloatArray( new float[] { 5.02f, 4.01f } );
			session.persist( entity );

			// int[]
			entity = new EntityWithArrays();
			entity.setId( 11L );
			entity.setIntArray( new int[] { 6, 7 } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 12L );
			entity.setIntArray( new int[] { 7, 6 } );
			session.persist( entity );

			// long[]
			entity = new EntityWithArrays();
			entity.setId( 13L );
			entity.setLongArray( new long[] { 9L, 11L } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 14L );
			entity.setLongArray( new long[] { 11L, 9L } );
			session.persist( entity );

			// short[]
			entity = new EntityWithArrays();
			entity.setId( 15L );
			entity.setShortArray( new short[] { 100, 101 } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 16L );
			entity.setShortArray( new short[] { 101, 100 } );
			session.persist( entity );

			// Object[]
			entity = new EntityWithArrays();
			entity.setId( 17L );
			entity.setSerializableArray( new Serializable[] { "one", 102 } );
			session.persist( entity );
			entity = new EntityWithArrays();
			entity.setId( 18L );
			entity.setSerializableArray( new Serializable[] { 102, "one" } );
			session.persist( entity );
		} );
	}

	Stream<Arguments> perTypeArguments() {
		return Stream.of(
				Arguments.of(
						"boolArray", 1, new boolean[] { true, false },
						(Function<EntityWithArrays, boolean[]>) EntityWithArrays::getBoolArray
				),
				Arguments.of(
						"byteArray", 3, new byte[] { 1, 2 },
						(Function<EntityWithArrays, byte[]>) EntityWithArrays::getByteArray
				),
				// Does not work:
				// org.hibernate.HibernateException: Unknown unwrap conversion requested: [C to [B : `org.hibernate.type.descriptor.java.PrimitiveCharacterArrayJavaType` ([C)
				// https://hibernate.atlassian.net/browse/HHH-16087
//				Arguments.of(
//						"charArray", 5, new char[] { 'a', 'b' },
//						(Function<EntityWithArrays, char[]>) EntityWithArrays::getCharArray
//				),
				Arguments.of(
						"doubleArray", 7, new double[] { 1.01, 1.02 },
						(Function<EntityWithArrays, double[]>) EntityWithArrays::getDoubleArray
				),
				Arguments.of(
						"floatArray", 9, new float[] { 4.01f, 5.02f },
						(Function<EntityWithArrays, float[]>) EntityWithArrays::getFloatArray
				),
				Arguments.of(
						"intArray", 11, new int[] { 6, 7 },
						(Function<EntityWithArrays, int[]>) EntityWithArrays::getIntArray
				),
				Arguments.of(
						"longArray", 13, new long[] { 9L, 11L },
						(Function<EntityWithArrays, long[]>) EntityWithArrays::getLongArray
				),
				Arguments.of(
						"shortArray", 15, new short[] { 100, 101 },
						(Function<EntityWithArrays, short[]>) EntityWithArrays::getShortArray
				),
				Arguments.of(
						"serializableArray", 17, new Serializable[] { "one", 102 },
						(Function<EntityWithArrays, Serializable[]>) EntityWithArrays::getSerializableArray
				)
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@MethodSource("perTypeArguments")
	<T> void loadById(String propertyName, long id, T value, Function<EntityWithArrays, T> getter,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityWithArrays entity = session.byId( EntityWithArrays.class ).load( id );
			assertThat( entity ).extracting( getter ).isEqualTo( value );
		} );
	}

	@ParameterizedTest
	@MethodSource("perTypeArguments")
	<T> void queryById(String propertyName, long id, T value, Function<EntityWithArrays, T> getter,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TypedQuery<EntityWithArrays> tq = session.createQuery(
					"SELECT e FROM EntityWithArrays e WHERE id = :id", EntityWithArrays.class );
			tq.setParameter( "id", id );
			EntityWithArrays entity = tq.getSingleResult();
			assertThat( entity ).extracting( EntityWithArrays::getId ).isEqualTo( id );
			assertThat( entity ).extracting( getter ).isEqualTo( value );
		} );
	}

	@ParameterizedTest
	@MethodSource("perTypeArguments")
	@SkipForDialect( dialectClass = HANADialect.class, matchSubTypes = true,
			reason = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "The statement failed because binary large objects are not allowed in the Union, Intersect, or Minus queries")
	<T> void queryByData(String propertyName, long id, T value, Function<EntityWithArrays, T> getter,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TypedQuery<EntityWithArrays> tq = session.createQuery(
					"SELECT e FROM EntityWithArrays e WHERE " + propertyName + " IS NOT DISTINCT FROM :value",
					EntityWithArrays.class
			);
			tq.setParameter( "value", value );
			EntityWithArrays entity = tq.getSingleResult();
			assertThat( entity ).extracting( EntityWithArrays::getId ).isEqualTo( id );
			assertThat( entity ).extracting( getter ).isEqualTo( value );
		} );
	}

	@Entity(name = "EntityWithArrays")
	public static class EntityWithArrays {

		@Id
		private Long id;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private boolean[] boolArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private byte[] byteArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private char[] charArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private double[] doubleArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private float[] floatArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private int[] intArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private long[] longArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private short[] shortArray;

		@Column
		@JdbcTypeCode(SqlTypes.VARBINARY)
		private Serializable[] serializableArray;

		public EntityWithArrays() {
		}

		public EntityWithArrays(Long id, boolean[] theArray) {
			this.id = id;
			this.boolArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public boolean[] getBoolArray() {
			return boolArray;
		}

		public void setBoolArray(boolean[] boolArray) {
			this.boolArray = boolArray;
		}

		public byte[] getByteArray() {
			return byteArray;
		}

		public void setByteArray(byte[] byteArray) {
			this.byteArray = byteArray;
		}

		public char[] getCharArray() {
			return charArray;
		}

		public void setCharArray(char[] charArray) {
			this.charArray = charArray;
		}

		public double[] getDoubleArray() {
			return doubleArray;
		}

		public void setDoubleArray(double[] doubleArray) {
			this.doubleArray = doubleArray;
		}

		public float[] getFloatArray() {
			return floatArray;
		}

		public void setFloatArray(float[] floatArray) {
			this.floatArray = floatArray;
		}

		public int[] getIntArray() {
			return intArray;
		}

		public void setIntArray(int[] intArray) {
			this.intArray = intArray;
		}

		public long[] getLongArray() {
			return longArray;
		}

		public void setLongArray(long[] longArray) {
			this.longArray = longArray;
		}

		public short[] getShortArray() {
			return shortArray;
		}

		public void setShortArray(short[] shortArray) {
			this.shortArray = shortArray;
		}

		public Serializable[] getSerializableArray() {
			return serializableArray;
		}

		public void setSerializableArray(Serializable[] serializableArray) {
			this.serializableArray = serializableArray;
		}
	}
}
