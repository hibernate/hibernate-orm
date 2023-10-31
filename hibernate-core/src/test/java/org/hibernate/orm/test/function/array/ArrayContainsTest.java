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

}
