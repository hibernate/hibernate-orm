/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel( annotatedClasses = {
		JsonFunctionTests.JsonHolder.class,
		EntityOfBasics.class
})
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18496")
public class JsonFunctionTests {

	JsonHolder entity;

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					entity = new JsonHolder();
					entity.id = 1L;
					entity.json = new HashMap<>();
					entity.json.put( "theInt", 1 );
					entity.json.put( "theFloat", 0.1 );
					entity.json.put( "theString", "abc" );
					entity.json.put( "theBoolean", true );
					entity.json.put( "theNull", null );
					entity.json.put( "theArray", new String[] { "a", "b", "c" } );
					entity.json.put( "theObject", new HashMap<>( entity.json ) );
					entity.json.put(
							"theNestedObjects",
							List.of(
									Map.of( "id", 1, "name", "val1" ),
									Map.of( "id", 2, "name", "val2" ),
									Map.of( "id", 3, "name", "val3" )
							)
					);
					em.persist(entity);

					EntityOfBasics e1 = new EntityOfBasics();
					e1.setId( 1 );
					e1.setTheString( "Dog" );
					e1.setTheInteger( 0 );
					e1.setTheUuid( UUID.randomUUID() );
					EntityOfBasics e2 = new EntityOfBasics();
					e2.setId( 2 );
					e2.setTheString( "Cat" );
					e2.setTheInteger( 0 );

					em.persist( e1 );
					em.persist( e2 );
				}
		);
	}

	@AfterAll
	public void cleanupData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	public void testJsonValue(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
									"select " +
											"json_value(e.json, '$.theInt'), " +
											"json_value(e.json, '$.theFloat'), " +
											"json_value(e.json, '$.theString'), " +
											"json_value(e.json, '$.theBoolean'), " +
											"json_value(e.json, '$.theNull'), " +
											"json_value(e.json, '$.theArray'), " +
											"json_value(e.json, '$.theArray[1]'), " +
											"json_value(e.json, '$.theObject'), " +
											"json_value(e.json, '$.theObject.theInt'), " +
											"json_value(e.json, '$.theObject.theArray[2]') " +
											"from JsonHolder e " +
											"where e.id = 1L",
									Tuple.class
							).getSingleResult();
					assertEquals( entity.json.get( "theInt" ).toString(), tuple.get( 0 ) );
					assertEquals( entity.json.get( "theFloat" ), Double.parseDouble( tuple.get( 1, String.class ) ) );
					assertEquals( entity.json.get( "theString" ), tuple.get( 2 ) );
					assertEquals( entity.json.get( "theBoolean" ).toString(), tuple.get( 3 ) );
					assertNull( tuple.get( 4 ) );
					// PostgreSQL emulation returns non-null value
//					assertNull( tuple.get( 5 ) );
					assertEquals( ( (String[]) entity.json.get( "theArray" ) )[1], tuple.get( 6 ) );
					// PostgreSQL emulation returns non-null value
