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
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
public class JsonEmbeddableArrayTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				JsonEmbeddableArrayTest.JsonArrayHolder.class
		};
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					session.persist( new JsonArrayHolder( 1L, EmbeddableAggregate.createAggregateArray1() ));
					session.persist( new JsonArrayHolder( 2L, EmbeddableAggregate.createAggregateArray2() ));
				}
		);
	}

	@AfterEach
	protected void cleanupTest() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from JsonArrayHolder h" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					JsonArrayHolder jsonArrayHolder = entityManager.find( JsonArrayHolder.class, 1L );
					jsonArrayHolder.setAggregateArray( EmbeddableAggregate.createAggregateArray2() );
					entityManager.flush();
					entityManager.clear();
					EmbeddableAggregate.assertArraysEquals(
							EmbeddableAggregate.createAggregateArray2(),
							entityManager.find( JsonArrayHolder.class, 1L ).getAggregateArray() );
				}
		);
	}

	@Test
	public void testFetch() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<JsonArrayHolder> jsonArrayHolders = entityManager.createQuery( "from JsonArrayHolder b where b.id = 1", JsonArrayHolder.class ).getResultList();
					assertEquals( 1, jsonArrayHolders.size() );
					assertEquals( 1L, jsonArrayHolders.get( 0 ).getId() );
					EmbeddableAggregate.assertArraysEquals( EmbeddableAggregate.createAggregateArray1(), jsonArrayHolders.get( 0 ).getAggregateArray() );
				}
		);
	}

	@Test
	public void testFetchNull() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<JsonArrayHolder> jsonArrayHolders = entityManager.createQuery( "from JsonArrayHolder b where b.id = 2", JsonArrayHolder.class ).getResultList();
					assertEquals( 1, jsonArrayHolders.size() );
					assertEquals( 2L, jsonArrayHolders.get( 0 ).getId() );
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregateArray2()[0], jsonArrayHolders.get( 0 ).getAggregateArray()[0] );
				}
		);
	}

	@Test
	public void testDomainResult() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<EmbeddableAggregate[]> structs = entityManager.createQuery( "select b.aggregateArray from JsonArrayHolder b where b.id = 1", EmbeddableAggregate[].class ).getResultList();
					assertEquals( 1, structs.size() );
					EmbeddableAggregate.assertArraysEquals( EmbeddableAggregate.createAggregateArray1(), structs.get( 0 ) );
				}
		);
	}

	@Test
	public void testSelectionItems() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<Tuple> tuples = entityManager.createQuery(
							"select " +
									"b.aggregateArray[0].theInt," +
									"b.aggregateArray[0].theDouble," +
									"b.aggregateArray[0].theBoolean," +
									"b.aggregateArray[0].theNumericBoolean," +
									"b.aggregateArray[0].theStringBoolean," +
									"b.aggregateArray[0].theString," +
									"b.aggregateArray[0].theInteger," +
									"b.aggregateArray[0].theUrl," +
									"b.aggregateArray[0].theClob," +
									"b.aggregateArray[0].theBinary," +
									"b.aggregateArray[0].theDate," +
									"b.aggregateArray[0].theTime," +
									"b.aggregateArray[0].theTimestamp," +
									"b.aggregateArray[0].theInstant," +
									"b.aggregateArray[0].theUuid," +
									"b.aggregateArray[0].gender," +
									"b.aggregateArray[0].convertedGender," +
									"b.aggregateArray[0].ordinalGender," +
									"b.aggregateArray[0].theDuration," +
									"b.aggregateArray[0].theLocalDateTime," +
									"b.aggregateArray[0].theLocalDate," +
									"b.aggregateArray[0].theLocalTime," +
									"b.aggregateArray[0].theZonedDateTime," +
									"b.aggregateArray[0].theOffsetDateTime," +
									"b.aggregateArray[0].mutableValue " +
									"from JsonArrayHolder b where b.id = 1",
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
				}
		);
	}

	@Test
	public void testDeleteWhere() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "delete JsonArrayHolder b where b.aggregateArray is not null" ).executeUpdate();
					assertNull( entityManager.find( JsonArrayHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update JsonArrayHolder b set b.aggregateArray = null" ).executeUpdate();
					assertNull( entityManager.find( JsonArrayHolder.class, 1L ).getAggregateArray() );
				}
		);
	}

	@Test
	public void testUpdateAggregateMember() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update JsonArrayHolder b set b.aggregateArray[0].theString = null where b.id = 1" ).executeUpdate();
					EmbeddableAggregate[] struct = EmbeddableAggregate.createAggregateArray1();
					struct[0].setTheString( null );
					EmbeddableAggregate.assertArraysEquals( struct, entityManager.find( JsonArrayHolder.class, 1L ).getAggregateArray() );
				}
		);
	}

	@Test
	public void testUpdateMultipleAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update JsonArrayHolder b set b.aggregateArray.theString = null, b.aggregateArray[0].theUuid = null" ).executeUpdate();
					EmbeddableAggregate[] struct = EmbeddableAggregate.createAggregateArray1();
					struct[0].setTheString( null );
					struct[0].setTheUuid( null );
					EmbeddableAggregate.assertArraysEquals( struct, entityManager.find( JsonArrayHolder.class, 1L ).getAggregateArray() );
				}
		);
	}

	@Test
	public void testUpdateAllAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					EmbeddableAggregate[] struct = EmbeddableAggregate.createAggregateArray1();
					entityManager.createMutationQuery(
									"update JsonArrayHolder b set " +
											"b.aggregateArray[0].theInt = :theInt," +
											"b.aggregateArray[0].theDouble = :theDouble," +
											"b.aggregateArray[0].theBoolean = :theBoolean," +
											"b.aggregateArray[0].theNumericBoolean = :theNumericBoolean," +
											"b.aggregateArray[0].theStringBoolean = :theStringBoolean," +
											"b.aggregateArray[0].theString = :theString," +
											"b.aggregateArray[0].theInteger = :theInteger," +
											"b.aggregateArray[0].theUrl = :theUrl," +
											"b.aggregateArray[0].theClob = :theClob," +
											"b.aggregateArray[0].theBinary = :theBinary," +
											"b.aggregateArray[0].theDate = :theDate," +
											"b.aggregateArray[0].theTime = :theTime," +
											"b.aggregateArray[0].theTimestamp = :theTimestamp," +
											"b.aggregateArray[0].theInstant = :theInstant," +
											"b.aggregateArray[0].theUuid = :theUuid," +
											"b.aggregateArray[0].gender = :gender," +
											"b.aggregateArray[0].convertedGender = :convertedGender," +
											"b.aggregateArray[0].ordinalGender = :ordinalGender," +
											"b.aggregateArray[0].theDuration = :theDuration," +
											"b.aggregateArray[0].theLocalDateTime = :theLocalDateTime," +
											"b.aggregateArray[0].theLocalDate = :theLocalDate," +
											"b.aggregateArray[0].theLocalTime = :theLocalTime," +
											"b.aggregateArray[0].theZonedDateTime = :theZonedDateTime," +
											"b.aggregateArray[0].theOffsetDateTime = :theOffsetDateTime," +
											"b.aggregateArray[0].mutableValue = :mutableValue " +
											"where b.id = 2"
							)
							.setParameter( "theInt", struct[0].getTheInt() )
							.setParameter( "theDouble", struct[0].getTheDouble() )
							.setParameter( "theBoolean", struct[0].isTheBoolean() )
							.setParameter( "theNumericBoolean", struct[0].isTheNumericBoolean() )
							.setParameter( "theStringBoolean", struct[0].isTheStringBoolean() )
							.setParameter( "theString", struct[0].getTheString() )
							.setParameter( "theInteger", struct[0].getTheInteger() )
							.setParameter( "theUrl", struct[0].getTheUrl() )
							.setParameter( "theClob", struct[0].getTheClob() )
							.setParameter( "theBinary", struct[0].getTheBinary() )
							.setParameter( "theDate", struct[0].getTheDate() )
							.setParameter( "theTime", struct[0].getTheTime() )
							.setParameter( "theTimestamp", struct[0].getTheTimestamp() )
							.setParameter( "theInstant", struct[0].getTheInstant() )
							.setParameter( "theUuid", struct[0].getTheUuid() )
							.setParameter( "gender", struct[0].getGender() )
							.setParameter( "convertedGender", struct[0].getConvertedGender() )
							.setParameter( "ordinalGender", struct[0].getOrdinalGender() )
							.setParameter( "theDuration", struct[0].getTheDuration() )
							.setParameter( "theLocalDateTime", struct[0].getTheLocalDateTime() )
							.setParameter( "theLocalDate", struct[0].getTheLocalDate() )
							.setParameter( "theLocalTime", struct[0].getTheLocalTime() )
							.setParameter( "theZonedDateTime", struct[0].getTheZonedDateTime() )
							.setParameter( "theOffsetDateTime", struct[0].getTheOffsetDateTime() )
							.setParameter( "mutableValue", struct[0].getMutableValue() )
							.executeUpdate();
					EmbeddableAggregate.assertArraysEquals( EmbeddableAggregate.createAggregateArray1(), entityManager.find( JsonArrayHolder.class, 2L ).getAggregateArray() );
				}
		);
	}

	@Entity(name = "JsonArrayHolder")
	public static class JsonArrayHolder {

		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.JSON_ARRAY)
		private EmbeddableAggregate [] aggregateArray;

		public JsonArrayHolder() {
		}

		public JsonArrayHolder(Long id, EmbeddableAggregate[] aggregateArray) {
			this.id = id;
			this.aggregateArray = aggregateArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EmbeddableAggregate[] getAggregateArray() {
			return aggregateArray;
		}

		public void setAggregateArray(EmbeddableAggregate[] aggregateArray) {
			this.aggregateArray = aggregateArray;
		}

	}

}
