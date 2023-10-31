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
import org.hibernate.dialect.HSQLDialect;
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
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Expression;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class ArrayContainsArrayTest {

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
	public void testContainsArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-contains-array-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_contains(e.theArray, array('abc', 'def'))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-contains-array-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testDoesNotContainArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_contains(e.theArray, array('xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 0, results.size() );
		} );
	}

	@Test
	public void testContainsArrayPartly(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_contains(e.theArray, array('abc','xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 0, results.size() );
		} );
	}

	@Test
	public void testContainsArrayWithNullElementOnly(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_contains_nullable(e.theArray, array(null))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testContainsArrayWithNullElement(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-contains-array-nullable-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_contains_nullable(e.theArray, array('abc',null))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-contains-array-nullable-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testContainsElementParameter(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery(
					"from EntityWithArrays e where array_contains_nullable(e.theArray, :param)",
					EntityWithArrays.class
			).setParameter( "param", "abc" ).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testContainsArrayParameter(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery(
					"from EntityWithArrays e where array_contains_nullable(e.theArray, :param)",
					EntityWithArrays.class
			).setParameter( "param", new String[]{ "abc", null } ).getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testContainsNullParameter(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery(
					"from EntityWithArrays e where array_contains_nullable(e.theArray, :param)",
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
					cb.arrayContainsAll( root.get( "theArray" ), cb.arrayLiteral( "xyz" ) ),
					cb.arrayContainsAll( root.get( "theArray" ), new String[]{ "xyz" } ),
					cb.arrayContainsAll( new String[]{ "abc", "xyz" }, cb.arrayLiteral( "xyz" ) ),
					cb.arrayContainsAllNullable( root.get( "theArray" ), cb.arrayLiteral( "xyz" ) ),
					cb.arrayContainsAllNullable( root.get( "theArray" ), new String[]{ "xyz" } ),
					cb.arrayContainsAllNullable( new String[]{ "abc", "xyz" }, cb.arrayLiteral( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.arrayContainsAll( root.<Integer[]>get( "theArray" ), cb.arrayLiteral( "xyz" ) );
//			cb.arrayContainsAll( root.<Integer[]>get( "theArray" ), new String[]{ "xyz" } );
//			cb.arrayContainsAll( new String[0], cb.literal( 1 ) );
//			cb.arrayContainsAll( new Integer[0], cb.literal( "" ) );
//			cb.arrayContainsAllNullable( root.<Integer[]>get( "theArray" ), cb.arrayLiteral( "xyz" ) );
//			cb.arrayContainsAllNullable( root.<Integer[]>get( "theArray" ), new String[]{ "xyz" } );
//			cb.arrayContainsAllNullable( new String[0], cb.literal( 1 ) );
//			cb.arrayContainsAllNullable( new Integer[0], cb.literal( "" ) );
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
					cb.collectionContainsAll( root.<Collection<String>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionContainsAll( root.get( "theCollection" ), List.of( "xyz" ) ),
					cb.collectionContainsAll( List.of( "abc", "xyz" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionContainsAllNullable( root.<Collection<String>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) ),
					cb.collectionContainsAllNullable( root.get( "theCollection" ), List.of( "xyz" ) ),
					cb.collectionContainsAllNullable( List.of( "abc", "xyz" ), cb.collectionLiteral( "xyz" ) )
			);
			em.createQuery( cq ).getResultList();

			// Should all fail to compile
//			cb.collectionContainsAll( root.<Collection<Integer>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) );
//			cb.collectionContainsAll( root.<Collection<Integer>>get( "theCollection" ), List.of( "xyz" ) );
//			cb.collectionContainsAll( Collections.<String>emptyList(), cb.literal( 1 ) );
//			cb.collectionContainsAll( Collections.<Integer>emptyList(), cb.literal( "" ) );
//			cb.collectionContainsAllNullable( root.<Collection<Integer>>get( "theCollection" ), cb.collectionLiteral( "xyz" ) );
//			cb.collectionContainsAllNullable( root.<Collection<Integer>>get( "theCollection" ), List.of( "xyz" ) );
//			cb.collectionContainsAllNullable( Collections.<String>emptyList(), cb.literal( 1 ) );
//			cb.collectionContainsAllNullable( Collections.<Integer>emptyList(), cb.literal( "" ) );
		} );
	}

}
