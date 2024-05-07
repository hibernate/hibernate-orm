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
public class ArrayIntersectsTest {

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
	public void testIntersectsFully(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-intersects-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_intersects(e.theArray, array('abc', 'def'))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-intersects-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testDoesNotIntersect(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_intersects(e.theArray, array('xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 0, results.size() );
		} );
	}

	@Test
	public void testIntersects(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_intersects(e.theArray, array('abc','xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testIntersectsNullFully(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_intersects_nullable(e.theArray, array(null))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testIntersectsNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-intersects-nullable-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_intersects_nullable(e.theArray, array('xyz',null))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-intersects-nullable-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
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
					cb.arrayIntersects( root.get( "theArray" ), cb.arrayLiteral( "xyz" ) ),
					cb.arrayIntersects( root.get( "theArray" ), new String[]{ "xyz" } ),
					cb.arrayIntersects( new String[]{ "abc", "xyz" }, cb.arrayLiteral( "xyz" ) ),
					cb.arrayIntersectsNullable( root.get( "theArray" ), cb.arrayLiteral( "xyz" ) ),
					cb.arrayIntersectsNullable( root.get( "theArray" ), new String[]{ "xyz" } ),
					cb.arrayIntersectsNullable( new String[]{ "abc", "xyz" }, cb.arrayLiteral( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayIntersects( root.<Integer[]>get( "theArray" ), cb.arrayLiteral( "xyz" ) );
//			cb.arrayIntersects( root.<Integer[]>get( "theArray" ), new String[]{ "xyz" } );
//			cb.arrayIntersects( new String[0], cb.literal( 1 ) );
//			cb.arrayIntersects( new Integer[0], cb.literal( "" ) );
//			cb.arrayIntersectsNullable( root.<Integer[]>get( "theArray" ), cb.arrayLiteral( "xyz" ) );
//			cb.arrayIntersectsNullable( root.<Integer[]>get( "theArray" ), new String[]{ "xyz" } );
//			cb.arrayIntersectsNullable( new String[0], cb.literal( 1 ) );
//			cb.arrayIntersectsNullable( new Integer[0], cb.literal( "" ) );
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
					cb.collectionIntersects( root.<Collection<String>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionIntersects( root.get( "theCollection" ), List.of( "xyz" ) ),
					cb.collectionIntersects( List.of( "abc", "xyz" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionIntersectsNullable( root.<Collection<String>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionIntersectsNullable( root.get( "theCollection" ), List.of( "xyz" ) ),
					cb.collectionIntersectsNullable( List.of( "abc", "xyz" ), cb.collectionLiteral( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionIntersects( root.<Collection<Integer>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) );
//			cb.collectionIntersects( root.<Collection<Integer>>get( "theCollection" ), List.of( "xyz" ) );
//			cb.collectionIntersects( Collections.<String>emptyList(), cb.literal( 1 ) );
//			cb.collectionIntersects( Collections.<Integer>emptyList(), cb.literal( "" ) );
//			cb.collectionIntersectsNullable( root.<Collection<Integer>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) );
//			cb.collectionIntersectsNullable( root.<Collection<Integer>>get( "theCollection" ), List.of( "xyz" ) );
//			cb.collectionIntersectsNullable( Collections.<String>emptyList(), cb.literal( 1 ) );
//			cb.collectionIntersectsNullable( Collections.<Integer>emptyList(), cb.literal( "" ) );
		} );
	}

	@Test
	public void testIntersectsSyntax(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-intersects-hql-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where e.theArray intersects ['abc','xyz']", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-intersects-hql-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

}
