/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.orm.test.mapping.SmokeTests;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SmokeTests.SimpleEntity.class )
@ServiceRegistry
@SessionFactory( exportSchema = true )
public class QueryPlanCachingTest {
	@Test
	public void testHqlTranslationCaching(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select e from SimpleEntity e" ).list();
					session.createQuery( "select e from SimpleEntity e" ).list();
				}
		);
	}

	@Test
	public void testCriteriaTranslationCaching(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.setCriteriaPlanCacheEnabled( true );
					session.createQuery( constructCriteriaQuery( session ) ).list();
					session.createQuery( constructCriteriaQuery( session ) ).list();
				}
		);
	}

	private static JpaCriteriaQuery<SmokeTests.SimpleEntity> constructCriteriaQuery(SessionImplementor session) {
		final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
		final JpaCriteriaQuery<SmokeTests.SimpleEntity> query = cb.createQuery( SmokeTests.SimpleEntity.class );
		final JpaRoot<SmokeTests.SimpleEntity> root = query.from( SmokeTests.SimpleEntity.class );
		query.select( root );
		return query;
	}
}
