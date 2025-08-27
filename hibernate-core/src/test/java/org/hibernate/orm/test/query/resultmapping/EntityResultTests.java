/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.LockModeType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.named.NamedResultSetMappingMemento;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				SimpleEntityWithNamedMappings.class,
				DiscriminatedRoot.class,
				DiscriminatedSubType1.class,
				DiscriminatedSubType2.class,
				EntityOfBasics.class,
				EntityWithEmbedded.class
		},
		xmlMappings = "org/hibernate/orm/test/query/resultmapping/result-set-mapping.hbm.xml"
)
@ServiceRegistry(
		settings = {
				@Setting(
						name = AvailableSettings.JDBC_TIME_ZONE,
						value = "UTC"
				)
		}
)
public class EntityResultTests extends AbstractUsageTest {

	@Test
	public void testSimpleEntityResultMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedObjectRepository()
							.getResultSetMappingMemento( "entity" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );
					assertThat( entity.getNotes(), is( "notes" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity" );
				}
		);
	}

	@Test
	public void testImplicitAttributeMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedObjectRepository()
							.getResultSetMappingMemento(
									"entity-none" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity-none" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( "test" ) );
					assertThat( entity.getNotes(), is( "notes" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity-none" );
				}
		);
	}

	@Test
	@RequiresDialect(
			value = H2Dialect.class,
			comment = "We don't really care about the execution on the database, just how the result-set is handled.  Some databases (mssql) don't like this query"
	)
	public void testImplicitAttributeMappingWithLockMode(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedObjectRepository()
							.getResultSetMappingMemento(
									"entity-lockmode" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings for update";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity-lockmode" )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( session.getLockMode(results.get(0)), is(LockModeType.PESSIMISTIC_WRITE));
				}
		);
	}

	@Test
	public void testMixedAttributeMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// make sure it is in the repository
					final NamedResultSetMappingMemento mappingMemento = session.getSessionFactory()
							.getQueryEngine()
							.getNamedObjectRepository()
							.getResultSetMappingMemento(
									"entity-id-notes" );
					assertThat( mappingMemento, notNullValue() );

					// apply it to a native-query
					final String qryString = "select id, name, notes from SimpleEntityWithNamedMappings";
					final List<SimpleEntityWithNamedMappings> results = session
							.createNativeQuery( qryString, "entity-id-notes" )
							.list();
					assertThat( results.size(), is( 1 ) );

					final SimpleEntityWithNamedMappings entity = results.get( 0 );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getName(), is( nullValue() ) );
					assertThat( entity.getNotes(), is( "notes" ) );

					// todo (6.0) : should also try executing the ProcedureCall once that functionality is implemented
					session.createStoredProcedureCall( "abc", "entity-id-notes" );
				}
		);
	}

	@Test
	public void testExplicitDiscriminatedMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String qryString =
							"select id as id_alias,"
							+ "   type_code as type_code_alias,"
							+ "   root_name as root_name_alias,"
							+ "   subtype1_name as sub_type1_name_alias,"
							+ "   subtype2_name as sub_type2_name_alias"
							+ " from discriminated_entity";

					final List<DiscriminatedRoot> results = session.createNativeQuery( qryString, "root-explicit" ).list();
					assertThat( results.size(), is( 4 ) );

					final Set<Integer> idsFound = new HashSet<>();
					results.forEach( result -> idsFound.add( result.getId() ) );
					assertThat( idsFound, containsInAnyOrder( 1, 2, 3, 4 ) );
				}
		);
	}

	@Test
	public void testExplicitDiscriminatedMappingWithResultClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String qryString =
							"select id as id_alias,"
									+ "   type_code as type_code_alias,"
									+ "   root_name as root_name_alias,"
									+ "   subtype1_name as sub_type1_name_alias,"
									+ "   subtype2_name as sub_type2_name_alias"
									+ " from discriminated_entity";

					final List<DiscriminatedRoot> results = session.createNativeQuery( qryString, "root-explicit", DiscriminatedRoot.class ).list();
					assertThat( results.size(), is( 4 ) );

					final Set<Integer> idsFound = new HashSet<>();
					results.forEach( result -> idsFound.add( result.getId() ) );
					assertThat( idsFound, containsInAnyOrder( 1, 2, 3, 4 ) );
				}
		);
	}

	@Test
	public void testConvertedAttributes(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select e.* from EntityOfBasics e";
					final List<EntityOfBasics> results = (List<EntityOfBasics>) session
							.createNativeQuery( sql, "entity-of-basics-implicit" )
							.getResultList();
					assertThat( results.size(), is( 1 ) );

					final EntityOfBasics entityOfBasics = results.get( 0 );
					assertThat( entityOfBasics.getGender(), is( EntityOfBasics.Gender.MALE ) );
					assertThat( entityOfBasics.getOrdinalGender(), is( EntityOfBasics.Gender.FEMALE ) );
					assertThat( entityOfBasics.getConvertedGender(), is( EntityOfBasics.Gender.OTHER ) );

					assertThat( entityOfBasics.getTheInstant(), is( THEN ) );
					assertThat( entityOfBasics.getTheTimestamp(), is( THEN_TIMESTAMP ) );

					final ZonedDateTime zonedDateTime = ZonedDateTime.from( THEN.atZone( ZoneId.of( "UTC" ) ) );

					// Date#year is epoch-based starting from 1900.  So add the adjustment
					assertThat( entityOfBasics.getTheDate().getYear() + 1900, is( zonedDateTime.getYear() ) );
					// Date#month is zero-based.  Add the adjustment
					assertThat( entityOfBasics.getTheDate().getMonth() + 1, is( zonedDateTime.getMonthValue() ) );
//					assertThat( entityOfBasics.getTheDate().getDay(), is( zonedDateTime.getDayOfWeek().getValue() ) );

// can't get these correct
//  todo (6.0) - enable these assertions and fix problems
//					assertThat( entityOfBasics.getTheTime().getHours(), is( zonedDateTime.getHour() ) );
//					assertThat( entityOfBasics.getTheTime().getMinutes(), is( zonedDateTime.getMinute() ) );
//					assertThat( entityOfBasics.getTheTime().getSeconds(), is( zonedDateTime.getSecond() + 1 ) );
				}
		);
	}

	@Test
	public void testImplicitEmbeddedMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityWithEmbedded result = (EntityWithEmbedded) session
							.getNamedNativeQuery( EntityWithEmbedded.IMPLICIT )
							.getSingleResult();
					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getCompoundName(), notNullValue() );
					assertThat( result.getCompoundName().getFirstPart(), is( "hi" ) );
					assertThat( result.getCompoundName().getSecondPart(), is( "there" ) );
				}
		);
	}

	@Test
	public void testExplicitEmbeddedMapping(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityWithEmbedded result = (EntityWithEmbedded) session
							.getNamedNativeQuery( EntityWithEmbedded.EXPLICIT )
							.getSingleResult();
					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getCompoundName(), notNullValue() );
					assertThat( result.getCompoundName().getFirstPart(), is( "hi" ) );
					assertThat( result.getCompoundName().getSecondPart(), is( "there" ) );
				}
		);
	}

	@Test
	public void testHbmMappingScalarComplete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql =
							"select id as hbm_id,"
							+ " name_first_part as hbm_name_first,"
							+ " name_second_part as hbm_name_second"
							+ " from entity_with_embedded";
					final Object[] result = (Object[]) session
							.createNativeQuery( sql, "hbm-resultset-scalar-complete" )
							.getSingleResult();
					assertThat( result, notNullValue() );
					assertThat( result, instanceOf( Object[].class ) );
					final Object[] values = result;
					assertThat( values[ 0 ], is( 1 ) );;
					assertThat( values[ 1 ], is( "hi" ) );
					assertThat( values[ 2 ], is( "there" ) );
				}
		);
	}

	@Test
	public void testHbmMappingEntityComplete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql =
							"select id as hbm_id,"
							+ " name_first_part as hbm_name_first,"
							+ " name_second_part as hbm_name_second"
							+ " from entity_with_embedded";
					final EntityWithEmbedded result = (EntityWithEmbedded) session
							.createNativeQuery( sql, "hbm-resultset-entity-complete" )
							.getSingleResult();
					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getCompoundName(), notNullValue() );
					assertThat( result.getCompoundName().getFirstPart(), is( "hi" ) );
					assertThat( result.getCompoundName().getSecondPart(), is( "there" ) );
				}
		);
	}

	@Test
	public void testHbmMappingEntityPartial(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql =
							"select id,"
							+ " name_first_part as hbm_name_first,"
							+ " name_second_part as hbm_name_second"
							+ " from entity_with_embedded";
					final EntityWithEmbedded result = (EntityWithEmbedded) session
							.createNativeQuery( sql, "hbm-resultset-entity-partial" )
							.getSingleResult();
					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getCompoundName(), notNullValue() );
					assertThat( result.getCompoundName().getFirstPart(), is( "hi" ) );
					assertThat( result.getCompoundName().getSecondPart(), is( "there" ) );
				}
		);
	}

	@Test
	public void testImplicitHbmMappingEntityComplete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<?> results = session
							.getNamedNativeQuery( "hbm-implicit-resultset" )
							.list();

					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), instanceOf( EntityWithEmbedded.class ) );

					final EntityWithEmbedded result = (EntityWithEmbedded) results.get( 0 );
					assertThat( result, notNullValue() );

					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getCompoundName(), notNullValue() );
					assertThat( result.getCompoundName().getFirstPart(), is( "hi" ) );
					assertThat( result.getCompoundName().getSecondPart(), is( "there" ) );
				}
		);
	}

	private static final Instant THEN = ZonedDateTime.of( 2020, 1, 1, 12, 0, 0, 100_000_000, ZoneOffset.UTC ).toInstant();
	private static final Date THEN_TIMESTAMP = Timestamp.from( THEN );
	private static final Date THEN_DATE = java.sql.Date.from( THEN  );
	private static final Date THEN_TIME = java.sql.Time.from( THEN );

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// discriminator hierarchy data
					final DiscriminatedRoot root1 = new DiscriminatedRoot( 1, "root-1" );
					final DiscriminatedRoot root2 = new DiscriminatedRoot( 2, "root-2" );
					final DiscriminatedSubType1 subType1_1 = new DiscriminatedSubType1( 3, "root-3", "sub_type1-1" );
					final DiscriminatedSubType2 subType2_1 = new DiscriminatedSubType2( 4, "root-3", "sub_type2-1" );
					session.persist( root1 );
					session.persist( root2 );
					session.persist( subType1_1 );
					session.persist( subType2_1 );


					// converted values data
					final EntityOfBasics entityOfBasics = new EntityOfBasics( 1 );

					//  - enums
					entityOfBasics.setGender( EntityOfBasics.Gender.MALE );
					entityOfBasics.setOrdinalGender( EntityOfBasics.Gender.FEMALE );

					//  - JPA converter
					entityOfBasics.setConvertedGender( EntityOfBasics.Gender.OTHER );

					//  - temporals
					entityOfBasics.setTheDate( THEN_DATE );
					entityOfBasics.setTheTime( THEN_TIME );
					entityOfBasics.setTheTimestamp( THEN_TIMESTAMP );
					entityOfBasics.setTheInstant( THEN );

					session.persist( entityOfBasics );

					// embedded values
					final EntityWithEmbedded entityWithEmbedded = new EntityWithEmbedded(
							1,
							new EntityWithEmbedded.CompoundName(
									"hi",
									"there"
							)
					);
					session.persist( entityWithEmbedded );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
