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
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.procedure.ProcedureParameter;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@JiraKey("HHH-15862")
@RequiresDialect( PostgreSQLDialect.class )
@RequiresDialect( OracleDialect.class )
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = AdditionalMappingContributor.class,
				impl = StructWithArrayEmbeddableTest.class
		),
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
// Don't reorder columns in the types here to avoid the need to rewrite the test
@ServiceRegistry(settings = @Setting(name = AvailableSettings.COLUMN_ORDERING_STRATEGY, value = "legacy"))
@DomainModel(annotatedClasses = StructWithArrayEmbeddableTest.StructHolder.class)
@SessionFactory
public class StructWithArrayEmbeddableTest implements AdditionalMappingContributor {
	@Override
	public void contribute(
			AdditionalMappingContributions contributions,
			InFlightMetadataCollector metadata,
			ResourceStreamLocator resourceStreamLocator,
			MetadataBuildingContext buildingContext) {
		final Namespace namespace = new Namespace(
				PhysicalNamingStrategyStandardImpl.INSTANCE,
				null,
				new Namespace.Name( null, null )
		);

		//---------------------------------------------------------
		// PostgreSQL
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgreSQL structFunction",
						namespace,
						"create function structFunction() returns structType as $$ declare result structType; begin result.theBinary = array[bytea '\\x01']; result.theString = array['ABC']; result.theDouble = array[0]; result.theInt = array[0]; result.theLocalDateTime = array[timestamp '2022-12-01 01:00:00']; result.theUuid = array['53886a8a-7082-4879-b430-25cb94415be8'::uuid]; return result; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgreSQL structProcedure",
						namespace,
						"create procedure structProcedure(INOUT result structType) AS $$ declare res structType; begin res.theBinary = array[bytea '\\x01']; res.theString = array['ABC']; res.theDouble = array[0]; res.theInt = array[0]; res.theLocalDateTime = array[timestamp '2022-12-01 01:00:00']; res.theUuid = array['53886a8a-7082-4879-b430-25cb94415be8'::uuid]; result = res; end $$ language plpgsql",
						"drop procedure structProcedure",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// PostgresPlus
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structFunction",
						namespace,
						"create function structFunction() returns structType as $$ declare result structType; begin result.theBinary = array[bytea '\\x01']; result.theString = array['ABC']; result.theDouble = array[0]; result.theInt = array[0]; result.theLocalDateTime = array[timestamp '2022-12-01 01:00:00']; result.theUuid = array['53886a8a-7082-4879-b430-25cb94415be8'::uuid]; return result; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structProcedure",
						namespace,
						"create procedure structProcedure(result INOUT structType) AS $$ declare res structType; begin res.theBinary = array[bytea '\\x01']; res.theString = array['ABC']; res.theDouble = array[0]; res.theInt = array[0]; res.theLocalDateTime = array[timestamp '2022-12-01 01:00:00']; res.theUuid = array['53886a8a-7082-4879-b430-25cb94415be8'::uuid]; result = res; end $$ language plpgsql",
						"drop procedure structProcedure",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// Oracle
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle structFunction",
						namespace,
						"create function structFunction return structType is result structType; begin " +
								"result := structType(" +
								"theBinary => byteArrayArray( hextoraw('01') )," +
								"theString => StringArray( 'ABC' )," +
								"theDouble => DoubleArray( 0 )," +
								"theInt => IntegerArray( 0 )," +
								"theLocalDateTime => LocalDateTimeTimestampArray( timestamp '2022-12-01 01:00:00' )," +
								"theUuid => UUIDbyteArrayArray( hextoraw('53886a8a70824879b43025cb94415be8') )," +
								"converted_gender => null," +
								"gender => null," +
								"mutableValue => null," +
								"ordinal_gender => null," +
								"theBoolean => null," +
								"theClob => null," +
								"theDate => null," +
								"theDuration => null," +
								"theInstant => null," +
								"theInteger => null," +
								"theLocalDate => null," +
								"theLocalTime => null," +
								"theNumericBoolean => null," +
								"theOffsetDateTime => null," +
								"theStringBoolean => null," +
								"theTime => null," +
								"theTimestamp => null," +
								"theUrl => null," +
								"theZonedDateTime => null" +
								"); return result; end;",
						"drop function structFunction",
						Set.of( OracleDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle structProcedure",
						namespace,
						"create procedure structProcedure(result OUT structType) AS begin " +
								"result := structType(" +
								"theBinary => byteArrayArray( hextoraw('01') )," +
								"theString => StringArray( 'ABC' )," +
								"theDouble => DoubleArray( 0 )," +
								"theInt => IntegerArray( 0 )," +
								"theLocalDateTime => LocalDateTimeTimestampArray( timestamp '2022-12-01 01:00:00' )," +
								"theUuid => UUIDbyteArrayArray( hextoraw('53886a8a70824879b43025cb94415be8') )," +
								"converted_gender => null," +
								"gender => null," +
								"mutableValue => null," +
								"ordinal_gender => null," +
								"theBoolean => null," +
								"theClob => null," +
								"theDate => null," +
								"theDuration => null," +
								"theInstant => null," +
								"theInteger => null," +
								"theLocalDate => null," +
								"theLocalTime => null," +
								"theNumericBoolean => null," +
								"theOffsetDateTime => null," +
								"theStringBoolean => null," +
								"theTime => null," +
								"theTimestamp => null," +
								"theUrl => null," +
								"theZonedDateTime => null" +
								"); end;",
						"drop procedure structProcedure",
						Set.of( OracleDialect.class.getName() )
				)
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new StructHolder( 1L, EmbeddableWithArrayAggregate.createAggregate1() ) );
					session.persist( new StructHolder( 2L, EmbeddableWithArrayAggregate.createAggregate2() ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from StructHolder h" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StructHolder structHolder = entityManager.find( StructHolder.class, 1L );
					structHolder.setAggregate( EmbeddableWithArrayAggregate.createAggregate2() );
					entityManager.flush();
					entityManager.clear();
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate2(), entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testFetch(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<StructHolder> structHolders = entityManager.createQuery( "from StructHolder b where b.id = 1", StructHolder.class ).getResultList();
					assertEquals( 1, structHolders.size() );
					assertEquals( 1L, structHolders.get( 0 ).getId() );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), structHolders.get( 0 ).getAggregate() );
				}
		);
	}

	@Test
	public void testFetchNull(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<StructHolder> structHolders = entityManager.createQuery( "from StructHolder b where b.id = 2", StructHolder.class ).getResultList();
					assertEquals( 1, structHolders.size() );
					assertEquals( 2L, structHolders.get( 0 ).getId() );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate2(), structHolders.get( 0 ).getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<EmbeddableWithArrayAggregate> structs = entityManager.createQuery( "select b.aggregate from StructHolder b where b.id = 1", EmbeddableWithArrayAggregate.class ).getResultList();
					assertEquals( 1, structs.size() );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), structs.get( 0 ) );
				}
		);
	}

	@Test
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "External driver fix required")
	public void testSelectionItems(SessionFactoryScope scope) {
		scope.inSession(
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
				}
		);
	}

	@Test
	public void testDeleteWhere(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "delete StructHolder b where b.aggregate is not null" ).executeUpdate();
					assertNull( entityManager.find( StructHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.aggregate = null" ).executeUpdate();
					assertNull( entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testUpdateAggregateMember(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.aggregate.theString = null" ).executeUpdate();
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					struct.setTheString( null );
					assertStructEquals( struct, entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testUpdateMultipleAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.aggregate.theString = null, b.aggregate.theUuid = null" ).executeUpdate();
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					struct.setTheString( null );
					struct.setTheUuid( null );
					assertStructEquals( struct, entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testUpdateAllAggregateMembers(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate1();
					entityManager.createMutationQuery(
							"update StructHolder b set " +
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
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), entityManager.find( StructHolder.class, 2L ).getAggregate() );
				}
		);
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Object> resultList = entityManager.createNativeQuery(
									"select b.aggregate from StructHolder b where b.id = 1", Object.class
							)
							.getResultList();
					assertEquals( 1, resultList.size() );
					assertInstanceOf( EmbeddableWithArrayAggregate.class, resultList.get( 0 ) );
					EmbeddableWithArrayAggregate struct = (EmbeddableWithArrayAggregate) resultList.get( 0 );
					assertStructEquals( EmbeddableWithArrayAggregate.createAggregate1(), struct );
				}
		);
	}

	@Test
	public void testFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ProcedureCall structFunction = entityManager.createStoredProcedureCall( "structFunction" )
							.markAsFunctionCall( EmbeddableWithArrayAggregate.class );
					//noinspection unchecked
					final List<Object> resultList = structFunction.getResultList();
					assertEquals( 1, resultList.size() );
					assertInstanceOf( EmbeddableWithArrayAggregate.class, resultList.get( 0 ) );
					EmbeddableWithArrayAggregate result = (EmbeddableWithArrayAggregate) resultList.get( 0 );
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate3();
					assertStructEquals( struct, result );
				}
		);
	}

	@Test
	public void testProcedure(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Dialect dialect = entityManager.getJdbcServices().getDialect();
					final ParameterMode parameterMode;
					if ( dialect instanceof PostgreSQLDialect ) {
						parameterMode = ParameterMode.INOUT;
					}
					else {
						parameterMode = ParameterMode.OUT;
					}
					ProcedureCall structFunction = entityManager.createStoredProcedureCall( "structProcedure" );
					ProcedureParameter<EmbeddableWithArrayAggregate> resultParameter = structFunction.registerParameter(
							"result",
							EmbeddableWithArrayAggregate.class,
							parameterMode
					);
					structFunction.setParameter( resultParameter, null );
					EmbeddableWithArrayAggregate result = structFunction.getOutputs().getOutputParameterValue( resultParameter );
					EmbeddableWithArrayAggregate struct = EmbeddableWithArrayAggregate.createAggregate3();
					assertStructEquals( struct, result );
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
		@Struct(name = "structType")
		private EmbeddableWithArrayAggregate aggregate;

		//Getters and setters are omitted for brevity

		public StructHolder() {
		}

		public StructHolder(Long id, EmbeddableWithArrayAggregate aggregate) {
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
