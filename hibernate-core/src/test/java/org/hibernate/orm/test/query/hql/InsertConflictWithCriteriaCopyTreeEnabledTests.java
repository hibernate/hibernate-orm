/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.query.criteria.JpaCriteriaInsertValues;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;


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
