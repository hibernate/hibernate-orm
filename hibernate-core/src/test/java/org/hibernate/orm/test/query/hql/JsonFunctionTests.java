/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel( annotatedClasses = JsonFunctionTests.JsonHolder.class)
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
					em.persist(entity);
				}
		);
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
					// HSQLDB bug
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

	private static double[] parseDoubleArray( String s ) {
		final List<Double> list = new ArrayList<>();
		int startIndex = 1;
		int commaIndex;
		while ( (commaIndex = s.indexOf(',', startIndex)) != -1 ) {
			list.add( Double.parseDouble( s.substring( startIndex, commaIndex ) ) );
			startIndex = commaIndex + 1;
		}
		list.add( Double.parseDouble( s.substring( startIndex, s.length() - 1 ) ) );
		double[] array = new double[list.size()];
		for ( int i = 0; i < list.size(); i++ ) {
			array[i] = list.get( i );
		}
		return array;
	}

	@Entity(name = "JsonHolder")
	public static class JsonHolder {
		@Id
		Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Map<String, Object> json;
	}
}
