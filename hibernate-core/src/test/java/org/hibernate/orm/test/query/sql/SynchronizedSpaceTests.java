/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.sql;

import java.util.function.Consumer;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.spi.NativeQueryImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class SynchronizedSpaceTests extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testNonSyncedCachedScenario() {
		// CachedEntity updated by native-query without adding query spaces
		// 		- the outcome should be all cached data being invalidated

		checkUseCase(
				"cached_entity",
				query -> {},
				// the 2 CachedEntity entries should not be there
				false
		);

		// and of course, let's make sure the update happened :)
		inTransaction(
				session -> {
					session.createQuery( "from CachedEntity", CachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name, is( "updated" ) )
					);
				}
		);
	}

	private void checkUseCase(
			String table,
			Consumer<Query> queryConsumer,
			boolean shouldExistAfter) {

		// first, load both `CachedEntity` instances into the L2 cache
		loadAll();

		final CacheImplementor cacheSystem = sessionFactory().getCache();

		// make sure they are there
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 1 ), is( true ) );
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 2 ), is( true ) );

		// create a query to update the specified table - allowing the passed consumer to register a space if needed
		inTransaction(
				session -> {
					// notice the type is the JPA Query interface
					final Query nativeQuery = session.createNativeQuery( "update " + table + " set name = 'updated'" );
					queryConsumer.accept( nativeQuery );
					nativeQuery.executeUpdate();
				}
		);

		// see if the entries exist based on the expectation
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 1 ), is( shouldExistAfter ) );
		assertThat( cacheSystem.containsEntity( CachedEntity.class, 2 ), is( shouldExistAfter ) );
	}

	@Test
	public void testSyncedCachedScenario() {
		final String tableName = "cached_entity";

		checkUseCase(
				tableName,
				query -> ( (NativeQueryImplementor<?>) query ).addSynchronizedQuerySpace( tableName ),
				// the 2 CachedEntity entries should not be there
				false
		);

		// and of course, let's make sure the update happened :)
		inTransaction(
				session -> {
					session.createQuery( "from CachedEntity", CachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name, is( "updated" ) )
					);
				}
		);
	}

	@Test
	public void testNonSyncedNonCachedScenario() {
		// NonCachedEntity updated by native-query without adding query spaces
		// 		- the outcome should be all cached data being invalidated

		checkUseCase(
				"non_cached_entity",
				query -> {},
				// the 2 CachedEntity entries should not be there
				false
		);

		// and of course, let's make sure the update happened :)
		inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name, is( "updated" ) )
					);
				}
		);
	}

	@Test
	public void testSyncedNonCachedScenario() {
		// NonCachedEntity updated by native-query with query spaces
		// 		- the caches for CachedEntity are not invalidated - they are not affected by the specified query-space

		final String tableName = "non_cached_entity";

		checkUseCase(
				tableName,
				query -> ( (NativeQueryImplementor<?>) query ).addSynchronizedQuerySpace( tableName ),
				// the 2 CachedEntity entries should still be there
				true
		);

		// and of course, let's make sure the update happened :)
		inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name, is( "updated" ) )
					);
				}
		);
	}

	@Test
	public void testSyncedNonCachedScenarioUsingHint() {
		// same as `#testSyncedNonCachedScenario`, but here using the hint

		final String tableName = "non_cached_entity";

		checkUseCase(
				tableName,
				query -> query.setHint( QueryHints.HINT_NATIVE_SPACES, tableName ),
				// the 2 CachedEntity entries should still be there
				true
		);

		// and of course, let's make sure the update happened :)
		inTransaction(
				session -> {
					session.createQuery( "from NonCachedEntity", NonCachedEntity.class ).list().forEach(
							cachedEntity -> assertThat( cachedEntity.name, is( "updated" ) )
					);
				}
		);
	}

	private void loadAll() {
		inTransaction(
				session -> {
					session.createQuery( "from CachedEntity" ).list();

					// this one is not strictly needed since this entity is not cached.
					// but it helps my OCD feel better to have it ;)
					session.createQuery( "from NonCachedEntity" ).list();
				}
		);
	}

	public void prepareTest() {
		inTransaction(
				session -> {
					session.persist( new CachedEntity( 1, "first cached" ) );
					session.persist( new CachedEntity( 2, "second cached" ) );

					session.persist( new NonCachedEntity( 1, "first non-cached" ) );
					session.persist( new NonCachedEntity( 2, "second non-cached" ) );
				}
		);

		cleanupCache();
	}

	public void cleanupTest() {
		cleanupCache();

		inTransaction(
				session -> {
					session.createQuery( "delete CachedEntity" ).executeUpdate();
					session.createQuery( "delete NonCachedEntity" ).executeUpdate();
				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { CachedEntity.class, NonCachedEntity.class };
	}

	@Override
	protected boolean overrideCacheStrategy() {
		return false;
	}

	@Entity( name = "CachedEntity" )
	@Table( name = "cached_entity" )
	@Cacheable( true )
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
	public static class CachedEntity {
		@Id
		private Integer id;
		private String name;

		public CachedEntity() {
		}

		public CachedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "NonCachedEntity" )
	@Table( name = "non_cached_entity" )
	@Cacheable( false )
	public static class NonCachedEntity {
		@Id
		private Integer id;
		private String name;

		public NonCachedEntity() {
		}

		public NonCachedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
