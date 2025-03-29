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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayReplace.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayReplaceTest {

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
		scope.inTransaction( em -> {
			em.createMutationQuery( "delete from EntityWithArrays" ).executeUpdate();
		} );
	}

	@Test
	public void testReplace(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-replace-example[]
			List<Tuple> results = em.createQuery( "select e.id, array_replace(e.theArray, 'abc', 'xyz') from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			//end::hql-array-replace-example[]
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] {}, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[] { "xyz", null, "def" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testReplaceNullElement(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery( "select e.id, array_replace(e.theArray, null, 'aaa') from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] {}, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[] { "abc", "aaa", "def" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testReplaceNonExisting(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery( "select e.id, array_replace(e.theArray, 'xyz', 'aaa') from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] {}, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[] { "abc", null, "def" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
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
					cb.arrayReplace( root.<String[]>get( "theArray" ), cb.literal( "abc" ), cb.literal( "xyz" ) ),
					cb.arrayReplace( root.<String[]>get( "theArray" ), cb.literal( "abc" ), "xyz" ),
					cb.arrayReplace( root.<String[]>get( "theArray" ), "abc", cb.literal( "xyz" ) ),
					cb.arrayReplace( root.get( "theArray" ), "abc", "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayReplace( root.<Integer[]>get( "theArray" ), cb.literal( "abc" ), cb.literal( "xyz" ) );
//			cb.arrayReplace( root.<Integer[]>get( "theArray" ), cb.literal( "abc" ), "xyz" );
//			cb.arrayReplace( root.<Integer[]>get( "theArray" ), "abc", cb.literal( "xyz" ) );
//			cb.arrayReplace( root.<Integer[]>get( "theArray" ), "abc", "xyz" );
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
					cb.collectionReplace( root.<Collection<String>>get( "theCollection" ), cb.literal( "abc" ), cb.literal( "xyz" ) ),
					cb.collectionReplace( root.<Collection<String>>get( "theCollection" ), cb.literal( "abc" ), "xyz" ),
					cb.collectionReplace( root.<Collection<String>>get( "theCollection" ), "abc", cb.literal( "xyz" ) ),
					cb.collectionReplace( root.get( "theCollection" ), "abc", "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionReplace( root.<Collection<Integer>>get( "theCollection" ), cb.literal( "abc" ), cb.literal( "xyz" ) );
//			cb.collectionReplace( root.<Collection<Integer>>get( "theCollection" ), cb.literal( "abc" ), "xyz" );
//			cb.collectionReplace( root.<Collection<Integer>>get( "theCollection" ), "abc", cb.literal( "xyz" ) );
//			cb.collectionReplace( root.<Collection<Integer>>get( "theCollection" ), "abc", "xyz" );
		} );
	}

}
