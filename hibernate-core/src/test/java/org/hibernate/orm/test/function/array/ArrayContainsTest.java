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
import org.hibernate.testing.orm.junit.JiraKey;
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
// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
@BootstrapServiceRegistry(integrators = SharedDriverManagerTypeCacheClearingIntegrator.class)
public class ArrayContainsTest {

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
	public void testContains(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-contains-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_contains(e.theArray, 'abc')", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-contains-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testDoesNotContain(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_contains(e.theArray, 'xyz')", EntityWithArrays.class )
					.getResultList();
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
					cb.arrayContains( root.<String[]>get( "theArray" ), cb.literal( "xyz" ) ),
					cb.arrayContains( root.get( "theArray" ), "xyz" ),
					cb.arrayContains( new String[]{ "abc", "xyz" }, cb.literal( "xyz" ) ),
					cb.arrayContainsNullable( root.<String[]>get( "theArray" ), cb.literal( "xyz" ) ),
					cb.arrayContainsNullable( root.get( "theArray" ), "xyz" ),
					cb.arrayContainsNullable( new String[]{ "abc", "xyz" }, cb.literal( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayContains( root.<Integer[]>get( "theArray" ), cb.literal( "xyz" ) );
//			cb.arrayContains( root.<Integer[]>get( "theArray" ), "xyz" );
//			cb.arrayContains( new String[]{ "abc", "xyz" }, cb.<Integer>literal( 1 ) );
//			cb.arrayContains( new Integer[0], cb.<String>literal( "" ) );
//			cb.arrayContainsNullable( root.<Integer[]>get( "theArray" ), cb.literal( "xyz" ) );
//			cb.arrayContainsNullable( root.<Integer[]>get( "theArray" ), "xyz" );
//			cb.arrayContainsNullable( new String[]{ "abc", "xyz" }, cb.<Integer>literal( 1 ) );
//			cb.arrayContainsNullable( new Integer[0], cb.<String>literal( "" ) );
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
					cb.collectionContains( root.<Collection<String>>get( "theCollection" ), cb.literal( "xyz" ) ),
					cb.collectionContains( root.get( "theCollection" ), "xyz" ),
					cb.collectionContains( List.of( "abc", "xyz" ), cb.literal( "xyz" ) ),
					cb.collectionContainsNullable( root.<Collection<String>>get( "theCollection" ), cb.literal( "xyz" ) ),
					cb.collectionContainsNullable( root.get( "theCollection" ), "xyz" ),
					cb.collectionContainsNullable( List.of( "abc", "xyz" ), cb.literal( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionContains( root.<Collection<Integer>>get( "theCollection" ), cb.literal( "xyz" ) );
//			cb.collectionContains( root.<Collection<Integer>>get( "theCollection" ), "xyz" );
//			cb.collectionContains( List.of( "abc", "xyz" ), cb.<Integer>literal( 1 ) );
//			cb.collectionContains( Collections.<Integer>emptyList(), cb.<String>literal( "" ) );
//			cb.collectionContainsNullable( root.<Collection<Integer>>get( "theCollection" ), cb.literal( "xyz" ) );
//			cb.collectionContainsNullable( root.<Collection<Integer>>get( "theCollection" ), "xyz" );
//			cb.collectionContainsNullable( List.of( "abc", "xyz" ), cb.<Integer>literal( 1 ) );
//			cb.collectionContainsNullable( Collections.<Integer>emptyList(), cb.<String>literal( "" ) );
		} );
	}

	@Test
	public void testContainsSyntax(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-contains-hql-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where e.theArray contains 'abc'", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-contains-hql-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testInSyntax(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-in-hql-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where 'abc' in e.theArray", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-in-hql-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	@JiraKey( "HHH-18851" )
	public void testInArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery(
							"select e.id " +
									"from EntityWithArrays e " +
									"where :p in e.theArray",
							Tuple.class
					)
					.setParameter( "p", "abc" )
					.getResultList();

			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).get( 0 ) );
		} );
	}

}
