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

import static org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsStructuralArrays;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Yoobin Yoon
 */
@JiraKey("HHH-19826")
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature(feature = SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArraySort.class)
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArraySortTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new EntityWithArrays( 1L, new String[] { "banana", "apple", "cherry" } ) );
			em.persist( new EntityWithArrays( 2L, null ) );
			em.persist( new EntityWithArrays( 3L, new String[] {} ) );
			em.persist( new EntityWithArrays( 4L, new String[] { "banana", null, "apple" } ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testArraySortAscending(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-sort-example[]
			List<String[]> results = em.createQuery(
					"select array_sort(e.theArray) from EntityWithArrays e order by e.id",
					String[].class
			).getResultList();
			//end::hql-array-sort-example[]
			assertEquals( 4, results.size() );
			assertArrayEquals( new String[] { "apple", "banana", "cherry" }, results.get( 0 ) );
			assertNull( results.get( 1 ) );
			assertArrayEquals( new String[] {}, results.get( 2 ) );
			assertArrayEquals( new String[] { "apple", "banana", null }, results.get( 3 ) );
		} );
	}

	@Test
	public void testArraySortAscendingExplicit(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery(
					"select array_sort(e.theArray, false) from EntityWithArrays e order by e.id",
					String[].class
			).getResultList();
			assertEquals( 4, results.size() );
			assertArrayEquals( new String[] { "apple", "banana", "cherry" }, results.get( 0 ) );
			assertNull( results.get( 1 ) );
			assertArrayEquals( new String[] {}, results.get( 2 ) );
			assertArrayEquals( new String[] { "apple", "banana", null }, results.get( 3 ) );
		} );
	}

	@Test
	public void testArraySortDescending(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery(
					"select array_sort(e.theArray, true) from EntityWithArrays e order by e.id",
					String[].class
			).getResultList();
			assertEquals( 4, results.size() );
			assertArrayEquals( new String[] { "cherry", "banana", "apple" }, results.get( 0 ) );
			assertNull( results.get( 1 ) );
			assertArrayEquals( new String[] {}, results.get( 2 ) );
			assertArrayEquals( new String[] { null, "banana", "apple" }, results.get( 3 ) );
		} );
	}

	@Test
	public void testArraySortDescendingNullsLast(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-sort-descending-nulls-last-example[]
			List<String[]> results = em.createQuery(
					"select array_sort(e.theArray, true, false) from EntityWithArrays e order by e.id",
					String[].class
			).getResultList();
			//end::hql-array-sort-descending-nulls-last-example[]
			assertEquals( 4, results.size() );
			assertArrayEquals( new String[] { "cherry", "banana", "apple" }, results.get( 0 ) );
			assertNull( results.get( 1 ) );
			assertArrayEquals( new String[] {}, results.get( 2 ) );
			assertArrayEquals( new String[] { "banana", "apple", null }, results.get( 3 ) );
		} );
	}

	@Test
	public void testArraySortWithParameters(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery(
							"select array_sort(e.theArray, :desc, :nullsFirst) from EntityWithArrays e order by e.id",
							String[].class
					)
					.setParameter( "desc", true )
					.setParameter( "nullsFirst", false )
					.getResultList();

			assertEquals( 4, results.size() );
			assertArrayEquals( new String[] { "cherry", "banana", "apple" }, results.get( 0 ) );
			assertNull( results.get( 1 ) );
			assertArrayEquals( new String[] {}, results.get( 2 ) );
			assertArrayEquals( new String[] { "banana", "apple", null }, results.get( 3 ) );
		} );
	}

	@Test
	public void testArraySortAscendingNullsFirst(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery(
					"select array_sort(e.theArray, false, true) from EntityWithArrays e order by e.id",
					String[].class
			).getResultList();
			assertEquals( 4, results.size() );
			assertArrayEquals( new String[] { "apple", "banana", "cherry" }, results.get( 0 ) );
			assertNull( results.get( 1 ) );
			assertArrayEquals( new String[] {}, results.get( 2 ) );
			assertArrayEquals( new String[] { null, "apple", "banana" }, results.get( 3 ) );
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
					cb.arraySort( root.<String[]>get( "theArray" ) ),
					cb.arraySort( root.get( "theArray" ) ),
					cb.arraySort( root.<String[]>get( "theArray" ), true ),
					cb.arraySort( root.get( "theArray" ), cb.literal( false ) ),
					cb.arraySort( root.<String[]>get( "theArray" ), true, false ),
					cb.arraySort( root.get( "theArray" ), cb.literal( true ), cb.literal( false ) )
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
					cb.collectionSort( root.<Collection<String>>get( "theCollection" ) ),
					cb.collectionSort( root.get( "theCollection" ) ),
					cb.collectionSort( root.<Collection<String>>get( "theCollection" ), true ),
					cb.collectionSort( root.get( "theCollection" ), cb.literal( false ) ),
					cb.collectionSort( root.<Collection<String>>get( "theCollection" ), true, false ),
					cb.collectionSort( root.get( "theCollection" ), cb.literal( true ), cb.literal( false ) )
			);
			em.createQuery( cq ).getResultList();

		} );
	}
}
