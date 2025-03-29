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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayConstructor.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayConstructorTest {

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
	public void testEmpty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where e.theArray = array()", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 1L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testNonExisting(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where e.theArray = array('abc')", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-example[]
			assertEquals( 0, results.size() );
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
					cb.arrayLiteral( "xyz" )
			);
			final List<Tuple> result = em.createQuery( cq ).getResultList();
			assertEquals( 3, result.size() );
			assertEquals( 1, result.get( 0 ).get( 1, String[].class ).length );
			assertEquals( "xyz", result.get( 0 ).get( 1, String[].class )[0] );
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
					cb.collectionLiteral( "xyz" )
			);
			final List<Tuple> result = em.createQuery( cq ).getResultList();
			assertEquals( 3, result.size() );
			assertEquals( 1, result.get( 0 ).get( 1, Collection.class ).size() );
			assertEquals( "xyz", result.get( 0 ).get( 1, Collection.class ).iterator().next() );
		} );
	}

	@Test
	public void testArrayConstructorSyntaxEmpty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where e.theArray = []", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 1L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testArrayConstructorSyntaxNonEmpty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-hql-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where e.theArray is not distinct from ['abc', null, 'def']", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-hql-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

}
