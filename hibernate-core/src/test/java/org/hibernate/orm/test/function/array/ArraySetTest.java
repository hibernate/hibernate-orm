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
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArraySet.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArraySetTest {

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
	public void testSet(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-set-example[]
			List<Tuple> results = em.createQuery( "select e.id, array_set(e.theArray, 1, 'xyz') from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			//end::hql-array-set-example[]
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] { "xyz" }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[] { "xyz", null, "def" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertArrayEquals( new String[] { "xyz" }, results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testSetNullElement(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery( "select e.id, array_set(e.theArray, 1, null) from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] { null }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[] { null, null, "def" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertArrayEquals( new String[] { null }, results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testSetFillNulls(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery( "select e.id, array_set(e.theArray, 3, 'aaa') from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] { null, null, "aaa" }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[] { "abc", null, "aaa" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertArrayEquals( new String[] { null, null, "aaa" }, results.get( 2 ).get( 1, String[].class ) );
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
					cb.arraySet( root.<String[]>get( "theArray" ), cb.literal( 1 ), cb.literal( "xyz" ) ),
					cb.arraySet( root.get( "theArray" ), cb.literal( 1 ), "xyz" ),
					cb.arraySet( root.<String[]>get( "theArray" ), 1, cb.literal( "xyz" ) ),
					cb.arraySet( root.get( "theArray" ), 1, "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arraySet( root.<Integer[]>get( "theArray" ), cb.literal( 1 ), cb.literal( "xyz" ) );
//			cb.arraySet( root.<Integer[]>get( "theArray" ), cb.literal( 1 ), "xyz" );
//			cb.arraySet( root.<Integer[]>get( "theArray" ), 1, cb.literal( "xyz" ) );
//			cb.arraySet( root.<Integer[]>get( "theArray" ), 1, "xyz" );
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
					cb.collectionSet( root.<Collection<String>>get( "theCollection" ), cb.literal( 1 ), cb.literal( "xyz" ) ),
					cb.collectionSet( root.get( "theCollection" ), cb.literal( 1 ), "xyz" ),
					cb.collectionSet( root.<Collection<String>>get( "theCollection" ), 1, cb.literal( "xyz" ) ),
					cb.collectionSet( root.get( "theCollection" ), 1, "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionSet( root.<Collection<Integer>>get( "theCollection" ), cb.literal( 1 ), cb.literal( "xyz" ) );
//			cb.collectionSet( root.<Collection<Integer>>get( "theCollection" ), cb.literal( 1 ), "xyz" );
//			cb.collectionSet( root.<Collection<Integer>>get( "theCollection" ), 1, cb.literal( "xyz" ) );
//			cb.collectionSet( root.<Collection<Integer>>get( "theCollection" ), 1, "xyz" );
		} );
	}

}
