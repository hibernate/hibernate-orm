/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = AdditionalMappingContributor.class,
				impl = StructEmbeddableArrayTest.class
		),
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
@DomainModel(annotatedClasses = StructEmbeddableArrayTest.StructHolder.class)
@SessionFactory
@RequiresDialect( PostgreSQLDialect.class )
@RequiresDialect( OracleDialect.class )
public class StructEmbeddableArrayTest implements AdditionalMappingContributor {

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
						"create function structFunction() returns structType array as $$ declare result structType; begin result.theBinary = bytea '\\x01'; result.theString = 'ABC'; result.theDouble = 0; result.theInt = 0; result.theLocalDateTime = timestamp '2022-12-01 01:00:00'; result.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; return array[result]; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgreSQL structProcedure",
						namespace,
						"create procedure structProcedure(INOUT result structType array) AS $$ declare res structType; begin res.theBinary = bytea '\\x01'; res.theString = 'ABC'; res.theDouble = 0; res.theInt = 0; res.theLocalDateTime = timestamp '2022-12-01 01:00:00'; res.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; result = array[res]; end $$ language plpgsql",
						"drop procedure structProcedure",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// PostgrePlus
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structFunction",
						namespace,
						"create function structFunction() returns structType array as $$ declare result structType; begin result.theBinary = bytea '\\x01'; result.theString = 'ABC'; result.theDouble = 0; result.theInt = 0; result.theLocalDateTime = timestamp '2022-12-01 01:00:00'; result.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; return array[result]; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structProcedure",
						namespace,
						"create procedure structProcedure(result INOUT structType array) AS $$ declare res structType; begin res.theBinary = bytea '\\x01'; res.theString = 'ABC'; res.theDouble = 0; res.theInt = 0; res.theLocalDateTime = timestamp '2022-12-01 01:00:00'; res.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; result = array[res]; end $$ language plpgsql",
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
						"create function structFunction return structTypeArray is result structTypeArray; begin " +
								"result := structTypeArray(structType(" +
								"theBinary => hextoraw('01')," +
								"theString => 'ABC'," +
								"theDouble => 0," +
								"theInt => 0," +
								"theLocalDateTime => timestamp '2022-12-01 01:00:00'," +
								"theUuid => hextoraw('53886a8a70824879b43025cb94415be8')," +
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
								")); return result; end;",
						"drop function structFunction",
						Set.of( OracleDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle structProcedure",
						namespace,
						"create procedure structProcedure(result OUT structTypeArray) AS begin " +
								"result := structTypeArray(structType(" +
								"theBinary => hextoraw('01')," +
								"theString => 'ABC'," +
								"theDouble => 0," +
								"theInt => 0," +
								"theLocalDateTime => timestamp '2022-12-01 01:00:00'," +
								"theUuid => hextoraw('53886a8a70824879b43025cb94415be8')," +
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
								")); end;",
						"drop procedure structProcedure",
						Set.of( OracleDialect.class.getName() )
				)
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new StructHolder( 1L, EmbeddableAggregate.createAggregate1() ) );
					session.persist( new StructHolder( 2L, EmbeddableAggregate.createAggregate2() ) );
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
					structHolder.setAggregate( EmbeddableAggregate.createAggregate2() );
					entityManager.flush();
					entityManager.clear();
					assertStructEquals( EmbeddableAggregate.createAggregate2(), entityManager.find( StructHolder.class, 1L ).getAggregate() );
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
					assertStructEquals( EmbeddableAggregate.createAggregate1(), structHolders.get( 0 ).getAggregate() );
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
					assertStructEquals( EmbeddableAggregate.createAggregate2(), structHolders.get( 0 ).getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult(SessionFactoryScope scope) {
		scope.inSession(
				entityManager -> {
					List<EmbeddableAggregate[]> structs = entityManager.createQuery( "select b.aggregate from StructHolder b where b.id = 1", EmbeddableAggregate[].class ).getResultList();
					assertEquals( 1, structs.size() );
					assertStructEquals( new EmbeddableAggregate[]{ EmbeddableAggregate.createAggregate1() }, structs.get( 0 ) );
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
									"b.aggregate[1].theInt," +
									"b.aggregate[1].theDouble," +
									"b.aggregate[1].theBoolean," +
									"b.aggregate[1].theNumericBoolean," +
									"b.aggregate[1].theStringBoolean," +
									"b.aggregate[1].theString," +
									"b.aggregate[1].theInteger," +
									"b.aggregate[1].theUrl," +
									"b.aggregate[1].theClob," +
									"b.aggregate[1].theBinary," +
									"b.aggregate[1].theDate," +
									"b.aggregate[1].theTime," +
									"b.aggregate[1].theTimestamp," +
									"b.aggregate[1].theInstant," +
									"b.aggregate[1].theUuid," +
									"b.aggregate[1].gender," +
									"b.aggregate[1].convertedGender," +
									"b.aggregate[1].ordinalGender," +
									"b.aggregate[1].theDuration," +
									"b.aggregate[1].theLocalDateTime," +
									"b.aggregate[1].theLocalDate," +
									"b.aggregate[1].theLocalTime," +
									"b.aggregate[1].theZonedDateTime," +
									"b.aggregate[1].theOffsetDateTime," +
									"b.aggregate[1].mutableValue " +
									"from StructHolder b where b.id = 1",
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
					assertNull( entityManager.find( StructHolder.class, 1L ).aggregate );
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-18051")
	public void testUpdateAggregateMember(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.aggregate[1].theString = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
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
					entityManager.createMutationQuery( "update StructHolder b set b.aggregate[1].theString = null, b.aggregate[1].theUuid = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
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
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					entityManager.createMutationQuery(
							"update StructHolder b set " +
									"b.aggregate[1].theInt = :theInt," +
									"b.aggregate[1].theDouble = :theDouble," +
									"b.aggregate[1].theBoolean = :theBoolean," +
									"b.aggregate[1].theNumericBoolean = :theNumericBoolean," +
									"b.aggregate[1].theStringBoolean = :theStringBoolean," +
									"b.aggregate[1].theString = :theString," +
									"b.aggregate[1].theInteger = :theInteger," +
									"b.aggregate[1].theUrl = :theUrl," +
									"b.aggregate[1].theClob = :theClob," +
									"b.aggregate[1].theBinary = :theBinary," +
									"b.aggregate[1].theDate = :theDate," +
									"b.aggregate[1].theTime = :theTime," +
									"b.aggregate[1].theTimestamp = :theTimestamp," +
									"b.aggregate[1].theInstant = :theInstant," +
									"b.aggregate[1].theUuid = :theUuid," +
									"b.aggregate[1].gender = :gender," +
									"b.aggregate[1].convertedGender = :convertedGender," +
									"b.aggregate[1].ordinalGender = :ordinalGender," +
									"b.aggregate[1].theDuration = :theDuration," +
									"b.aggregate[1].theLocalDateTime = :theLocalDateTime," +
									"b.aggregate[1].theLocalDate = :theLocalDate," +
									"b.aggregate[1].theLocalTime = :theLocalTime," +
									"b.aggregate[1].theZonedDateTime = :theZonedDateTime," +
									"b.aggregate[1].theOffsetDateTime = :theOffsetDateTime," +
									"b.aggregate[1].mutableValue = :mutableValue " +
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
					assertStructEquals( EmbeddableAggregate.createAggregate1(), entityManager.find( StructHolder.class, 2L ).getAggregate() );
				}
		);
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					//noinspection unchecked
					List<Object> resultList = entityManager.createNativeQuery(
									"select b.aggregate from StructHolder b where b.id = 1",
									// Using Object.class on purpose to verify Dialect#resolveSqlTypeDescriptor works
									Object.class
							)
							.getResultList();
					assertEquals( 1, resultList.size() );
					assertInstanceOf( EmbeddableAggregate[].class, resultList.get( 0 ) );
					EmbeddableAggregate[] structs = (EmbeddableAggregate[]) resultList.get( 0 );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), structs[0] );
				}
		);
	}

	@Test
	public void testFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					ProcedureCall structFunction = entityManager.createStoredProcedureCall( "structFunction" )
							.markAsFunctionCall( EmbeddableAggregate[].class );
					//noinspection unchecked
					final List<Object> resultList = structFunction.getResultList();
					assertEquals( 1, resultList.size() );
					assertInstanceOf( EmbeddableAggregate[].class, resultList.get( 0 ) );
					EmbeddableAggregate[] result = (EmbeddableAggregate[]) resultList.get( 0 );
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate3();
					assertEquals( 1, result.length );
					assertStructEquals( struct, result[0] );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, majorVersion = 10, reason = "Procedures were only introduced in version 11")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, majorVersion = 10, reason = "Procedures were only introduced in version 11")
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
					ProcedureParameter<EmbeddableAggregate[]> resultParameter = structFunction.registerParameter(
							"structType",
							EmbeddableAggregate[].class,
							parameterMode
					);
					structFunction.setParameter( resultParameter, null );
					EmbeddableAggregate[] result = structFunction.getOutputs().getOutputParameterValue( resultParameter );
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate3();
					assertEquals( 1, result.length );
					assertStructEquals( struct, result[0] );
				}
		);
	}

	private static void assertStructEquals(EmbeddableAggregate[] struct, EmbeddableAggregate[] struct2) {
		assertEquals( struct.length, struct2.length );
		for ( int i = 0; i < struct.length; i++ ) {
			assertStructEquals( struct[i], struct2[i] );
		}
	}

	private static void assertStructEquals(EmbeddableAggregate struct, EmbeddableAggregate struct2) {
		assertArrayEquals( struct.getTheBinary(), struct2.getTheBinary() );
		assertEquals( struct.getTheString(), struct2.getTheString() );
		assertEquals( struct.getTheLocalDateTime(), struct2.getTheLocalDateTime() );
		assertEquals( struct.getTheUuid(), struct2.getTheUuid() );
	}

	@Entity(name = "StructHolder")
	public static class StructHolder {

		@Id
		private Long id;
		@Struct(name = "structType")
		private EmbeddableAggregate[] aggregate;

		public StructHolder() {
		}

		public StructHolder(Long id, EmbeddableAggregate aggregate) {
			this.id = id;
			this.aggregate = new EmbeddableAggregate[]{ aggregate };
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public EmbeddableAggregate getAggregate() {
			return aggregate[0];
		}

		public void setAggregate(EmbeddableAggregate aggregate) {
			this.aggregate[0] = aggregate;
		}

	}
}
