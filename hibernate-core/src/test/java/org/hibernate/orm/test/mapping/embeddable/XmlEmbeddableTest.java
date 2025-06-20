/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlAggregate.class)
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, matchSubTypes = true,
		reason = "Currently failing on Oracle 23+ due to Bug 37319693 - ORA-00600 with check constraint on xml type")
public class XmlEmbeddableTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			XmlHolder.class
		};
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					session.persist( new XmlHolder( 1L, EmbeddableAggregate.createAggregate1() ) );
					session.persist( new XmlHolder( 2L, EmbeddableAggregate.createAggregate2() ) );
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
					XmlHolder XmlHolder = entityManager.find( XmlHolder.class, 1L );
					XmlHolder.setAggregate( EmbeddableAggregate.createAggregate2() );
					entityManager.flush();
					entityManager.clear();
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate2(), entityManager.find( XmlHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@JiraKey( "HHH-16682" )
	public void testDirtyChecking() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					XmlHolder XmlHolder = entityManager.find( XmlHolder.class, 1L );
					XmlHolder.getAggregate().setTheString( "MyString" );
					entityManager.flush();
					entityManager.clear();
					assertEquals( "MyString", entityManager.find( XmlHolder.class, 1L ).getAggregate().getTheString() );
				}
		);
	}

	@Test
	public void testFetch() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<XmlHolder> XmlHolders = entityManager.createQuery( "from XmlHolder b where b.id = 1", XmlHolder.class ).getResultList();
					assertEquals( 1, XmlHolders.size() );
					assertEquals( 1L, XmlHolders.get( 0 ).getId() );
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate1(), XmlHolders.get( 0 ).getAggregate() );
				}
		);
	}

	@Test
	public void testFetchNull() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<XmlHolder> xmlHolders = entityManager.createQuery( "from XmlHolder b where b.id = 2", XmlHolder.class ).getResultList();
					assertEquals( 1, xmlHolders.size() );
					assertEquals( 2L, xmlHolders.get( 0 ).getId() );
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate2(), xmlHolders.get( 0 ).getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<EmbeddableAggregate> structs = entityManager.createQuery( "select b.aggregate from XmlHolder b where b.id = 1", EmbeddableAggregate.class ).getResultList();
					assertEquals( 1, structs.size() );
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate1(), structs.get( 0 ) );
				}
		);
	}

	@Test
	public void testSelectionItems() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<Tuple> tuples = entityManager.createQuery(
							"select " +
									"b.aggregate.theInt," +
									"b.aggregate.theDouble," +
									"b.aggregate.theBoolean," +
									"b.aggregate.theNumericBoolean," +
									"b.aggregate.theStringBoolean," +
									"b.aggregate.theString," +
									"b.aggregate.theInteger," +
									"b.aggregate.theUrl," +
									"b.aggregate.theClob," +
									"b.aggregate.theBinary," +
									"b.aggregate.theDate," +
									"b.aggregate.theTime," +
									"b.aggregate.theTimestamp," +
									"b.aggregate.theInstant," +
									"b.aggregate.theUuid," +
									"b.aggregate.gender," +
									"b.aggregate.convertedGender," +
									"b.aggregate.ordinalGender," +
									"b.aggregate.theDuration," +
									"b.aggregate.theLocalDateTime," +
									"b.aggregate.theLocalDate," +
									"b.aggregate.theLocalTime," +
									"b.aggregate.theZonedDateTime," +
									"b.aggregate.theOffsetDateTime," +
									"b.aggregate.mutableValue " +
									"from XmlHolder b where b.id = 1",
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
					entityManager.createMutationQuery( "delete XmlHolder b where b.aggregate is not null" ).executeUpdate();
					assertNull( entityManager.find( XmlHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update XmlHolder b set b.aggregate = null" ).executeUpdate();
					assertNull( entityManager.find( XmlHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlComponentUpdate.class)
	public void testUpdateAggregateMember() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update XmlHolder b set b.aggregate.theString = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					EmbeddableAggregate.assertEquals( struct, entityManager.find( XmlHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlComponentUpdate.class)
	public void testUpdateMultipleAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update XmlHolder b set b.aggregate.theString = null, b.aggregate.theUuid = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					struct.setTheUuid( null );
					EmbeddableAggregate.assertEquals( struct, entityManager.find( XmlHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlComponentUpdate.class)
	public void testUpdateAllAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					entityManager.createMutationQuery(
									"update XmlHolder b set " +
											"b.aggregate.theInt = :theInt," +
											"b.aggregate.theDouble = :theDouble," +
											"b.aggregate.theBoolean = :theBoolean," +
											"b.aggregate.theNumericBoolean = :theNumericBoolean," +
											"b.aggregate.theStringBoolean = :theStringBoolean," +
											"b.aggregate.theString = :theString," +
											"b.aggregate.theInteger = :theInteger," +
											"b.aggregate.theUrl = :theUrl," +
											"b.aggregate.theClob = :theClob," +
											"b.aggregate.theBinary = :theBinary," +
											"b.aggregate.theDate = :theDate," +
											"b.aggregate.theTime = :theTime," +
											"b.aggregate.theTimestamp = :theTimestamp," +
											"b.aggregate.theInstant = :theInstant," +
											"b.aggregate.theUuid = :theUuid," +
											"b.aggregate.gender = :gender," +
											"b.aggregate.convertedGender = :convertedGender," +
											"b.aggregate.ordinalGender = :ordinalGender," +
											"b.aggregate.theDuration = :theDuration," +
											"b.aggregate.theLocalDateTime = :theLocalDateTime," +
											"b.aggregate.theLocalDate = :theLocalDate," +
											"b.aggregate.theLocalTime = :theLocalTime," +
											"b.aggregate.theZonedDateTime = :theZonedDateTime," +
											"b.aggregate.theOffsetDateTime = :theOffsetDateTime," +
											"b.aggregate.mutableValue = :mutableValue " +
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
							.executeUpdate();
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate1(), entityManager.find( XmlHolder.class, 2L ).getAggregate() );
				}
		);
	}

	//tag::embeddable-xml-type-mapping-example[]
	@Entity(name = "XmlHolder")
	public static class XmlHolder {

		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.SQLXML)
		private EmbeddableAggregate aggregate;

		//end::embeddable-xml-type-mapping-example[]
		//Getters and setters are omitted for brevity

		public XmlHolder() {
		}

		public XmlHolder(Long id, EmbeddableAggregate aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EmbeddableAggregate getAggregate() {
			return aggregate;
		}

		public void setAggregate(EmbeddableAggregate aggregate) {
			this.aggregate = aggregate;
		}

	//tag::embeddable-xml-type-mapping-example[]
	}

	//end::embeddable-xml-type-mapping-example[]
}
