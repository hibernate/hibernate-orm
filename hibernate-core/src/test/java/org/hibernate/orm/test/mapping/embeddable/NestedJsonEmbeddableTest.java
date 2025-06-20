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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
public class NestedJsonEmbeddableTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			JsonHolder.class
		};
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					session.persist( new JsonHolder( 1L, "XYZ", 10, "String \"<abc>A&B</abc>\"", EmbeddableAggregate.createAggregate1() ) );
					session.persist( new JsonHolder( 2L, null, 20, "String 'abc'", EmbeddableAggregate.createAggregate2() ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder jsonHolder = entityManager.find( JsonHolder.class, 1L );
					jsonHolder.setAggregate( EmbeddableAggregate.createAggregate2() );
					entityManager.flush();
					entityManager.clear();
					jsonHolder = entityManager.find( JsonHolder.class, 1L );
					assertEquals( "XYZ", jsonHolder.theJson.stringField );
					assertEquals( 10, jsonHolder.theJson.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate2(), jsonHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetch() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<JsonHolder> jsonHolders = entityManager.createQuery( "from JsonHolder b where b.id = 1", JsonHolder.class ).getResultList();
					assertEquals( 1, jsonHolders.size() );
					JsonHolder jsonHolder = jsonHolders.get( 0 );
					assertEquals( 1L, jsonHolder.getId() );
					assertEquals( "XYZ", jsonHolder.theJson.stringField );
					assertEquals( 10, jsonHolder.theJson.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", jsonHolder.theJson.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), jsonHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetchNull() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<JsonHolder> jsonHolders = entityManager.createQuery( "from JsonHolder b where b.id = 2", JsonHolder.class ).getResultList();
					assertEquals( 1, jsonHolders.size() );
					JsonHolder jsonHolder = jsonHolders.get( 0 );
					assertEquals( 2L, jsonHolder.getId() );
					assertNull( jsonHolder.theJson.stringField );
					assertEquals( 20, jsonHolder.theJson.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate2(), jsonHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<TheJson> structs = entityManager.createQuery( "select b.theJson from JsonHolder b where b.id = 1", TheJson.class ).getResultList();
					assertEquals( 1, structs.size() );
					TheJson theJson = structs.get( 0 );
					assertEquals( "XYZ", theJson.stringField );
					assertEquals( 10, theJson.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", theJson.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), theJson.nested );
				}
		);
	}

	@Test
	public void testSelectionItems() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<Tuple> tuples = entityManager.createQuery(
							"select " +
									"b.theJson.nested.theInt," +
									"b.theJson.nested.theDouble," +
									"b.theJson.nested.theBoolean," +
									"b.theJson.nested.theNumericBoolean," +
									"b.theJson.nested.theStringBoolean," +
									"b.theJson.nested.theString," +
									"b.theJson.nested.theInteger," +
									"b.theJson.nested.theUrl," +
									"b.theJson.nested.theClob," +
									"b.theJson.nested.theBinary," +
									"b.theJson.nested.theDate," +
									"b.theJson.nested.theTime," +
									"b.theJson.nested.theTimestamp," +
									"b.theJson.nested.theInstant," +
									"b.theJson.nested.theUuid," +
									"b.theJson.nested.gender," +
									"b.theJson.nested.convertedGender," +
									"b.theJson.nested.ordinalGender," +
									"b.theJson.nested.theDuration," +
									"b.theJson.nested.theLocalDateTime," +
									"b.theJson.nested.theLocalDate," +
									"b.theJson.nested.theLocalTime," +
									"b.theJson.nested.theZonedDateTime," +
									"b.theJson.nested.theOffsetDateTime," +
									"b.theJson.nested.mutableValue," +
									"b.theJson.simpleEmbeddable," +
									"b.theJson.simpleEmbeddable.doubleNested," +
									"b.theJson.simpleEmbeddable.doubleNested.theNested," +
									"b.theJson.simpleEmbeddable.doubleNested.theNested.theLeaf " +
									"from JsonHolder b where b.id = 1",
							Tuple.class
					).getResultList();
					assertEquals( 1, tuples.size() );
					final Tuple tuple = tuples.get( 0 );
					final EmbeddableAggregate struct = new EmbeddableAggregate();
					struct.setTheInt( tuple.get( 0, int.class ) );
					struct.setTheDouble( tuple.get( 1, Double.class ) );
					struct.setTheBoolean( tuple.get( 2, Boolean.class ) );
					struct.setTheNumericBoolean( tuple.get( 3, Boolean.class ) );
					struct.setTheStringBoolean( tuple.get( 4, Boolean.class ) );
					struct.setTheString( tuple.get( 5, String.class ) );
					struct.setTheInteger( tuple.get( 6, Integer.class ) );
					struct.setTheUrl( tuple.get( 7, URL.class ) );
					struct.setTheClob( tuple.get( 8, String.class ) );
					struct.setTheBinary( tuple.get( 9, byte[].class ) );
					struct.setTheDate( tuple.get( 10, Date.class ) );
					struct.setTheTime( tuple.get( 11, Time.class ) );
					struct.setTheTimestamp( tuple.get( 12, Timestamp.class ) );
					struct.setTheInstant( tuple.get( 13, Instant.class ) );
					struct.setTheUuid( tuple.get( 14, UUID.class ) );
					struct.setGender( tuple.get( 15, EntityOfBasics.Gender.class ) );
					struct.setConvertedGender( tuple.get( 16, EntityOfBasics.Gender.class ) );
					struct.setOrdinalGender( tuple.get( 17, EntityOfBasics.Gender.class ) );
					struct.setTheDuration( tuple.get( 18, Duration.class ) );
					struct.setTheLocalDateTime( tuple.get( 19, LocalDateTime.class ) );
					struct.setTheLocalDate( tuple.get( 20, LocalDate.class ) );
					struct.setTheLocalTime( tuple.get( 21, LocalTime.class ) );
					struct.setTheZonedDateTime( tuple.get( 22, ZonedDateTime.class ) );
					struct.setTheOffsetDateTime( tuple.get( 23, OffsetDateTime.class ) );
					struct.setMutableValue( tuple.get( 24, MutableValue.class ) );
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate1(), struct );

					SimpleEmbeddable simpleEmbeddable = tuple.get( 25, SimpleEmbeddable.class );
					assertEquals( simpleEmbeddable.doubleNested, tuple.get( 26, DoubleNested.class ) );
					assertEquals( simpleEmbeddable.doubleNested.theNested, tuple.get( 27, Nested.class ) );
					assertEquals( simpleEmbeddable.doubleNested.theNested.theLeaf, tuple.get( 28, Leaf.class ) );
					assertEquals( 10, simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
				}
		);
	}

	@Test
	public void testDeleteWhere() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "delete JsonHolder b where b.theJson is not null" ).executeUpdate();
					assertNull( entityManager.find( JsonHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update JsonHolder b set b.theJson = null" ).executeUpdate();
					assertNull( entityManager.find( JsonHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17695" )
	public void testNullNestedAggregate() {
		sessionFactoryScope().inTransaction(
			entityManager -> {
				JsonHolder jsonHolder = new JsonHolder(3L, "abc", 30, "String 'xyz'", null );
				entityManager.persist( jsonHolder );
			}
		);
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder jsonHolder = entityManager.createQuery( "from JsonHolder b where b.id = 3", JsonHolder.class ).getSingleResult();
					assertEquals( "abc", jsonHolder.theJson.stringField );
					assertEquals( 30, jsonHolder.theJson.simpleEmbeddable.integerField );
					assertEquals( "String 'xyz'", jsonHolder.theJson.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertNull( jsonHolder.getAggregate() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17695" )
	public void testNullNestedEmbeddable() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder jsonHolder = new JsonHolder( );
					jsonHolder.setId( 3L );
					jsonHolder.setTheJson( new TheJson( "abc", null, EmbeddableAggregate.createAggregate1() ) );
					entityManager.persist( jsonHolder );
				}
		);
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder jsonHolder = entityManager.createQuery( "from JsonHolder b where b.id = 3", JsonHolder.class ).getSingleResult();
					assertEquals( "abc", jsonHolder.theJson.stringField );
					assertNull( jsonHolder.theJson.simpleEmbeddable );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), jsonHolder.getAggregate() );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17695" )
	public void testNullNestedEmbeddableAndAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder jsonHolder = new JsonHolder( );
					jsonHolder.setId( 3L );
					jsonHolder.setTheJson( new TheJson( "abc", null, null ) );
					entityManager.persist( jsonHolder );
				}
		);
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonHolder jsonHolder = entityManager.createQuery( "from JsonHolder b where b.id = 3", JsonHolder.class ).getSingleResult();
					assertEquals( "abc", jsonHolder.theJson.stringField );
					assertNull( jsonHolder.theJson.simpleEmbeddable );
					assertNull( jsonHolder.getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonComponentUpdate.class)
	public void testUpdateAggregateMember() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update JsonHolder b set b.theJson.nested.theString = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					assertStructEquals( struct, entityManager.find( JsonHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonComponentUpdate.class)
	public void testUpdateMultipleAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update JsonHolder b set b.theJson.nested.theString = null, b.theJson.nested.theUuid = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					struct.setTheUuid( null );
					assertStructEquals( struct, entityManager.find( JsonHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonComponentUpdate.class)
	public void testUpdateAllAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					entityManager.createMutationQuery(
							"update JsonHolder b set " +
									"b.theJson.nested.theInt = :theInt," +
									"b.theJson.nested.theDouble = :theDouble," +
									"b.theJson.nested.theBoolean = :theBoolean," +
									"b.theJson.nested.theNumericBoolean = :theNumericBoolean," +
									"b.theJson.nested.theStringBoolean = :theStringBoolean," +
									"b.theJson.nested.theString = :theString," +
									"b.theJson.nested.theInteger = :theInteger," +
									"b.theJson.nested.theUrl = :theUrl," +
									"b.theJson.nested.theClob = :theClob," +
									"b.theJson.nested.theBinary = :theBinary," +
									"b.theJson.nested.theDate = :theDate," +
									"b.theJson.nested.theTime = :theTime," +
									"b.theJson.nested.theTimestamp = :theTimestamp," +
									"b.theJson.nested.theInstant = :theInstant," +
									"b.theJson.nested.theUuid = :theUuid," +
									"b.theJson.nested.gender = :gender," +
									"b.theJson.nested.convertedGender = :convertedGender," +
									"b.theJson.nested.ordinalGender = :ordinalGender," +
									"b.theJson.nested.theDuration = :theDuration," +
									"b.theJson.nested.theLocalDateTime = :theLocalDateTime," +
									"b.theJson.nested.theLocalDate = :theLocalDate," +
									"b.theJson.nested.theLocalTime = :theLocalTime," +
									"b.theJson.nested.theZonedDateTime = :theZonedDateTime," +
									"b.theJson.nested.theOffsetDateTime = :theOffsetDateTime," +
									"b.theJson.nested.mutableValue = :mutableValue," +
									"b.theJson.simpleEmbeddable.integerField = :integerField " +
									"where b.id = 2"
					)
							.setParameter( "theInt", struct.getTheInt() )
							.setParameter( "theDouble", struct.getTheDouble() )
							.setParameter( "theBoolean", struct.isTheBoolean() )
							.setParameter( "theNumericBoolean", struct.isTheNumericBoolean() )
							.setParameter( "theStringBoolean", struct.isTheStringBoolean() )
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
							.setParameter( "integerField", 5 )
							.executeUpdate();
					JsonHolder jsonHolder = entityManager.find( JsonHolder.class, 2L );
					assertEquals( 5, jsonHolder.theJson.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), jsonHolder.getAggregate() );
				}
		);
	}

	private static void assertStructEquals(EmbeddableAggregate struct, EmbeddableAggregate struct2) {
		assertArrayEquals( struct.getTheBinary(), struct2.getTheBinary() );
		assertEquals( struct.getTheString(), struct2.getTheString() );
		assertEquals( struct.getTheLocalDateTime(), struct2.getTheLocalDateTime() );
		assertEquals( struct.getTheUuid(), struct2.getTheUuid() );
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {
		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		private TheJson theJson;

		public JsonHolder() {
		}

		public JsonHolder(Long id, String stringField, Integer integerField, String leaf, EmbeddableAggregate aggregate) {
			this.id = id;
			this.theJson = new TheJson( stringField, integerField, leaf, aggregate );
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public TheJson getTheJson() {
			return theJson;
		}

		public void setTheJson(TheJson struct) {
			this.theJson = struct;
		}

		public EmbeddableAggregate getAggregate() {
			return theJson == null ? null : theJson.nested;
		}

		public void setAggregate(EmbeddableAggregate aggregate) {
			if ( theJson == null ) {
				theJson = new TheJson( null, null, null, aggregate );
			}
			else {
				theJson.nested = aggregate;
			}
		}

	}

	@Embeddable
	public static class TheJson {
		private String stringField;
		private SimpleEmbeddable simpleEmbeddable;
		@JdbcTypeCode(SqlTypes.JSON)
		private EmbeddableAggregate nested;

		public TheJson() {
		}

		public TheJson(String stringField, Integer integerField, String leaf, EmbeddableAggregate nested) {
			this.stringField = stringField;
			this.simpleEmbeddable = new SimpleEmbeddable( integerField, leaf );
			this.nested = nested;
		}

		public TheJson(String stringField, SimpleEmbeddable simpleEmbeddable, EmbeddableAggregate nested) {
			this.stringField = stringField;
			this.simpleEmbeddable = simpleEmbeddable;
			this.nested = nested;
		}
	}

	@Embeddable
	public static class SimpleEmbeddable {
		private Integer integerField;
		@JdbcTypeCode(SqlTypes.JSON)
		private DoubleNested doubleNested;

		public SimpleEmbeddable() {
		}

		public SimpleEmbeddable(Integer integerField, String leaf) {
			this.integerField = integerField;
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

			if ( !Objects.equals( integerField, that.integerField ) ) {
				return false;
			}
			return Objects.equals( doubleNested, that.doubleNested );
		}

		@Override
		public int hashCode() {
			int result = integerField != null ? integerField.hashCode() : 0;
			result = 31 * result + ( doubleNested != null ? doubleNested.hashCode() : 0 );
			return result;
		}
	}

	@Embeddable
	public static class DoubleNested {
		@JdbcTypeCode(SqlTypes.JSON)
		private Nested theNested;

		public DoubleNested() {
		}

		public DoubleNested(String leaf) {
			this.theNested = new Nested( leaf );
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

			return Objects.equals( theNested, that.theNested );
		}

		@Override
		public int hashCode() {
			return theNested != null ? theNested.hashCode() : 0;
		}
	}

	@Embeddable
	public static class Nested {
		@JdbcTypeCode(SqlTypes.JSON)
		private Leaf theLeaf;

		public Nested() {
		}

		public Nested(String stringField) {
			this.theLeaf = new Leaf( stringField );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Nested nested = (Nested) o;

			return Objects.equals( theLeaf, nested.theLeaf );
		}

		@Override
		public int hashCode() {
			return theLeaf != null ? theLeaf.hashCode() : 0;
		}
	}

	@Embeddable
	public static class Leaf {
		private String stringField;

		public Leaf() {
		}

		public Leaf(String stringField) {
			this.stringField = stringField;
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

			return Objects.equals( stringField, leaf.stringField );
		}

		@Override
		public int hashCode() {
			return stringField != null ? stringField.hashCode() : 0;
		}
	}
}
