/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Calendar;
import java.util.List;

import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel( annotatedClasses = SimpleEntity.class )
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
public class SubqueryLimitOffsetTest {
	@Test
	public void testSubqueryLimitOffset(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List results = session.createQuery(
							"select o from SimpleEntity o where o.someString = ( select oSub.someString from SimpleEntity oSub order by oSub.someString limit 1 )" )
							.list();
					assertThat( results.size(), is( 2 ) );
				} );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity entity = new SimpleEntity(
							1,
							Calendar.getInstance().getTime(),
							null,
							Integer.MAX_VALUE,
							Long.MAX_VALUE,
							"some"
					);
					session.persist( entity );

					SimpleEntity second_entity = new SimpleEntity(
							2,
							Calendar.getInstance().getTime(),
							null,
							Integer.MIN_VALUE,
							Long.MAX_VALUE,
							"some"
					);
					session.persist( second_entity );

				} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
