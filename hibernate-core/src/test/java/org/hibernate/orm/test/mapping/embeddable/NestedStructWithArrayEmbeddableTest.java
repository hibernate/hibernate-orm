/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialect( PostgreSQLDialect.class )
@RequiresDialect( OracleDialect.class )
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
// Don't reorder columns in the types here to avoid the need to rewrite the test
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.COLUMN_ORDERING_STRATEGY, value = "legacy"),
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.PREFERRED_ARRAY_JDBC_TYPE,
				provider = OracleNestedTableSettingProvider.class
		)
)
@DomainModel(annotatedClasses = NestedStructWithArrayEmbeddableTest.StructHolder.class)
@SessionFactory
public class NestedStructWithArrayEmbeddableTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new StructHolder( 1L, "XYZ", 10, "String \"<abc>A&B</abc>\"", EmbeddableWithArrayAggregate.createAggregate1() ) );
					session.persist( new StructHolder( 2L, null, 20, "String 'abc'", EmbeddableWithArrayAggregate.createAggregate2() ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StructHolder structHolder = entityManager.find( StructHolder.class, 1L );
					structHolder.setAggregate( EmbeddableWithArrayAggregate.createAggregate2() );
					entityManager.flush();
					entityManager.clear();
					structHolder = entityManager.find( StructHolder.class, 1L );
					assertEquals( "XYZ", structHolder.struct.stringField );
					assertArrayEquals( new Integer[]{ 10 }, structHolder.struct.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate2(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetch(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<StructHolder> structHolders = entityManager.createQuery( "from StructHolder b where b.id = 1", StructHolder.class ).getResultList();
					assertEquals( 1, structHolders.size() );
					StructHolder structHolder = structHolders.get( 0 );
					assertEquals( 1L, structHolder.getId() );
					assertEquals( "XYZ", structHolder.struct.stringField );
					assertArrayEquals( new Integer[]{ 10 }, structHolder.struct.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", structHolder.struct.simpleEmbeddable.doubleNested.leaf[0].stringField );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetchNull(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<StructHolder> structHolders = entityManager.createQuery( "from StructHolder b where b.id = 2", StructHolder.class ).getResultList();
					assertEquals( 1, structHolders.size() );
					StructHolder structHolder = structHolders.get( 0 );
					assertEquals( 2L, structHolder.getId() );
					assertNull( structHolder.struct.stringField );
					assertArrayEquals( new Integer[]{ 20 }, structHolder.struct.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate2(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<TheStruct> structs = entityManager.createQuery( "select b.struct from StructHolder b where b.id = 1", TheStruct.class ).getResultList();
					assertEquals( 1, structs.size() );
					TheStruct theStruct = structs.get( 0 );
					assertEquals( "XYZ", theStruct.stringField );
					assertArrayEquals( new Integer[]{ 10 }, theStruct.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", theStruct.simpleEmbeddable.doubleNested.leaf[0].stringField );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), theStruct.nested[0] );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "We have to use TABLE storage in this test because Oracle doesn't support LOBs in struct arrays, but TABLE is not indexed")
	public void testSelectionItems(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<Tuple> tuples = entityManager.createQuery(
							"select " +
									"b.struct.nested[1].theInt," +
									"b.struct.nested[1].theDouble," +
									"b.struct.nested[1].theBoolean," +
									"b.struct.nested[1].theNumericBoolean," +
									"b.struct.nested[1].theStringBoolean," +
									"b.struct.nested[1].theString," +
									"b.struct.nested[1].theInteger," +
									"b.struct.nested[1].theUrl," +
									"b.struct.nested[1].theClob," +
									"b.struct.nested[1].theBinary," +
									"b.struct.nested[1].theDate," +
									"b.struct.nested[1].theTime," +
									"b.struct.nested[1].theTimestamp," +
									"b.struct.nested[1].theInstant," +
									"b.struct.nested[1].theUuid," +
									"b.struct.nested[1].gender," +
									"b.struct.nested[1].convertedGender," +
									"b.struct.nested[1].ordinalGender," +
									"b.struct.nested[1].theDuration," +
									"b.struct.nested[1].theLocalDateTime," +
									"b.struct.nested[1].theLocalDate," +
									"b.struct.nested[1].theLocalTime," +
									"b.struct.nested[1].theZonedDateTime," +
									"b.struct.nested[1].theOffsetDateTime," +
									"b.struct.nested[1].mutableValue," +
									"b.struct.simpleEmbeddable," +
									"b.struct.simpleEmbeddable.doubleNested," +
									"b.struct.simpleEmbeddable.doubleNested.leaf " +
									"from StructHolder b where b.id = 1",
							Tuple.class
					).getResultList();
					assertEquals( 1, tuples.size() );
					final Tuple tuple = tuples.get( 0 );
					final EmbeddableWithArrayAggregate struct = new EmbeddableWithArrayAggregate();
					struct.setTheInt( tuple.get( 0, int[].class ) );
					struct.setTheDouble( tuple.get( 1, double[].class ) );
					struct.setTheBoolean( tuple.get( 2, Boolean[].class ) );
					struct.setTheNumericBoolean( tuple.get( 3, Boolean[].class ) );
					struct.setTheStringBoolean( tuple.get( 4, Boolean[].class ) );
					struct.setTheString( tuple.get( 5, String[].class ) );
					struct.setTheInteger( tuple.get( 6, Integer[].class ) );
					struct.setTheUrl( tuple.get( 7, URL[].class ) );
					struct.setTheClob( tuple.get( 8, String[].class ) );
					struct.setTheBinary( tuple.get( 9, byte[][].class ) );
					struct.setTheDate( tuple.get( 10, Date[].class ) );
					struct.setTheTime( tuple.get( 11, Time[].class ) );
					struct.setTheTimestamp( tuple.get( 12, Timestamp[].class ) );
					struct.setTheInstant( tuple.get( 13, Instant[].class ) );
					struct.setTheUuid( tuple.get( 14, UUID[].class ) );
					struct.setGender( tuple.get( 15, EntityOfBasics.Gender[].class ) );
					struct.setConvertedGender( tuple.get( 16, EntityOfBasics.Gender[].class ) );
					struct.setOrdinalGender( tuple.get( 17, EntityOfBasics.Gender[].class ) );
					struct.setTheDuration( tuple.get( 18, Duration[].class ) );
					struct.setTheLocalDateTime( tuple.get( 19, LocalDateTime[].class ) );
					struct.setTheLocalDate( tuple.get( 20, LocalDate[].class ) );
					struct.setTheLocalTime( tuple.get( 21, LocalTime[].class ) );
					struct.setTheZonedDateTime( tuple.get( 22, ZonedDateTime[].class ) );
					struct.setTheOffsetDateTime( tuple.get( 23, OffsetDateTime[].class ) );
					struct.setMutableValue( tuple.get( 24, MutableValue[].class ) );
					EmbeddableWithArrayAggregate.assertEquals( EmbeddableWithArrayAggregate.createAggregate1(), struct );

					SimpleEmbeddable simpleEmbeddable = tuple.get( 25, SimpleEmbeddable.class );
					assertEquals( simpleEmbeddable.doubleNested, tuple.get( 26, DoubleNested.class ) );
					assertArrayEquals( simpleEmbeddable.doubleNested.leaf, tuple.get( 27, Leaf[].class ) );
					assertArrayEquals( new Integer[]{ 10 }, simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", simpleEmbeddable.doubleNested.leaf[0].stringField );
				}
		);
	}

	@Test
	public void testDeleteWhere(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "delete StructHolder b where b.struct is not null" ).executeUpdate();
					assertNull( entityManager.find( StructHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.struct = null" ).executeUpdate();
					assertNull( entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-18051")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "We have to use TABLE storage in this test because Oracle doesn't support LOBs in struct arrays, but TABLE is not indexed")
	public void testUpdateAggregateMember(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.struct.nested[1].theString = null" ).executeUpdate();
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					struct.setTheString( null );
					assertStructEquals( struct, entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-18051")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "We have to use TABLE storage in this test because Oracle doesn't support LOBs in struct arrays, but TABLE is not indexed")
	public void testUpdateMultipleAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.struct.nested[1].theString = null, b.struct.nested[1].theUuid = null" ).executeUpdate();
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					struct.setTheString( null );
					struct.setTheUuid( null );
					assertStructEquals( struct, entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-18051")
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "We have to use TABLE storage in this test because Oracle doesn't support LOBs in struct arrays, but TABLE is not indexed")
	public void testUpdateAllAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					entityManager.createMutationQuery(
							"update StructHolder b set " +
									"b.struct.nested[1].theInt = :theInt," +
									"b.struct.nested[1].theDouble = :theDouble," +
									"b.struct.nested[1].theBoolean = :theBoolean," +
									"b.struct.nested[1].theNumericBoolean = :theNumericBoolean," +
									"b.struct.nested[1].theStringBoolean = :theStringBoolean," +
									"b.struct.nested[1].theString = :theString," +
									"b.struct.nested[1].theInteger = :theInteger," +
									"b.struct.nested[1].theUrl = :theUrl," +
									"b.struct.nested[1].theClob = :theClob," +
									"b.struct.nested[1].theBinary = :theBinary," +
									"b.struct.nested[1].theDate = :theDate," +
									"b.struct.nested[1].theTime = :theTime," +
									"b.struct.nested[1].theTimestamp = :theTimestamp," +
									"b.struct.nested[1].theInstant = :theInstant," +
									"b.struct.nested[1].theUuid = :theUuid," +
									"b.struct.nested[1].gender = :gender," +
									"b.struct.nested[1].convertedGender = :convertedGender," +
									"b.struct.nested[1].ordinalGender = :ordinalGender," +
									"b.struct.nested[1].theDuration = :theDuration," +
									"b.struct.nested[1].theLocalDateTime = :theLocalDateTime," +
									"b.struct.nested[1].theLocalDate = :theLocalDate," +
									"b.struct.nested[1].theLocalTime = :theLocalTime," +
									"b.struct.nested[1].theZonedDateTime = :theZonedDateTime," +
									"b.struct.nested[1].theOffsetDateTime = :theOffsetDateTime," +
									"b.struct.nested[1].mutableValue = :mutableValue," +
									"b.struct.simpleEmbeddable.integerField = :integerField " +
									"where b.id = 2"
					)
							.setParameter( "theInt", struct.getTheInt() )
							.setParameter( "theDouble", struct.getTheDouble() )
							.setParameter( "theBoolean", struct.getTheBoolean() )
							.setParameter( "theNumericBoolean", struct.getTheNumericBoolean() )
							.setParameter( "theStringBoolean", struct.getTheStringBoolean() )
							.setParameter( "theString", struct.getTheString() )
							.setParameter( "theInteger", struct.getTheInteger() )
							.setParameter( "theUrl", struct.getTheUrl() )
							.setParameter( "theClob", struct.getTheClob() )
							.setParameter( "theBinary", struct.getTheBinary() )
							.setParameter( "theDate", struct.getTheDate() )
							.setParameter( "theTime", struct.getTheTime() )
							.setParameter( "theTimestamp", struct.getTheTimestamp() )
							.setParameter( "theInstant", struct.getTheInstant() )
							.setParameter( "theUuid", struct.getTheUuid() )
							.setParameter( "gender", struct.getGender() )
							.setParameter( "convertedGender", struct.getConvertedGender() )
							.setParameter( "ordinalGender", struct.getOrdinalGender() )
							.setParameter( "theDuration", struct.getTheDuration() )
							.setParameter( "theLocalDateTime", struct.getTheLocalDateTime() )
							.setParameter( "theLocalDate", struct.getTheLocalDate() )
							.setParameter( "theLocalTime", struct.getTheLocalTime() )
							.setParameter( "theZonedDateTime", struct.getTheZonedDateTime() )
							.setParameter( "theOffsetDateTime", struct.getTheOffsetDateTime() )
							.setParameter( "mutableValue", struct.getMutableValue() )
							.setParameter( "integerField", new Integer[]{ 5 } )
							.executeUpdate();
					StructHolder structHolder = entityManager.find( StructHolder.class, 2L );
					assertArrayEquals( new Integer[]{ 5 }, structHolder.struct.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					//noinspection unchecked
					List<Object> resultList = entityManager.createNativeQuery(
									"select b.struct from StructHolder b where b.id = 1", Object.class
							)
							.getResultList();
					assertEquals( 1, resultList.size() );
					assertInstanceOf( TheStruct.class, resultList.get( 0 ) );
					TheStruct theStruct = (TheStruct) resultList.get( 0 );
					assertEquals( "XYZ", theStruct.stringField );
					assertArrayEquals( new Integer[]{ 10 }, theStruct.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", theStruct.simpleEmbeddable.doubleNested.leaf[0].stringField );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), theStruct.nested[0] );
				}
		);
	}

	private static void assertStructEquals(EmbeddableWithArrayAggregate struct, EmbeddableWithArrayAggregate struct2) {
		assertArrayEquals( struct.getTheBinary(), struct2.getTheBinary() );
		assertArrayEquals( struct.getTheString(), struct2.getTheString() );
		assertArrayEquals( struct.getTheLocalDateTime(), struct2.getTheLocalDateTime() );
		assertArrayEquals( struct.getTheUuid(), struct2.getTheUuid() );
	}

	@Entity(name = "StructHolder")
	public static class StructHolder {
		@Id
		private Long id;
		private TheStruct struct;

		public StructHolder() {
		}

		public StructHolder(Long id, String stringField, Integer integerField, String leaf, EmbeddableWithArrayAggregate aggregate) {
			this.id = id;
			this.struct = new TheStruct( stringField, integerField, leaf, aggregate );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public TheStruct getStruct() {
			return struct;
		}

		public void setStruct(TheStruct struct) {
			this.struct = struct;
		}

		public EmbeddableWithArrayAggregate getAggregate() {
			return struct == null ? null : struct.nested[0];
		}

		public void setAggregate(EmbeddableWithArrayAggregate aggregate) {
			if ( struct == null ) {
				struct = new TheStruct( null, null, null, aggregate );
			}
			else {
				struct.nested = new EmbeddableWithArrayAggregate[]{ aggregate };
			}
		}

	}

	@Embeddable
	@Struct( name = "theStruct" )
	public static class TheStruct {
		private String stringField;
		private SimpleEmbeddable simpleEmbeddable;
		@Struct(name = "structType")
		private EmbeddableWithArrayAggregate[] nested;

		public TheStruct() {
		}

		public TheStruct(String stringField, Integer integerField, String leaf, EmbeddableWithArrayAggregate nested) {
			this.stringField = stringField;
			this.simpleEmbeddable = new SimpleEmbeddable( integerField, leaf );
			this.nested = new EmbeddableWithArrayAggregate[]{ nested };
		}
	}

	@Embeddable
	public static class SimpleEmbeddable {
		@JdbcTypeCode(SqlTypes.ARRAY)
		private Integer[] integerField;
		private DoubleNested doubleNested;

		public SimpleEmbeddable() {
		}

		public SimpleEmbeddable(Integer integerField, String leaf) {
			this.integerField = new Integer[]{ integerField };
			this.doubleNested = new DoubleNested( leaf );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			SimpleEmbeddable that = (SimpleEmbeddable) o;

			if ( !Arrays.equals( integerField, that.integerField ) ) {
				return false;
			}
			return Objects.equals( doubleNested, that.doubleNested );
		}

		@Override
		public int hashCode() {
			int result = Arrays.hashCode( integerField );
			result = 31 * result + ( doubleNested != null ? doubleNested.hashCode() : 0 );
			return result;
		}
	}

	@Embeddable
	@Struct( name = "double_nested")
	public static class DoubleNested {
		@JdbcTypeCode(SqlTypes.ARRAY)
		private Leaf[] leaf;

		public DoubleNested() {
		}

		public DoubleNested(String leaf) {
			this.leaf = new Leaf[]{ new Leaf( leaf ) };
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			DoubleNested that = (DoubleNested) o;

			return Arrays.equals( leaf, that.leaf );
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode( leaf );
		}
	}

	@Embeddable
	@Struct( name = "leaf")
	public static class Leaf {
		private String stringField;
		@JdbcTypeCode(SqlTypes.ARRAY)
		private String[] stringArrayField;

		public Leaf() {
		}

		public Leaf(String stringField) {
			this.stringField = stringField;
			this.stringArrayField = new String[]{ stringField };
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Leaf leaf = (Leaf) o;

			if ( !Objects.equals( stringField, leaf.stringField ) ) {
				return false;
			}
			return Arrays.equals( stringArrayField, leaf.stringArrayField );
		}

		@Override
		public int hashCode() {
			int result = stringField != null ? stringField.hashCode() : 0;
			result = 31 * result + Arrays.hashCode( stringArrayField );
			return result;
		}
	}
}
