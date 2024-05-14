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
public class ArrayIncludesTest {

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
	public void testIncludesArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-includes-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_includes(e.theArray, array('abc', 'def'))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-includes-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testDoesNotIncludeArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_includes(e.theArray, array('xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 0, results.size() );
		} );
	}

	@Test
	public void testIncludesArrayPartly(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_includes(e.theArray, array('abc','xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 0, results.size() );
		} );
	}

	@Test
	public void testIncludesArrayWithNullElementOnly(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_includes_nullable(e.theArray, array(null))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testIncludesArrayWithNullElement(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-includes-nullable-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_includes_nullable(e.theArray, array('abc',null))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-includes-nullable-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testIncludesArrayParameter(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery(
					"from EntityWithArrays e where array_includes_nullable(e.theArray, :param)",
					EntityWithArrays.class
			).setParameter( "param", new String[]{ "abc", null } ).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testIncludesNullParameter(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery(
					"from EntityWithArrays e where array_includes_nullable(e.theArray, :param)",
					EntityWithArrays.class
			).setParameter( "param", null ).getResultList();
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
					cb.arrayIncludes( root.get( "theArray" ), cb.arrayLiteral( "xyz" ) ),
					cb.arrayIncludes( root.get( "theArray" ), new String[]{ "xyz" } ),
					cb.arrayIncludes( new String[]{ "abc", "xyz" }, cb.arrayLiteral( "xyz" ) ),
					cb.arrayIncludesNullable( root.get( "theArray" ), cb.arrayLiteral( "xyz" ) ),
					cb.arrayIncludesNullable( root.get( "theArray" ), new String[]{ "xyz" } ),
					cb.arrayIncludesNullable( new String[]{ "abc", "xyz" }, cb.arrayLiteral( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayIncludes( root.<Integer[]>get( "theArray" ), cb.arrayLiteral( "xyz" ) );
//			cb.arrayIncludes( root.<Integer[]>get( "theArray" ), new String[]{ "xyz" } );
//			cb.arrayIncludes( new String[0], cb.literal( 1 ) );
//			cb.arrayIncludes( new Integer[0], cb.literal( "" ) );
//			cb.arrayIncludesNullable( root.<Integer[]>get( "theArray" ), cb.arrayLiteral( "xyz" ) );
//			cb.arrayIncludesNullable( root.<Integer[]>get( "theArray" ), new String[]{ "xyz" } );
//			cb.arrayIncludesNullable( new String[0], cb.literal( 1 ) );
//			cb.arrayIncludesNullable( new Integer[0], cb.literal( "" ) );
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
					cb.collectionIncludes( root.<Collection<String>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionIncludes( root.get( "theCollection" ), List.of( "xyz" ) ),
					cb.collectionIncludes( List.of( "abc", "xyz" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionIncludesNullable( root.<Collection<String>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionIncludesNullable( root.get( "theCollection" ), List.of( "xyz" ) ),
					cb.collectionIncludesNullable( List.of( "abc", "xyz" ), cb.collectionLiteral( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionIncludes( root.<Collection<Integer>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) );
//			cb.collectionIncludes( root.<Collection<Integer>>get( "theCollection" ), List.of( "xyz" ) );
//			cb.collectionIncludes( Collections.<String>emptyList(), cb.literal( 1 ) );
//			cb.collectionIncludes( Collections.<Integer>emptyList(), cb.literal( "" ) );
//			cb.collectionIncludesNullable( root.<Collection<Integer>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) );
//			cb.collectionIncludesNullable( root.<Collection<Integer>>get( "theCollection" ), List.of( "xyz" ) );
//			cb.collectionIncludesNullable( Collections.<String>emptyList(), cb.literal( 1 ) );
//			cb.collectionIncludesNullable( Collections.<Integer>emptyList(), cb.literal( "" ) );
		} );
	}

	@Test
	public void testIncludesSyntax(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-includes-hql-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where e.theArray includes ['abc', 'def']", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-includes-hql-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

}
