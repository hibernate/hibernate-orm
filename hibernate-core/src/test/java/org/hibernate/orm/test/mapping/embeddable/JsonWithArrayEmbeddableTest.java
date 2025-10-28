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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-15862")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
// Don't reorder columns in the types here to avoid the need to rewrite the test
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.COLUMN_ORDERING_STRATEGY, value = "legacy")
})
@DomainModel(annotatedClasses = JsonWithArrayEmbeddableTest.JsonHolder.class)
@SessionFactory
public class JsonWithArrayEmbeddableTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new JsonHolder( 1L, EmbeddableWithArrayAggregate.createAggregate1() ) );
					session.persist( new JsonHolder( 2L, EmbeddableWithArrayAggregate.createAggregate2() ) );
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
				session -> {
					JsonHolder jsonHolder = session.find( JsonHolder.class, 1L );
					jsonHolder.setAggregate( EmbeddableWithArrayAggregate.createAggregate2() );
					session.flush();
					session.clear();
					EmbeddableWithArrayAggregate.assertEquals( EmbeddableWithArrayAggregate.createAggregate2(), session.find( JsonHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testFetch(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<JsonHolder> jsonHolders = session.createQuery( "from JsonHolder b where b.id = 1", JsonHolder.class ).getResultList();
					assertEquals( 1, jsonHolders.size() );
					assertEquals( 1L, jsonHolders.get( 0 ).getId() );
					EmbeddableWithArrayAggregate.assertEquals( EmbeddableWithArrayAggregate.createAggregate1(), jsonHolders.get( 0 ).getAggregate() );
				}
		);
	}

	@Test
	public void testFetchNull(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<JsonHolder> jsonHolders = session.createQuery( "from JsonHolder b where b.id = 2", JsonHolder.class ).getResultList();
					assertEquals( 1, jsonHolders.size() );
					assertEquals( 2L, jsonHolders.get( 0 ).getId() );
					EmbeddableWithArrayAggregate.assertEquals( EmbeddableWithArrayAggregate.createAggregate2(), jsonHolders.get( 0 ).getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<EmbeddableWithArrayAggregate> structs = session.createQuery( "select b.aggregate from JsonHolder b where b.id = 1", EmbeddableWithArrayAggregate.class ).getResultList();
					assertEquals( 1, structs.size() );
					EmbeddableWithArrayAggregate.assertEquals( EmbeddableWithArrayAggregate.createAggregate1(), structs.get( 0 ) );
				}
		);
	}

	@Test
	public void testSelectionItems(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<Tuple> tuples = session.createQuery(
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
									"from JsonHolder b where b.id = 1",
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
				}
		);
	}

	@Test
	public void testDeleteWhere(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete JsonHolder b where b.aggregate is not null" ).executeUpdate();
					assertNull( session.find( JsonHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "update JsonHolder b set b.aggregate = null" ).executeUpdate();
					assertNull( session.find( JsonHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonComponentUpdate.class)
	public void testUpdateAggregateMember(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "update JsonHolder b set b.aggregate.theString = null" ).executeUpdate();
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					struct.setTheString( null );
					EmbeddableWithArrayAggregate.assertEquals( struct, session.find( JsonHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonComponentUpdate.class)
	public void testUpdateMultipleAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "update JsonHolder b set b.aggregate.theString = null, b.aggregate.theUuid = null" ).executeUpdate();
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					struct.setTheString( null );
					struct.setTheUuid( null );
					EmbeddableWithArrayAggregate.assertEquals( struct, session.find( JsonHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonComponentUpdate.class)
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	public void testUpdateAllAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					session.createMutationQuery(
									"update JsonHolder b set " +
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
							.executeUpdate();
					EmbeddableWithArrayAggregate.assertEquals( EmbeddableWithArrayAggregate.createAggregate1(), session.find( JsonHolder.class, 2L ).getAggregate() );
				}
		);
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {

		@Id
		private Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		private EmbeddableWithArrayAggregate aggregate;

		//Getters and setters are omitted for brevity

		public JsonHolder() {
		}

		public JsonHolder(Long id, EmbeddableWithArrayAggregate aggregate) {
			this.id = id;
			this.aggregate = aggregate;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EmbeddableWithArrayAggregate getAggregate() {
			return aggregate;
		}

		public void setAggregate(EmbeddableWithArrayAggregate aggregate) {
			this.aggregate = aggregate;
		}

	}
}
