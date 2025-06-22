/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;
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

import jakarta.persistence.PersistenceException;
import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayTrim.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayTrimTest {

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
	public void testTrimOne(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-trim-example[]
			List<Tuple> results = em.createQuery( "select e.id, array_trim(e.theArray, 1) from EntityWithArrays e where e.id = 2", Tuple.class )
					.getResultList();
			//end::hql-array-trim-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[] { "abc", null }, results.get( 0 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testTrimAll(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery( "select e.id, array_trim(e.theArray, 3) from EntityWithArrays e where e.id = 2", Tuple.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[0], results.get( 0 ).get( 1, String[].class ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, majorVersion = 13, matchSubTypes = true, reason = "The PostgreSQL emulation for version < 14 doesn't throw an error")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "The Cockroach emulation doesn't throw an error")
	public void testTrimOutOfRange(SessionFactoryScope scope) {
		scope.inSession( em -> {
			try {
				em.createQuery( "select array_trim(e.theArray, 1) from EntityWithArrays e where e.id = 1" )
						.getResultList();
				fail( "Should fail because array is too small to trim!" );
			}
			catch (PersistenceException ex) {
				assertInstanceOf( SQLException.class, ex.getCause() );
			}
		} );
	}

	@Test
	public void testNodeBuilderArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<EntityWithArrays> root = cq.from( EntityWithArrays.class );
			cq.where( root.get( "id" ).equalTo( 2L ) );
			cq.multiselect(
					root.get( "id" ),
					cb.arrayTrim( root.<String[]>get( "theArray" ), cb.literal( 1 ) ),
					cb.arrayTrim( root.get( "theArray" ), 1 )
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
			cq.where( root.get( "id" ).equalTo( 2L ) );
			cq.multiselect(
					root.get( "id" ),
					cb.collectionTrim( root.<Collection<String>>get( "theCollection" ), cb.literal( 1 ) ),
					cb.collectionTrim( root.get( "theCollection" ), 1 )
			);
			em.createQuery( cq ).getResultList();
		} );
	}

}
