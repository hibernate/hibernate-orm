/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.testing.orm.junit.Jira;
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
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayPositionsTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new EntityWithArrays( 1L, new String[]{} ) );
			em.persist( new EntityWithArrays( 2L, new String[]{ "abc", null, "def", "abc" } ) );
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
	public void testPositions(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-positions-example[]
			List<int[]> results = em.createQuery( "select array_positions(e.theArray, 'abc') from EntityWithArrays e order by e.id", int[].class )
					.getResultList();
			//end::hql-array-positions-example[]
			assertEquals( 3, results.size() );
			assertArrayEquals( new int[0], results.get( 0 ) );
			assertArrayEquals( new int[]{ 1, 4 }, results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

	@Test
	public void testPositionsNotFound(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<int[]> results = em.createQuery( "select array_positions(e.theArray, 'xyz') from EntityWithArrays e order by e.id", int[].class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertArrayEquals( new int[0], results.get( 0 ) );
			assertArrayEquals( new int[0], results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

	@Test
	public void testPositionsNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<int[]> results = em.createQuery( "select array_positions(e.theArray, null) from EntityWithArrays e order by e.id", int[].class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertArrayEquals( new int[0], results.get( 0 ) );
			assertArrayEquals( new int[]{ 2 }, results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19490")
	public void testPositionsParam(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<int[]> results = em.createQuery( "select array_positions(e.theArray, ?1) from EntityWithArrays e order by e.id", int[].class )
					.setParameter( 1, "abc" )
					.getResultList();
			assertEquals( 3, results.size() );
			assertArrayEquals( new int[0], results.get( 0 ) );
			assertArrayEquals( new int[]{ 1, 4 }, results.get( 1 ) );
			assertNull( results.get( 2 ) );
		} );
	}

	@Test
	public void testPositionsList(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<List<Integer>> results = em.createQuery( "select array_positions_list(e.theArray, null) from EntityWithArrays e order by e.id" )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( List.of(), results.get( 0 ) );
			assertEquals( List.of( 2 ), results.get( 1 ) );
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
					cb.arrayPositions( root.<String[]>get( "theArray" ), cb.literal( "xyz" ) ),
					cb.arrayPositions( root.get( "theArray" ), "xyz" ),
					cb.arrayPositionsList( root.<String[]>get( "theArray" ), cb.literal( "xyz" ) ),
					cb.arrayPositionsList( root.get( "theArray" ), "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayPositions( root.<Integer[]>get( "theArray" ), cb.literal( "xyz" ) );
//			cb.arrayPositions( root.<Integer[]>get( "theArray" ), "xyz" );
//			cb.arrayPositionsList( root.<Integer[]>get( "theArray" ), cb.literal( "xyz" ) );
//			cb.arrayPositionsList( root.<Integer[]>get( "theArray" ), "xyz" );
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
					cb.collectionPositions( root.<Collection<String>>get( "theCollection" ), cb.literal( "xyz" ) ),
					cb.collectionPositions( root.get( "theCollection" ), "xyz" ),
					cb.collectionPositionsList( root.<Collection<String>>get( "theCollection" ), cb.literal( "xyz" ) ),
					cb.collectionPositionsList( root.get( "theCollection" ), "xyz" )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionPositions( root.<Collection<Integer>>get( "theCollection" ), cb.literal( "xyz" ) );
//			cb.collectionPositions( root.<Collection<Integer>>get( "theCollection" ), "xyz" );
//			cb.collectionPositionsList( root.<Collection<Integer>>get( "theCollection" ), cb.literal( "xyz" ) );
//			cb.collectionPositionsList( root.<Collection<Integer>>get( "theCollection" ), "xyz" );
		} );
	}

}
