/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.function.array;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
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
// Make sure this stuff runs on a dedicated connection pool,
// otherwise we might run into ORA-21700: object does not exist or is marked for delete
// because the JDBC connection or database session caches something that should have been invalidated
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = ""))
public class ArrayConcatTest {

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
	public void testConcatAppend(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-concat-example[]
			List<Tuple> results = em.createQuery( "select e.id, array_concat(e.theArray, array('xyz')) from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			//end::hql-array-concat-example[]
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[]{ "xyz" }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[]{ "abc", null, "def", "xyz" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testConcatPrepend(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> results = em.createQuery( "select e.id, array_concat(array('xyz'), e.theArray) from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[]{ "xyz" }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[]{ "xyz", "abc", null, "def" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testConcatPipe(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-concat-pipe-example[]
			List<Tuple> results = em.createQuery( "select e.id, e.theArray || array('xyz') from EntityWithArrays e order by e.id", Tuple.class )
					.getResultList();
			//end::hql-array-concat-pipe-example[]
			assertEquals( 3, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertArrayEquals( new String[]{ "xyz" }, results.get( 0 ).get( 1, String[].class ) );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertArrayEquals( new String[]{ "abc", null, "def", "xyz" }, results.get( 1 ).get( 1, String[].class ) );
			assertEquals( 3L, results.get( 2 ).get( 0 ) );
			assertNull( results.get( 2 ).get( 1, String[].class ) );
		} );
	}

	@Test
	public void testConcatPipeTwoArrayPaths(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery( "select e.id, e.theArray || e.theArray from EntityWithArrays e order by e.id" ).getResultList();
		} );
	}

	@Test
	public void testConcatPipeAppendLiteral(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-concat-pipe-element-example[]
			em.createQuery( "select e.id, e.theArray || 'last' from EntityWithArrays e order by e.id" ).getResultList();
			//end::hql-array-concat-pipe-element-example[]
		} );
	}

	@Test
	public void testConcatPipePrependLiteral(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery( "select e.id, 'first' || e.theArray from EntityWithArrays e order by e.id" ).getResultList();
		} );
	}

	@Test
	public void testConcatPipeAppendNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery( "select e.id, e.theArray || null from EntityWithArrays e order by e.id" ).getResultList();
		} );
	}

	@Test
	public void testConcatPipePrependNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery( "select e.id, null || e.theArray from EntityWithArrays e order by e.id" ).getResultList();
		} );
	}

	@Test
	public void testConcatPipeAppendParameter(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery( "select e.id, e.theArray || :p from EntityWithArrays e order by e.id" )
					.setParameter( "p", null )
					.getResultList();
			em.createQuery( "select e.id, e.theArray || :p from EntityWithArrays e order by e.id" )
					.setParameter( "p", "last" )
					.getResultList();
			em.createQuery( "select e.id, e.theArray || :p from EntityWithArrays e order by e.id" )
					.setParameter( "p", new String[]{ "last" } )
					.getResultList();
		} );
	}

	@Test
	public void testConcatPipePrependParameter(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.createQuery( "select e.id, :p || e.theArray from EntityWithArrays e order by e.id" )
					.setParameter( "p", null )
					.getResultList();
			em.createQuery( "select e.id, :p || e.theArray from EntityWithArrays e order by e.id" )
					.setParameter( "p", "first" )
					.getResultList();
			em.createQuery( "select e.id, :p || e.theArray from EntityWithArrays e order by e.id" )
					.setParameter( "p", new String[]{ "first" } )
					.getResultList();
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
					cb.arrayConcat( root.get( "theArray" ), cb.arrayLiteral( "xyz" ) ),
					cb.arrayConcat( root.get( "theArray" ), new String[]{ "xyz" } ),
					cb.arrayConcat( new String[]{ "xyz" }, root.get( "theArray" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayConcat( root.<Integer[]>get( "theArray" ), cb.literal( new String[]{ "xyz" } ) );
//			cb.arrayConcat( root.<Integer[]>get( "theArray" ), new String[]{ "xyz" } );
//			cb.arrayConcat( new String[]{ "xyz" }, root.<Integer[]>get( "theArray" ) );
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
					cb.collectionConcat( root.get( "theCollection" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionConcat( root.get( "theCollection" ), List.of( "xyz" ) ),
					cb.collectionConcat( List.of( "xyz" ), root.get( "theCollection" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionConcat( root.<Collection<Integer>>get( "theCollection" ), cb.literal( List.of( "xyz" ) ) );
//			cb.collectionConcat( root.<Collection<Integer>>get( "theCollection" ), List.of( "xyz" ) );
//			cb.collectionConcat( List.of( "xyz" ), root.<Collection<Integer>>get( "theCollection" ) );
		} );
	}

}
