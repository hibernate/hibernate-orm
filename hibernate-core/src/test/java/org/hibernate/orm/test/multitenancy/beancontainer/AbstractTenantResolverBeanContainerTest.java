/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.beancontainer;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Yanming Zhou
 */
@JiraKey("HHH-15422")
@SessionFactory
@DomainModel(annotatedClasses = TestEntity.class)
abstract class AbstractTenantResolverBeanContainerTest {

	@Test
	void tentantIdShouldBeFilled(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			TestEntity entity = new TestEntity();
			s.persist( entity );
			s.flush();
			assertThat( entity.getTenant(), is( TestCurrentTenantIdentifierResolver.FIXED_TENANT ) );
		} );
	}

}