//					assertNull( tuple.get( 7 ) );
					assertEquals( entity.json.get( "theInt" ).toString(), tuple.get( 8 ) );
					assertEquals( ( (String[]) entity.json.get( "theArray" ) )[2], tuple.get( 9 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	public void testJsonValueExpression(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select json_value('{\"theArray\":[1,10]}', '$.theArray[$idx]' passing :idx as idx) ",
							Tuple.class
					).setParameter( "idx", 0 ).getSingleResult();
					assertEquals( "1", tuple.get( 0 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonValue.class)
	public void testJsonValueBoolean(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select json_value(e.json, '$.theBoolean' returning boolean) from JsonHolder e where json_value(e.json, '$.theBoolean' returning boolean) = true",
							Tuple.class
					).getSingleResult();
					assertEquals( true, tuple.get( 0 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonQuery.class)
	public void testJsonQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select " +
									"json_query(e.json, '$.theArray'), " +
									"json_query(e.json, '$.theNestedObjects'), " +
									"json_query(e.json, '$.theNestedObjects[$idx]' passing :idx as idx with wrapper) " +
									"from JsonHolder e " +
									"where e.id = 1L",
							Tuple.class
					).setParameter( "idx", 0 ).getSingleResult();
					assertEquals( parseJson( "[\"a\",\"b\",\"c\"]" ), parseJson( tuple.get( 0, String.class ) ) );
					assertEquals(
							parseJson(
									"[{\"id\":1,\"name\":\"val1\"},{\"id\":2,\"name\":\"val2\"},{\"id\":3,\"name\":\"val3\"}]" ),
							parseJson( tuple.get( 1, String.class ) )
					);
					assertEquals( parseJson( "[{\"id\":1,\"name\":\"val1\"}]" ), parseJson( tuple.get( 2, String.class ) ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonQueryNestedPath.class)
	public void testJsonQueryNested(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select " +
									"json_query(e.json, '$.theNestedObjects[*].id' with wrapper) " +
									"from JsonHolder e " +
									"where e.id = 1L",
							Tuple.class
					).getSingleResult();
					assertEquals( parseJson( "[1,2,3]" ), parseJson( tuple.get( 0, String.class ) ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArray.class)
	public void testJsonArray(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select " +
									"json_array(), " +
									"json_array(1, 2, 3), " +
									"json_array(0.1, 0.2, 0.3), " +
									"json_array('a', 'b', 'c'), " +
									"json_array(true, false, true), " +
									"json_array(null, null null on null), " +
									"json_array(json_array(), json_array(1), json_array('a'), null null on null)",
							Tuple.class
					).getSingleResult();
					assertEquals( "[]", tuple.get( 0 ) );
					assertArrayEquals( new Integer[] { 1, 2, 3 }, parseArray( tuple.get( 1 ).toString() ) );
					assertArrayEquals( new Double[] { 0.1, 0.2, 0.3 }, parseArray( tuple.get( 2 ).toString() ) );
					assertArrayEquals( new String[] { "a", "b", "c" }, parseArray( tuple.get( 3 ).toString() ) );
					assertArrayEquals( new Boolean[] { true, false, true }, parseArray( tuple.get( 4 ).toString() ) );
					assertArrayEquals( new Object[] { null, null }, parseArray( tuple.get( 5 ).toString() ) );
					assertArrayEquals(
							new Object[] { List.of(), List.of( 1 ), List.of( "a" ), null },
							parseArray( tuple.get( 6 ).toString() )
					);
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	public void testJsonObject(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_object(" +
									"'theInt', 1, " +
									"'theFloat', 0.1, " +
									"'theString', 'abc', " +
									"'theBoolean', true, " +
									"'theNull', null, " +
									"'theObject', json_object(" +
									"    'theInt', 1, " +
									"    'theFloat', 0.1, " +
									"    'theString', 'abc', " +
									"    'theBoolean', true, " +
									"    'theNull', null " +
									"    absent on null" +
									")) ",
							String.class
					).getSingleResult();
					Map<String, Object> map = parseObject( json );
					assertEquals( entity.json.get( "theInt" ).toString(), map.get( "theInt" ).toString() );
					assertEquals( entity.json.get( "theFloat" ), Double.parseDouble( map.get( "theFloat" ).toString() ) );
					assertEquals( entity.json.get( "theString" ), map.get( "theString" ) );
					assertEquals( entity.json.get( "theBoolean" ), map.get( "theBoolean" ) );
					assertTrue( map.containsKey( "theNull" ) );
					assertNull( map.get( "theNull" ) );
					Map<String, Object> nested = (Map<String, Object>) map.get( "theObject" );
					assertEquals( entity.json.get( "theInt" ).toString(), nested.get( "theInt" ).toString() );
					assertEquals( entity.json.get( "theFloat" ), Double.parseDouble( nested.get( "theFloat" ).toString() ) );
					assertEquals( entity.json.get( "theString" ), nested.get( "theString" ) );
					assertEquals( entity.json.get( "theBoolean" ), nested.get( "theBoolean" ) );
					// HSQLDB bug: https://sourceforge.net/p/hsqldb/bugs/1720/
					if ( !( DialectContext.getDialect() instanceof HSQLDialect ) ) {
						assertFalse( nested.containsKey( "theNull" ) );
					}
					assertNull( nested.get( "theNull" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObject.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArray.class)
	public void testJsonObjectAndArray(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select json_object(" +
									"'a', json_array( 1, 2, 3 ), " +
									"'b', json_object(" +
									"    'c', json_array( 4, 5, 6 ) " +
									")), " +
									"json_array(json_object('a', 1), json_object('b', 'c'), json_object('c', null))",
							Tuple.class
					).getSingleResult();
					Map<String, Object> map = parseObject( tuple.get( 0 ).toString() );
					assertEquals( List.of( 1,2,3 ), map.get( "a" ) );
					assertInstanceOf( Map.class, map.get( "b" ) );
					Map<String, Object> nested = (Map<String, Object>) map.get( "b" );
					assertEquals( List.of( 4, 5, 6 ), nested.get( "c" ) );

					Object[] array = parseArray( tuple.get( 1 ).toString() );
					assertEquals( 3, array.length );
					assertInstanceOf( Map.class, array[0] );
					assertInstanceOf( Map.class, array[1] );
					assertInstanceOf( Map.class, array[2] );
					assertEquals( 1, ( (Map<String, Object>) array[0] ).get( "a" ) );
					assertEquals( "c", ( (Map<String, Object>) array[1] ).get( "b" ) );
					Map<String, Object> nested2 = (Map<String, Object>) array[2];
					assertTrue( nested2.containsKey( "c" ) );
					assertNull( nested2.get( "c" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonExists.class)
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 23, versionMatchMode = VersionMatchMode.OLDER, reason = "Oracle bug in versions before 23")
	public void testJsonExists(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select " +
									"json_exists(e.json, '$.theUnknown'), " +
									"json_exists(e.json, '$.theInt'), " +
									"json_exists(e.json, '$.theArray[0]'), " +
									"json_exists(e.json, '$.theArray[$idx]' passing :idx as idx) " +
									"from JsonHolder e " +
									"where e.id = 1L",
							Tuple.class
					).setParameter( "idx", 3 ).getSingleResult();
					assertEquals( false, tuple.get( 0 ) );
					assertEquals( true, tuple.get( 1 ) );
					assertEquals( true, tuple.get( 2 ) );
					assertEquals( false, tuple.get( 3 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayAgg.class)
	public void testJsonArrayAgg(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String jsonArray = session.createQuery(
							"select json_arrayagg(e.theString) " +
									"from EntityOfBasics e",
							String.class
					).getSingleResult();
					Object[] array = parseArray( jsonArray );
					assertEquals( 2, array.length );
					assertTrue( Arrays.asList( array ).contains( "Cat" ) );
					assertTrue( Arrays.asList( array ).contains( "Dog" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayAgg.class)
	public void testJsonArrayAggOrderBy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String jsonArray = session.createQuery(
							"select json_arrayagg(e.theString order by e.theString)" +
									"from EntityOfBasics e",
							String.class
					).getSingleResult();
					Object[] array = parseArray( jsonArray );
					assertArrayEquals( new Object[]{ "Cat", "Dog" }, array );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObjectAgg.class)
	public void testJsonObjectAgg(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String jsonArray = session.createQuery(
							"select json_objectagg(e.theString value e.id) " +
									"from EntityOfBasics e",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( jsonArray );
					assertEquals( 2, object.size() );
					assertEquals( 1, object.get( "Dog" ) );
					assertEquals( 2, object.get( "Cat" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObjectAgg.class)
	public void testJsonObjectAggNullFilter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String jsonArray = session.createQuery(
							"select json_objectagg(e.theString value e.theUuid) " +
									"from EntityOfBasics e",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( jsonArray );
					assertEquals( 1, object.size() );
					assertTrue( object.containsKey( "Dog" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObjectAgg.class)
	public void testJsonObjectAggNullClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String jsonArray = session.createQuery(
							"select json_objectagg(e.theString value e.theUuid null on null) " +
									"from EntityOfBasics e",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( jsonArray );
					assertEquals( 2, object.size() );
					assertNotNull( object.get( "Dog" ) );
					assertNull( object.get( "Cat" ) );
					assertTrue( object.containsKey( "Cat" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonObjectAgg.class)
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "MySQL has no way to throw an error on duplicate json object keys. The last one always wins.")
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "SQL Server has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = DB2Dialect.class, reason = "DB2 has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB has no way to throw an error on duplicate json object keys.")
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, majorVersion = 15, versionMatchMode = VersionMatchMode.SAME_OR_OLDER, reason = "Before version 16, PostgreSQL didn't support the unique keys clause.")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, majorVersion = 15, versionMatchMode = VersionMatchMode.SAME_OR_OLDER, reason = "Before version 16, PostgresPlus didn't support the unique keys clause.")
	public void testJsonObjectAggUniqueKeys(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					try {
						session.createQuery(
								"select json_objectagg(str(e.theInteger) value e.theString with unique keys) " +
										"from EntityOfBasics e",
								String.class
						).getSingleResult();
						fail("Should fail because keys are not unique");
					}
					catch (HibernateException e) {
						assertInstanceOf( JDBCException.class, e );
					}
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonSet.class)
	public void testJsonSet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_set('{}', '$.a', 123)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( 123, object.get( "a" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonSet.class)
	public void testJsonSetReplace(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_set('{\"a\":456}', '$.a', 123)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( 123, object.get( "a" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonRemove.class)
	public void testJsonRemove(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_remove('{\"a\":123,\"b\":456}', '$.a')",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( 456, object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonRemove.class)
	public void testJsonRemoveToEmpty(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_remove('{\"a\":123}', '$.a')",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 0, object.size() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonRemove.class)
	public void testJsonRemoveNonExisting(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_remove('{}', '$.a')",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 0, object.size() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonReplace.class)
	public void testJsonReplaceNonExisting(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_replace('{}', '$.a', 123)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 0, object.size() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonReplace.class)
	public void testJsonReplace(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_replace('{\"a\":456}', '$.a', 123)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( 123, object.get( "a" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonInsert.class)
	public void testJsonInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_insert('{}', '$.a', 123)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( 123, object.get( "a" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonInsert.class)
	public void testJsonInsertWithExisting(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_insert('{\"a\":456}', '$.a', 123)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( 456, object.get( "a" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonMergepatch.class)
	public void testJsonMergepatch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_mergepatch('{\"a\":456, \"b\":[1,2], \"c\":{\"a\":1}}', '{\"a\":null, \"b\":[4,5], \"c\":{\"b\":1}}')",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 2, object.size() );
					assertEquals( Arrays.asList( parseArray( "[4,5]" ) ), object.get( "b" ) );
					assertEquals( parseObject( "{\"a\":1,\"b\":1}" ), object.get( "c" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonMergepatch.class)
	public void testJsonMergepatchVarargs(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_mergepatch('{\"a\":456, \"b\":[1,2], \"c\":{\"a\":1}}', '{\"a\":null, \"b\":[4,5], \"c\":{\"b\":1}}', '{\"d\":1}')",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 3, object.size() );
					assertEquals( Arrays.asList( parseArray( "[4,5]" ) ), object.get( "b" ) );
					assertEquals( parseObject( "{\"a\":1,\"b\":1}" ), object.get( "c" ) );
					assertEquals( 1, object.get( "d" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayAppend.class)
	public void testJsonArrayAppend(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_append('{\"b\":[2]}', '$.b', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( Arrays.asList( 2, 1 ), object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayAppend.class)
	public void testJsonArrayAppendNonExisting(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_append('{\"b\":[2]}', '$.c', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( List.of( 2 ), object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayAppend.class)
	public void testJsonArrayAppendNonArray(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_append('{\"b\":2}', '$.b', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( Arrays.asList( 2, 1 ), object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayAppend.class)
	public void testJsonArrayAppendToNull(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_append('{\"b\":null}', '$.b', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( Arrays.asList( null, 1 ), object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayInsert.class)
	public void testJsonArrayInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_insert('{\"b\":[2]}', '$.b[0]', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( Arrays.asList( 1, 2 ), object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayInsert.class)
	public void testJsonArrayInsertNonExisting(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_insert('{\"b\":[2]}', '$.c[0]', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( List.of( 2 ), object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayInsert.class)
	public void testJsonArrayInsertNonArray(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_insert('{\"b\":2}', '$.b[0]', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertEquals( 2, object.get( "b" ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonArrayInsert.class)
	public void testJsonArrayInsertToNull(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String json = session.createQuery(
							"select json_array_insert('{\"b\":null}', '$.b[0]', 1)",
							String.class
					).getSingleResult();
					Map<String, Object> object = parseObject( json );
					assertEquals( 1, object.size() );
					assertNull( object.get( "b" ) );
				}
		);
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static Map<String, Object> parseObject(String json) {
		try {
			//noinspection unchecked
			return MAPPER.readValue( json, Map.class );
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}
	}

	private static Object[] parseArray(String json) {
		try {
			return MAPPER.readValue( json, Object[].class );
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}
	}

	private static Object parseJson(String json) {
		try {
			return toJavaNode( MAPPER.readTree( json ) );
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException( e );
		}
	}

	private static Object toJavaNode(JsonNode jsonNode) {
		if ( jsonNode instanceof ArrayNode arrayNode ) {
			final var list = new ArrayList<>( arrayNode.size() );
			for ( JsonNode node : arrayNode ) {
				list.add( toJavaNode( node ) );
			}
			return list;
		}
		else if ( jsonNode instanceof ObjectNode object ) {
			final var map = new HashMap<>( object.size() );
			final Iterator<Map.Entry<String, JsonNode>> iter = object.fields();
			while ( iter.hasNext() ) {
				final Map.Entry<String, JsonNode> entry = iter.next();
				map.put( entry.getKey(), toJavaNode( entry.getValue() ) );
			}
			return map;
		}
		else if ( jsonNode instanceof NullNode ) {
			return null;
		}
		else if ( jsonNode instanceof NumericNode numericNode ) {
			return numericNode.numberValue();
		}
		else if ( jsonNode instanceof BooleanNode booleanNode ) {
			return booleanNode.booleanValue();
		}
		else if ( jsonNode instanceof TextNode textNode ) {
			return textNode.textValue();
		}
		else if ( jsonNode instanceof BinaryNode binaryNode ) {
			return binaryNode.binaryValue();
		}
		else {
			throw new UnsupportedOperationException( "Unsupported node type: " +  jsonNode.getClass().getName() );
		}
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {
		@Id
		Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Map<String, Object> json;
	}
}
