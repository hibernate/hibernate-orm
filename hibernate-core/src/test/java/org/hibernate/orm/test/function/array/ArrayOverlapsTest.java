/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.function.array;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HSQLDialect;

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
public class ArrayOverlapsTest {

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
	public void testOverlapsFully(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-overlaps-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_overlaps(e.theArray, array('abc', 'def'))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-overlaps-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testDoesNotOverlap(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_overlaps(e.theArray, array('xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 0, results.size() );
		} );
	}

	@Test
	public void testOverlaps(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_overlaps(e.theArray, array('abc','xyz'))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testOverlapsNullFully(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_overlaps_nullable(e.theArray, array(null))", EntityWithArrays.class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

	@Test
	public void testOverlapsNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-overlaps-nullable-example[]
			List<EntityWithArrays> results = em.createQuery( "from EntityWithArrays e where array_overlaps_nullable(e.theArray, array('xyz',null))", EntityWithArrays.class )
					.getResultList();
			//end::hql-array-overlaps-nullable-example[]
			assertEquals( 1, results.size() );
			assertEquals( 2L, results.get( 0 ).getId() );
		} );
	}

}
