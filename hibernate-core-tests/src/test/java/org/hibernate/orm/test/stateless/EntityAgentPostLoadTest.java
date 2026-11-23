package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;

import org.hibernate.annotations.Cache;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = EntityAgentPostLoadTest.Book.class,
		useCollectingStatementInspector = true,
		properties = @Setting(name = AvailableSettings.CACHE_REGION_FACTORY,
				value = "org.hibernate.testing.cache.CachingRegionFactory"))
@JiraKey("HHH-21109")
class EntityAgentPostLoadTest {
	@BeforeEach
	void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.persist( new Book( 1, "Stored title" ) ) );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
		scope.getEntityManagerFactory().getCache().evictAll();
	}

	@Test
	void find(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getCache().evict( Book.class, 1 );
		scope.getEntityManagerFactory().runInTransaction( EntityAgent.class, agent ->
				assertPostLoad( agent.find( Book.class, 1 ) ) );
	}

	@Test
	void refresh(EntityManagerFactoryScope scope) {
		var book = new Book( 1, "Stale title" );
		scope.getEntityManagerFactory().runInTransaction( EntityAgent.class, agent -> {
			agent.refresh( book );
			assertPostLoad( book );
		} );
	}

	@Test
	void query(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().runInTransaction( EntityAgent.class, agent ->
				assertPostLoad( agent.createQuery( "select b from Book b", Book.class ).getSingleResult() ) );
	}

	@Test
	void findFromSecondLevelCache(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> em.find( Book.class, 1 ) );
		scope.getEntityManagerFactory().runInTransaction( EntityAgent.class, agent -> {
			var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			var book = agent.find( Book.class, 1 );
			inspector.assertExecutedCount( 0 );
			assertPostLoad( book );
		} );
	}

	private static void assertPostLoad(Book book) {
		assertEquals( 1, book.postLoadCalls );
		assertEquals( "Stored title", book.loadedTitle );
	}

	@Entity(name = "Book")
	@Cache(usage = READ_WRITE)
	static class Book {
		@Id int id;
		String title;
		transient int postLoadCalls;
		transient String loadedTitle;

		Book() {
		}

		Book(int id, String title) {
			this.id = id;
			this.title = title;
		}

		@PostLoad
		void postLoad() {
			postLoadCalls++;
			loadedTitle = title;
		}
	}
}
