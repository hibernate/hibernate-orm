/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.sql.Clob;
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
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.procedure.ProcedureParameter;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialect( PostgreSQLDialect.class )
@RequiresDialect( OracleDialect.class )
@RequiresDialect( DB2Dialect.class )
public class NestedStructEmbeddableTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			StructHolder.class
		};
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder ssrBuilder) {
		// Make sure this stuff runs on a dedicated connection pool,
		// otherwise we might run into ORA-21700: object does not exist or is marked for delete
		// because the JDBC connection or database session caches something that should have been invalidated
		ssrBuilder.applySetting( AvailableSettings.CONNECTION_PROVIDER, DriverManagerConnectionProviderImpl.class.getName() );
		// Don't reorder columns in the types here to avoid the need to rewrite the test
		ssrBuilder.applySetting( AvailableSettings.COLUMN_ORDERING_STRATEGY, "legacy" );
		return super.produceServiceRegistry( ssrBuilder );
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		final Namespace namespace = new Namespace(
				PhysicalNamingStrategyStandardImpl.INSTANCE,
				null,
				new Namespace.Name( null, null )
		);

		//---------------------------------------------------------
		// PostgreSQL
		//---------------------------------------------------------

		metadataBuilder.applyAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgreSQL structFunction",
						namespace,
						"create function structFunction() returns theStruct as $$ declare result theStruct; struct structType; begin struct.theBinary = bytea '\\x01'; struct.theString = 'ABC'; struct.theDouble = 0; struct.theInt = 0; struct.theLocalDateTime = timestamp '2022-12-01 01:00:00'; struct.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; result.nested = struct; return result; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);
		metadataBuilder.applyAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgreSQL structProcedure",
						namespace,
						"create procedure structProcedure(OUT result theStruct) AS $$ begin result.nested.theBinary = bytea '\\x01'; result.nested.theString = 'ABC'; result.nested.theDouble = 0; result.nested.theInt = 0; result.nested.theLocalDateTime = timestamp '2022-12-01 01:00:00'; result.nested.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; end $$ language plpgsql",
						"drop procedure structProcedure",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// PostgrePlus
		//---------------------------------------------------------

		metadataBuilder.applyAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structFunction",
						namespace,
						"create function structFunction() returns theStruct as $$ declare result theStruct; struct structType; begin struct.theBinary = bytea '\\x01'; struct.theString = 'ABC'; struct.theDouble = 0; struct.theInt = 0; struct.theLocalDateTime = timestamp '2022-12-01 01:00:00'; struct.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; result.nested = struct; return result; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);
		metadataBuilder.applyAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structProcedure",
						namespace,
						"create procedure structProcedure(result OUT theStruct) AS $$ begin result.nested.theBinary = bytea '\\x01'; result.nested.theString = 'ABC'; result.nested.theDouble = 0; result.nested.theInt = 0; result.nested.theLocalDateTime = timestamp '2022-12-01 01:00:00'; result.nested.theUuid = '53886a8a-7082-4879-b430-25cb94415be8'::uuid; end $$ language plpgsql",
						"drop procedure structProcedure",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// DB2
		//---------------------------------------------------------
		final String binaryType;
		final String binaryLiteralPrefix;
		if ( getDialect().getVersion().isBefore( 11 ) ) {
			binaryType = "char(16) for bit data";
			binaryLiteralPrefix = "x";
		}
		else {
			binaryType = "binary(16)";
			binaryLiteralPrefix = "bx";
		}

		metadataBuilder.applyAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"DB2 structFunction",
						namespace,
						"create function structFunction() returns theStruct language sql RETURN select theStruct()..nested(structType()..theBinary(" + binaryLiteralPrefix + "'01')..theString('ABC')..theDouble(0)..theInt(0)..theLocalDateTime(timestamp '2022-12-01 01:00:00')..theUuid(cast(" + binaryLiteralPrefix + "'" +
								// UUID is already in HEX encoding, but we have to remove the dashes
								"53886a8a-7082-4879-b430-25cb94415be8".replace( "-", "" )
								+ "' as " + binaryType + "))) from (values (1)) t",
						"drop function structFunction",
						Set.of( DB2Dialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// Oracle
		//---------------------------------------------------------

		metadataBuilder.applyAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle structFunction",
						namespace,
						"create function structFunction return theStruct is result theStruct; begin " +
								"result := theStruct(" +
								"stringField => null," +
								"integerField => null," +
								"doubleNested => null," +
								"nested => structType(" +
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
		metadataBuilder.applyAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle structProcedure",
						namespace,
						"create procedure structProcedure(result OUT theStruct) AS begin " +
								"result := theStruct(" +
								"stringField => null," +
								"integerField => null," +
								"doubleNested => null," +
								"nested => structType(" +
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
	public void setUp() {
		inTransaction(
				session -> {
					session.persist( new StructHolder( 1L, "XYZ", 10, "String \"<abc>A&B</abc>\"", EmbeddableAggregate.createAggregate1() ) );
					session.persist( new StructHolder( 2L, null, 20, "String 'abc'", EmbeddableAggregate.createAggregate2() ) );
				}
		);
	}

	@AfterEach
	protected void cleanupTest() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from StructHolder h" ).executeUpdate();
				}
		);
	}

	@Test
	public void testUpdate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					StructHolder structHolder = entityManager.find( StructHolder.class, 1L );
					structHolder.setAggregate( EmbeddableAggregate.createAggregate2() );
					entityManager.flush();
					entityManager.clear();
					structHolder = entityManager.find( StructHolder.class, 1L );
					assertEquals( "XYZ", structHolder.struct.stringField );
					assertEquals( 10, structHolder.struct.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate2(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetch() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<StructHolder> structHolders = entityManager.createQuery( "from StructHolder b where b.id = 1", StructHolder.class ).getResultList();
					assertEquals( 1, structHolders.size() );
					StructHolder structHolder = structHolders.get( 0 );
					assertEquals( 1L, structHolder.getId() );
					assertEquals( "XYZ", structHolder.struct.stringField );
					assertEquals( 10, structHolder.struct.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", structHolder.struct.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testFetchNull() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<StructHolder> structHolders = entityManager.createQuery( "from StructHolder b where b.id = 2", StructHolder.class ).getResultList();
					assertEquals( 1, structHolders.size() );
					StructHolder structHolder = structHolders.get( 0 );
					assertEquals( 2L, structHolder.getId() );
					assertNull( structHolder.struct.stringField );
					assertEquals( 20, structHolder.struct.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate2(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testDomainResult() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<TheStruct> structs = entityManager.createQuery( "select b.struct from StructHolder b where b.id = 1", TheStruct.class ).getResultList();
					assertEquals( 1, structs.size() );
					TheStruct theStruct = structs.get( 0 );
					assertEquals( "XYZ", theStruct.stringField );
					assertEquals( 10, theStruct.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", theStruct.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), theStruct.nested );
				}
		);
	}

	@Test
	public void testSelectionItems() {
		sessionFactoryScope().inSession(
				entityManager -> {
					List<Tuple> tuples = entityManager.createQuery(
							"select " +
									"b.struct.nested.theInt," +
									"b.struct.nested.theDouble," +
									"b.struct.nested.theBoolean," +
									"b.struct.nested.theNumericBoolean," +
									"b.struct.nested.theStringBoolean," +
									"b.struct.nested.theString," +
									"b.struct.nested.theInteger," +
									"b.struct.nested.theClob," +
									"b.struct.nested.theBinary," +
									"b.struct.nested.theDate," +
									"b.struct.nested.theTime," +
									"b.struct.nested.theTimestamp," +
									"b.struct.nested.theInstant," +
									"b.struct.nested.theUuid," +
									"b.struct.nested.gender," +
									"b.struct.nested.convertedGender," +
									"b.struct.nested.ordinalGender," +
									"b.struct.nested.theDuration," +
									"b.struct.nested.theLocalDateTime," +
									"b.struct.nested.theLocalDate," +
									"b.struct.nested.theLocalTime," +
									"b.struct.nested.theZonedDateTime," +
									"b.struct.nested.theOffsetDateTime," +
									"b.struct.nested.mutableValue," +
									"b.struct.simpleEmbeddable," +
									"b.struct.simpleEmbeddable.doubleNested," +
									"b.struct.simpleEmbeddable.doubleNested.theNested," +
									"b.struct.simpleEmbeddable.doubleNested.theNested.theLeaf " +
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
					struct.setTheClob( tuple.get( 7, Clob.class ) );
					struct.setTheBinary( tuple.get( 8, byte[].class ) );
					struct.setTheDate( tuple.get( 9, Date.class ) );
					struct.setTheTime( tuple.get( 10, Time.class ) );
					struct.setTheTimestamp( tuple.get( 11, Timestamp.class ) );
					struct.setTheInstant( tuple.get( 12, Instant.class ) );
					struct.setTheUuid( tuple.get( 13, UUID.class ) );
					struct.setGender( tuple.get( 14, EntityOfBasics.Gender.class ) );
					struct.setConvertedGender( tuple.get( 15, EntityOfBasics.Gender.class ) );
					struct.setOrdinalGender( tuple.get( 16, EntityOfBasics.Gender.class ) );
					struct.setTheDuration( tuple.get( 17, Duration.class ) );
					struct.setTheLocalDateTime( tuple.get( 18, LocalDateTime.class ) );
					struct.setTheLocalDate( tuple.get( 19, LocalDate.class ) );
					struct.setTheLocalTime( tuple.get( 20, LocalTime.class ) );
					struct.setTheZonedDateTime( tuple.get( 21, ZonedDateTime.class ) );
					struct.setTheOffsetDateTime( tuple.get( 22, OffsetDateTime.class ) );
					struct.setMutableValue( tuple.get( 23, MutableValue.class ) );
					EmbeddableAggregate.assertEquals( EmbeddableAggregate.createAggregate1(), struct );

					SimpleEmbeddable simpleEmbeddable = tuple.get( 24, SimpleEmbeddable.class );
					assertEquals( simpleEmbeddable.doubleNested, tuple.get( 25, DoubleNested.class ) );
					assertEquals( simpleEmbeddable.doubleNested.theNested, tuple.get( 26, Nested.class ) );
					assertEquals( simpleEmbeddable.doubleNested.theNested.theLeaf, tuple.get( 27, Leaf.class ) );
					assertEquals( 10, simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
				}
		);
	}

	@Test
	public void testDeleteWhere() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "delete StructHolder b where b.struct is not null" ).executeUpdate();
					assertNull( entityManager.find( StructHolder.class, 1L ) );

				}
		);
	}

	@Test
	public void testUpdateAggregate() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.struct = null" ).executeUpdate();
					assertNull( entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testUpdateAggregateMember() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.struct.nested.theString = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					assertStructEquals( struct, entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testUpdateMultipleAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					entityManager.createMutationQuery( "update StructHolder b set b.struct.nested.theString = null, b.struct.nested.theUuid = null" ).executeUpdate();
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					struct.setTheString( null );
					struct.setTheUuid( null );
					assertStructEquals( struct, entityManager.find( StructHolder.class, 1L ).getAggregate() );
				}
		);
	}

	@Test
	public void testUpdateAllAggregateMembers() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate1();
					entityManager.createMutationQuery(
							"update StructHolder b set " +
									"b.struct.nested.theInt = :theInt," +
									"b.struct.nested.theDouble = :theDouble," +
									"b.struct.nested.theBoolean = :theBoolean," +
									"b.struct.nested.theNumericBoolean = :theNumericBoolean," +
									"b.struct.nested.theStringBoolean = :theStringBoolean," +
									"b.struct.nested.theString = :theString," +
									"b.struct.nested.theInteger = :theInteger," +
									"b.struct.nested.theClob = :theClob," +
									"b.struct.nested.theBinary = :theBinary," +
									"b.struct.nested.theDate = :theDate," +
									"b.struct.nested.theTime = :theTime," +
									"b.struct.nested.theTimestamp = :theTimestamp," +
									"b.struct.nested.theInstant = :theInstant," +
									"b.struct.nested.theUuid = :theUuid," +
									"b.struct.nested.gender = :gender," +
									"b.struct.nested.convertedGender = :convertedGender," +
									"b.struct.nested.ordinalGender = :ordinalGender," +
									"b.struct.nested.theDuration = :theDuration," +
									"b.struct.nested.theLocalDateTime = :theLocalDateTime," +
									"b.struct.nested.theLocalDate = :theLocalDate," +
									"b.struct.nested.theLocalTime = :theLocalTime," +
									"b.struct.nested.theZonedDateTime = :theZonedDateTime," +
									"b.struct.nested.theOffsetDateTime = :theOffsetDateTime," +
									"b.struct.nested.mutableValue = :mutableValue," +
									"b.struct.simpleEmbeddable.integerField = :integerField " +
									"where b.id = 2"
					)
							.setParameter( "theInt", struct.getTheInt() )
							.setParameter( "theDouble", struct.getTheDouble() )
							.setParameter( "theBoolean", struct.isTheBoolean() )
							.setParameter( "theNumericBoolean", struct.isTheNumericBoolean() )
							.setParameter( "theStringBoolean", struct.isTheStringBoolean() )
							.setParameter( "theString", struct.getTheString() )
							.setParameter( "theInteger", struct.getTheInteger() )
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
					StructHolder structHolder = entityManager.find( StructHolder.class, 2L );
					assertEquals( 5, structHolder.struct.simpleEmbeddable.integerField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), structHolder.getAggregate() );
				}
		);
	}

	@Test
	public void testNativeQuery() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					//noinspection unchecked
					List<Object> resultList = entityManager.createNativeQuery(
									"select b.struct from StructHolder b where b.id = 1",
									// DB2 does not support structs on the driver level, and we instead do a XML serialization/deserialization
									// So in order to receive the correct value, we have to specify the actual type that we expect
									getDialect() instanceof DB2Dialect
											? (Class<Object>) (Class<?>) TheStruct.class
											// Using Object.class on purpose to verify Dialect#resolveSqlTypeDescriptor works
											: Object.class
							)
							.getResultList();
					assertEquals( 1, resultList.size() );
					assertInstanceOf( TheStruct.class, resultList.get( 0 ) );
					TheStruct theStruct = (TheStruct) resultList.get( 0 );
					assertEquals( "XYZ", theStruct.stringField );
					assertEquals( 10, theStruct.simpleEmbeddable.integerField );
					assertEquals( "String \"<abc>A&B</abc>\"", theStruct.simpleEmbeddable.doubleNested.theNested.theLeaf.stringField );
					assertStructEquals( EmbeddableAggregate.createAggregate1(), theStruct.nested );
				}
		);
	}

	@Test
	public void testFunction() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					ProcedureCall structFunction = entityManager.createStoredProcedureCall( "structFunction" )
							.markAsFunctionCall( TheStruct.class );
					//noinspection unchecked
					final List<Object> resultList = structFunction.getResultList();
					assertEquals( 1, resultList.size() );
					assertInstanceOf( TheStruct.class, resultList.get( 0 ) );
					TheStruct result = (TheStruct) resultList.get( 0 );
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate3();
					assertStructEquals( struct, result.nested );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, majorVersion = 10, reason = "Procedures were only introduced in version 11")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, majorVersion = 10, reason = "Procedures were only introduced in version 11")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 does not support struct types in procedures")
	public void testProcedure() {
		sessionFactoryScope().inTransaction(
				entityManager -> {
					ProcedureCall structFunction = entityManager.createStoredProcedureCall( "structProcedure" );
					ProcedureParameter<TheStruct> resultParameter = structFunction.registerParameter(
							"structType",
							TheStruct.class,
							ParameterMode.OUT
					);
					structFunction.setParameter( resultParameter, null );
					TheStruct result = structFunction.getOutputs().getOutputParameterValue( resultParameter );
					EmbeddableAggregate struct = EmbeddableAggregate.createAggregate3();
					assertStructEquals( struct, result.nested );
				}
		);
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
		private TheStruct struct;

		public StructHolder() {
		}

		public StructHolder(Long id, String stringField, Integer integerField, String leaf, EmbeddableAggregate aggregate) {
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

		public EmbeddableAggregate getAggregate() {
			return struct == null ? null : struct.nested;
		}

		public void setAggregate(EmbeddableAggregate aggregate) {
			if ( struct == null ) {
				struct = new TheStruct( null, null, null, aggregate );
			}
			else {
				struct.nested = aggregate;
			}
		}

	}

	@Embeddable
	@Struct( name = "theStruct" )
	public static class TheStruct {
		private String stringField;
		private SimpleEmbeddable simpleEmbeddable;
		@Struct(name = "structType")
		private EmbeddableAggregate nested;

		public TheStruct() {
		}

		public TheStruct(String stringField, Integer integerField, String leaf, EmbeddableAggregate nested) {
			this.stringField = stringField;
			this.simpleEmbeddable = new SimpleEmbeddable( integerField, leaf );
			this.nested = nested;
		}
	}

	@Embeddable
	public static class SimpleEmbeddable {
		private Integer integerField;
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
	@Struct( name = "double_nested")
	public static class DoubleNested {
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
	@Struct( name = "nested")
	public static class Nested {
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
	@Struct( name = "leaf")
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
