/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.util.Collection;
import java.util.List;

import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Yoobin Yoon
 */
@JiraKey("HHH-19826")
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayReverse.class)
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayReverseTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new EntityWithArrays( 1L, new String[] {} ) );
			em.persist( new EntityWithArrays( 2L, new String[] { "abc", "def", null } ) );
			em.persist( new EntityWithArrays( 3L, null ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testReverse(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-reverse-example[]
			List<Tuple> results = em.createQuery(
							"select e.id, array_reverse(e.theArray) from EntityWithArrays e order by e.id",
							Tuple.class
					)
					.getResultList();
			//end::hql-array-reverse-example[]
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] {}, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[] { null, "def", "abc" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testReverseSingleElement(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new EntityWithArrays( 4L, new String[] { "single" } ) );
		} );
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery(
							"select e.id, array_reverse(e.theArray) from EntityWithArrays e where e.id = 4",
							Tuple.class
					)
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 4L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] { "single" }, results.get( 0 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testReverseDuplicates(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new EntityWithArrays( 5L, new String[] { "a", "b", "a", "c", "a" } ) );
		} );
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery(
							"select e.id, array_reverse(e.theArray) from EntityWithArrays e where e.id = 5",
							Tuple.class
					)
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 5L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] { "a", "c", "a", "b", "a" }, results.get( 0 ).get( 1, String[].class ) );
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
					cb.arrayReverse( root.<String[]>get( "theArray" ) ),
					cb.arrayReverse( root.get( "theArray" ) )
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
					cb.collectionReverse( root.<Collection<String>>get( "theCollection" ) ),
					cb.collectionReverse( root.get( "theCollection" ) )
			);
			em.createQuery( cq ).getResultList();
		} );
	}

}
