/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.util.List;

import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaFunctionJoin;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;

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
import jakarta.persistence.criteria.Nulls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = EntityWithArrays.class)
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsUnnest.class)
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayUnnestTest {

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
	public void testUnnest(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-unnest-example[]
			List<Tuple> results = em.createQuery(
							"select e.id, a " +
									"from EntityWithArrays e " +
									"join e.theArray a " +
									"order by e.id, a nulls first",
							Tuple.class
					)
					.getResultList();
			//end::hql-array-unnest-example[]

			assertEquals( 3, results.size() );
			assertEquals( 2L, results.get( 0 ).get( 0 ) );
			assertNull( results.get( 0 ).get( 1 ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( "abc", results.get( 1 ).get( 1 ) );
			assertEquals( 2L, results.get( 2 ).get( 0 ) );
			assertEquals( "def", results.get( 2 ).get( 1 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "xmltable can't be used with a left join")
	public void testNodeBuilderUnnest(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaRoot<EntityWithArrays> root = cq.from( EntityWithArrays.class );
			final JpaFunctionJoin<String> a = root.joinArray( "theArray", SqmJoinType.LEFT );
			cq.multiselect(
					root.get( "id" ),
					a
			);
			cq.orderBy( cb.asc( root.get( "id" ) ), cb.asc( a ).nullPrecedence( Nulls.FIRST ) );
			final List<Tuple> results = em.createQuery( cq ).getResultList();

			assertEquals( 5, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertNull( results.get( 0 ).get( 1 ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertNull( results.get( 1 ).get( 1 ) );
			assertEquals( 2L, results.get( 2 ).get( 0 ) );
			assertEquals( "abc", results.get( 2 ).get( 1 ) );
			assertEquals( 2L, results.get( 3 ).get( 0 ) );
			assertEquals( "def", results.get( 3 ).get( 1 ) );
			assertEquals( 3L, results.get( 4 ).get( 0 ) );
			assertNull( results.get( 4 ).get( 1 ) );
		} );
	}

}
