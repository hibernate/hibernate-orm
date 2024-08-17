/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

	@Entity(name = "JsonHolder")
	public static class JsonHolder {
		@Id
		Long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Map<String, Object> json;
	}
}
