/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.orm.test.mapping.SmokeTests;

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
}
