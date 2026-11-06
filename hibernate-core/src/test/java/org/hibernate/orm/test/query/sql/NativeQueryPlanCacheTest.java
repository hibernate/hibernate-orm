package org.hibernate.orm.test.query.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = NativeQueryPlanCacheTest.Item.class)
@SessionFactory(generateStatistics = true)
@JiraKey("HHH-20987")
class NativeQueryPlanCacheTest {
	private static final String SQL = "select id from cache_item where id = :id";

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				session.persist( new Item( id ) );
			}
		} );
		scope.getSessionFactory().getQueryEngine().getInterpretationCache().close();
		scope.getSessionFactory().getStatistics().clear();
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void basicResultClassReusesPlan(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				Long result = session.createNativeQuery( SQL, Long.class )
						.setParameter( "id", id )
						.getSingleResult();
				assertEquals( id, result.longValue() );
			}
		} );
		assertPlanCacheCounts( scope, 1, 2 );
	}

	@Test
	void differentResultClassesUseDifferentPlans(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				Object longResult = session.createNativeQuery( SQL, Long.class )
						.setParameter( "id", id )
						.getSingleResult();
				Object integerResult = session.createNativeQuery( SQL, Integer.class )
						.setParameter( "id", id )
						.getSingleResult();
				assertEquals( Long.valueOf( id ), longResult );
				assertEquals( Integer.valueOf( (int) id ), integerResult );
			}
		} );
		assertPlanCacheCounts( scope, 2, 4 );
	}

	@Test
	void inferredScalarReusesPlan(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				Number result = (Number) session.createNativeQuery( SQL )
						.setParameter( "id", id )
						.getSingleResult();
				assertEquals( id, result.longValue() );
			}
		} );
		assertPlanCacheCounts( scope, 1, 2 );
	}

	@Test
	void explicitScalarReusesPlan(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				Object result = session.createNativeQuery( SQL )
						.addScalar( "id", Long.class )
						.setParameter( "id", id )
						.getSingleResult();
				assertEquals( Long.valueOf( id ), result );
			}
		} );
		assertPlanCacheCounts( scope, 1, 2 );
	}

	@Test
	void namedBasicResultClassReusesPlan(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				Long result = session.createNamedQuery( "CacheItem.id", Long.class )
						.setParameter( "id", id )
						.getSingleResult();
				assertEquals( id, result.longValue() );
			}
		} );
		assertPlanCacheCounts( scope, 1, 2 );
	}

	@Test
	void entityResultClassReusesPlan(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 3; id++ ) {
				Item result = session.createNativeQuery( SQL, Item.class )
						.setParameter( "id", id )
						.getSingleResult();
				assertEquals( id, result.id.longValue() );
			}
		} );
		assertPlanCacheCounts( scope, 1, 2 );
	}

	private static void assertPlanCacheCounts(SessionFactoryScope scope, long misses, long hits) {
		var statistics = scope.getSessionFactory().getStatistics();
		assertEquals( misses, statistics.getQueryPlanCacheMissCount() );
		assertEquals( hits, statistics.getQueryPlanCacheHitCount() );
	}

	@Entity(name = "CacheItem")
	@Table(name = "cache_item")
	@NamedNativeQuery(name = "CacheItem.id", query = SQL, resultClass = Long.class)
	static class Item {
		@Id
		private Long id;

		Item() {
		}

		Item(Long id) {
			this.id = id;
		}
	}
}
