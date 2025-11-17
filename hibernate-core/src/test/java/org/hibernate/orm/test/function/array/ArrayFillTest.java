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
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayFill.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayFillTest {

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
	public void testFill(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-fill-example[]
			List<String[]> results = em.createQuery( "select array_fill('aaa', 2)", String[].class )
					.getResultList();
			//end::hql-array-fill-example[]
			assertEquals( 1, results.size() );
			assertArrayEquals( new String[] { "aaa", "aaa" }, results.get( 0 ) );
		} );
	}

	@Test
	public void testFillEmpty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery( "select array_fill('aaa', 0)", String[].class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertArrayEquals( new String[0], results.get( 0 ) );
		} );
	}

	@Test
	public void testFillNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery( "select array_fill(cast(null as String), 1)", String[].class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertArrayEquals( new String[]{ null }, results.get( 0 ) );
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
					cb.arrayFill( cb.literal( "xyz" ), cb.literal( 2 ) ),
					cb.arrayFill( cb.literal( "xyz" ), 2 ),
					cb.arrayFill( "xyz", cb.literal( 2 ) ),
					cb.arrayFill( "xyz", 2 )
			);
			final List<Tuple> result = em.createQuery( cq ).getResultList();
			final String[] expected = new String[]{ "xyz", "xyz" };
			assertEquals( 3, result.size() );
			assertArrayEquals( expected, result.get( 0 ).get( 1, String[].class ) );
			assertArrayEquals( expected, result.get( 0 ).get( 2, String[].class ) );
			assertArrayEquals( expected, result.get( 0 ).get( 3, String[].class ) );
			assertArrayEquals( expected, result.get( 0 ).get( 4, String[].class ) );
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
					cb.collectionFill( cb.literal( "xyz" ), cb.literal( 2 ) ),
					cb.collectionFill( cb.literal( "xyz" ), 2 ),
					cb.collectionFill( "xyz", cb.literal( 2 ) ),
					cb.collectionFill( "xyz", 2 )
			);
			final List<Tuple> result = em.createQuery( cq ).getResultList();
			final List<String> expected = List.of( "xyz", "xyz" );
			assertEquals( 3, result.size() );
			assertEquals( expected, result.get( 0 ).get( 1, Collection.class ) );
			assertEquals( expected, result.get( 0 ).get( 2, Collection.class ) );
			assertEquals( expected, result.get( 0 ).get( 3, Collection.class ) );
			assertEquals( expected, result.get( 0 ).get( 4, Collection.class ) );
		} );
	}

}
