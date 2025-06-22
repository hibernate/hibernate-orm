/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.util.List;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import org.assertj.core.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayToString.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayToStringTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new EntityWithArrays( 1L, new String[]{} ) );
			em.persist( new EntityWithArrays( 2L, new String[]{ "abc", null, "def" } ) );
			em.persist( new EntityWithArrays( 3L, null ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-to-string-example[]
			List<String> results = em.createQuery( "select array_to_string(e.theArray, ',') from EntityWithArrays e order by e.id", String.class )
					.getResultList();
			//end::hql-array-to-string-example[]
			assertEquals( 3, results.size() );
			// We expect an empty string, but Oracle returns NULL instead of empty strings
			Assertions.assertThat( results.get( 0 ) ).isNullOrEmpty();
			assertEquals( "abc,def", results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

	@Test
	public void testNullValue(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String> results = em.createQuery( "select array_to_string(e.theArray, ',', 'null') from EntityWithArrays e order by e.id", String.class )
					.getResultList();
			assertEquals( 3, results.size() );
			Assertions.assertThat( results.get( 0 ) ).isNullOrEmpty();
			assertEquals( "abc,null,def", results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

	@Test
	public void testNodeBuilderArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<EntityWithArrays> root = cq.from( EntityWithArrays.class );
			cq.multiselect(
					root.get( "id" ),
					cb.arrayToString( root.get( "theArray" ), cb.literal( "," ) ),
					cb.arrayToString( root.get( "theArray" ), "," )
			);
			em.createQuery( cq ).getResultList();
		} );
	}

	@Test
	public void testNodeBuilderCollection(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<EntityWithArrays> root = cq.from( EntityWithArrays.class );
			cq.multiselect(
					root.get( "id" ),
					cb.collectionToString( root.get( "theCollection" ), cb.literal( "," ) ),
					cb.collectionToString( root.get( "theCollection" ), "," )
			);
			em.createQuery( cq ).getResultList();
		} );
	}

	@Test
	public void testCast(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-to-string-hql-example[]
			List<String> results = em.createQuery( "select cast(e.theArray as String) from EntityWithArrays e order by e.id", String.class )
					.getResultList();
			//end::hql-array-to-string-hql-example[]
			assertEquals( 3, results.size() );
			assertEquals( "[]", results.get( 0 ) );
			assertEquals( "[abc,null,def]", results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

	@Test
	@SkipForDialect( dialectClass = HSQLDialect.class, majorVersion = 2, minorVersion = 7, microVersion = 2,
			reason = "Needs at least 2.7.3 due to the change in HSQLArrayToStringFunction that introduced a cast")
	public void testStr(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String> results = em.createQuery( "select str(e.theArray) from EntityWithArrays e order by e.id", String.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( "[]", results.get( 0 ) );
			assertEquals( "[abc,null,def]", results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

}
