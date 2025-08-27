/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Tuple;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaJsonTableColumnsNode;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.SqmJsonTableFunction;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithJson.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.JSON_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonTable.class)
public class JsonTableTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			EntityWithJson entity = new EntityWithJson();
			entity.setId( 1L );
			entity.getJson().put( "theInt", 1 );
			entity.getJson().put( "theFloat", 0.1 );
			entity.getJson().put( "theString", "abc" );
			entity.getJson().put( "theBoolean", true );
			entity.getJson().put( "theNull", null );
			entity.getJson().put( "theArray", new String[] { "a", "b", "c" } );
			entity.getJson().put( "theObject", new HashMap<>( entity.getJson() ) );
			em.persist(entity);
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-json-table-example[]
			final String query = """
					select
					t.theInt,
					t.theFloat,
					t.theString,
					t.theBoolean,
					t.theNull,
					t.theObject,
					t.theNestedInt,
					t.theNestedFloat,
					t.theNestedString,
					t.arrayIndex,
					t.arrayValue,
					t.nonExisting
					from EntityWithJson e
					join lateral json_table(e.json,'$' columns(
					theInt Integer,
					theFloat Float,
					theString String,
					theBoolean Boolean,
					theNull String,
					theObject JSON,
					theNestedInt Integer path '$.theObject.theInt',
					theNestedFloat Float path '$.theObject.theFloat',
					theNestedString String path '$.theObject.theString',
					nested '$.theArray[*]' columns(
						arrayIndex for ordinality,
						arrayValue String path '$'
					),
					nonExisting exists
					)) t
					order by e.id, t.arrayIndex
					""";
			List<Tuple> resultList = em.createQuery( query, Tuple.class ).getResultList();
			//end::hql-json-table-example[]

			assertEquals( 3, resultList.size() );

			assertTupleEquals( resultList.get( 0 ), 1L, "a" );
			assertTupleEquals( resultList.get( 1 ), 2L, "b" );
			assertTupleEquals( resultList.get( 2 ), 3L, "c" );
		} );
	}

	@Test
	public void testNodeBuilderJsonTableObject(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final SqmSelectStatement<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<EntityWithJson> root = cq.from( EntityWithJson.class );
			final SqmJsonTableFunction<?> jsonTable = cb.jsonTable( root.get( "json" ), cb.literal( "$" ) );

			jsonTable.valueColumn( "theInt", Integer.class );
			jsonTable.valueColumn( "theFloat", Float.class );
			jsonTable.valueColumn( "theString", String.class );
			jsonTable.valueColumn( "theBoolean", Boolean.class );
			jsonTable.valueColumn( "theNull", String.class );
			jsonTable.queryColumn( "theObject" );
			jsonTable.valueColumn( "theNestedInt", Integer.class, "$.theObject.theInt" );
			jsonTable.valueColumn( "theNestedFloat", Float.class, "$.theObject.theFloat" );
			jsonTable.valueColumn( "theNestedString", String.class, "$.theObject.theString" );
			final JpaJsonTableColumnsNode theArray = jsonTable.nested( "$.theArray[*]" );
			theArray.ordinalityColumn( "arrayIndex" );
			theArray.valueColumn( "arrayValue", String.class, "$" );
			jsonTable.existsColumn( "nonExisting" );

			final JpaFunctionJoin<?> join = root.joinLateral( jsonTable );
			cq.multiselect(
					join.get( "theInt" ),
					join.get( "theFloat" ),
					join.get( "theString" ),
					join.get( "theBoolean" ),
					join.get( "theNull" ),
					join.get( "theObject" ),
					join.get( "theNestedInt" ),
					join.get( "theNestedFloat" ),
					join.get( "theNestedString" ),
					join.get( "arrayIndex" ),
					join.get( "arrayValue" ),
					join.get( "nonExisting" )
			);
			cq.orderBy( cb.asc( root.get( "id" ) ), cb.asc( join.get( "arrayIndex" ) ) );
			List<Tuple> resultList = em.createQuery( cq ).getResultList();

			assertEquals( 3, resultList.size() );

			assertTupleEquals( resultList.get( 0 ), 1L, "a" );
			assertTupleEquals( resultList.get( 1 ), 2L, "b" );
			assertTupleEquals( resultList.get( 2 ), 3L, "c" );
		} );
	}

	@Test
	public void testArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final String query = """
					select
					t.idx,
					t.val
					from json_table('[1,2]','$[*]' columns(val Integer path '$', idx for ordinality)) t
					order by t.idx
					""";
			List<Tuple> resultList = em.createQuery( query, Tuple.class ).getResultList();

			assertEquals( 2, resultList.size() );

			assertEquals( 1L, resultList.get( 0 ).get( 0 ) );
			assertEquals( 1, resultList.get( 0 ).get( 1 ) );
			assertEquals( 2L, resultList.get( 1 ).get( 0 ) );
			assertEquals( 2, resultList.get( 1 ).get( 1 ) );
		} );
	}

	@Test
	public void testArrayParam(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final String query = """
					select
					t.idx,
					t.val
					from json_table(:arr,'$[*]' columns(val Integer path '$', idx for ordinality)) t
					order by t.idx
					""";
			List<Tuple> resultList = em.createQuery( query, Tuple.class )
					.setParameter( "arr", "[1,2]" )
					.getResultList();

			assertEquals( 2, resultList.size() );

			assertEquals( 1L, resultList.get( 0 ).get( 0 ) );
			assertEquals( 1, resultList.get( 0 ).get( 1 ) );
			assertEquals( 2L, resultList.get( 1 ).get( 0 ) );
			assertEquals( 2, resultList.get( 1 ).get( 1 ) );
		} );
	}

	private static void assertTupleEquals(Tuple tuple, long arrayIndex, String arrayValue) {
		assertEquals( 1, tuple.get( 0 ) );
		assertEquals( 0.1F, tuple.get( 1 ) );
		assertEquals( "abc", tuple.get( 2 ) );
		assertEquals( true, tuple.get( 3 ) );
		assertNull( tuple.get( 4 ) );

		Map<String, Object> jsonMap = parseObject( tuple.get( 5, String.class ) );
		assertEquals( 1, jsonMap.get( "theInt" ) );
		assertEquals( 0.1D, jsonMap.get( "theFloat" ) );
		assertEquals( "abc", jsonMap.get( "theString" ) );
		assertEquals( true, jsonMap.get( "theBoolean" ) );
		assertNull( jsonMap.get( "theNull" ) );
		assertEquals( Arrays.asList( "a", "b", "c" ), jsonMap.get( "theArray" ) );

		assertEquals( 1, tuple.get( 6 ) );
		assertEquals( 0.1F, tuple.get( 7 ) );
		assertEquals( "abc", tuple.get( 8 ) );
		assertEquals( arrayIndex, tuple.get( 9 ) );
		assertEquals( arrayValue, tuple.get( 10 ) );
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

}
