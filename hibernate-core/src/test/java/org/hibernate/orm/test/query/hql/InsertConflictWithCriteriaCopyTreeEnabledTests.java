package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.query.criteria.JpaCriteriaInsertValues;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DomainModel(
		annotatedClasses = {
				InsertConflictWithCriteriaCopyTreeEnabledTests.TestEntity.class,
				InsertConflictWithCriteriaCopyTreeEnabledTests.AnotherTestEntity.class,
		}
)
@ServiceRegistry(
		settings = {@Setting(name = QuerySettings.CRITERIA_COPY_TREE, value = "true")}
)
@SessionFactory
@JiraKey("HHH-19314")
@SkipForDialect( dialectClass = SpannerPostgreSQLDialect.class,
		reason = "ON CONFLICT clause with empty conflict target in INSERT statement is not supported")
@SkipForDialect( dialectClass = SpannerDialect.class, reason = "UNIMPLEMENTED: ON CONFLICT clause with empty conflict target in INSERT statement is not supported in Emulator")
public class InsertConflictWithCriteriaCopyTreeEnabledTests {

	@Test
	void createCriteriaInsertValuesTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					JpaCriteriaInsertValues<TestEntity> insertIntoItem = cb
							.createCriteriaInsertValues( TestEntity.class );
					insertIntoItem.setInsertionTargetPaths( insertIntoItem.getTarget().get( "id" ) );
					insertIntoItem.values( cb.values( cb.value( 1L ) ) );
					insertIntoItem.onConflict().onConflictDoNothing();

					session.createMutationQuery( insertIntoItem ).executeUpdate();
				}
		);
	}

	@Test
	void createCriteriaInsertSelectTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					JpaCriteriaInsertSelect<TestEntity> insertIntoItem = cb
							.createCriteriaInsertSelect( TestEntity.class );
					insertIntoItem.setInsertionTargetPaths( insertIntoItem.getTarget().get( "id" ) );

					JpaCriteriaQuery<Tuple> cq = cb.createQuery( Tuple.class );
					cq.select( cb.tuple( cb.literal( 1 ) ) );
					cq.fetch( 1 );
					insertIntoItem.select( cq );
					insertIntoItem.onConflict().onConflictDoNothing();

					session.createMutationQuery( insertIntoItem ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey("HHH-20755")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUpsertOrMerge.class)
	void createCriteriaInsertSelectOnConflictTargetTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity entity = new TestEntity();
			entity.id = 2L;
			session.persist( entity );
			session.flush();

			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaInsertSelect<TestEntity> insert = cb.createCriteriaInsertSelect( TestEntity.class );
			insert.setInsertionTargetPaths( insert.getTarget().get( "id" ) );
			final JpaCriteriaQuery<Tuple> select = cb.createTupleQuery();
			select.select( cb.tuple( cb.literal( 2L ) ) );
			insert.select( select );
			insert.onConflict().conflictOnConstraintPaths( insert.getTarget().get( "id" ) )
					.onConflictDoNothing();
			session.createMutationQuery( insert ).executeUpdate();

			assertEquals( 1L, session.createSelectionQuery(
					"select count(*) from TestEntity where id = 2", Long.class
			).getSingleResult() );
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

	}

	@Entity(name = "AnotherTestEntity")
	public static class AnotherTestEntity {
		@Id
		private Long id;

		private String name;

	}
}
