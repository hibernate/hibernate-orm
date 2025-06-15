/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.orm.test.jpa.CompositeId;
import org.hibernate.orm.test.jpa.EntityWithCompositeId;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Rob Green
 */
@JiraKey( value = "HHH-8370" )
@RequiresDialect( value = SQLServerDialect.class, majorVersion = 10 )
@Jpa(
		annotatedClasses = { EntityWithCompositeId.class },
		integrationSettings = {
				@Setting( name = AvailableSettings.USE_SQL_COMMENTS, value = "true" ),
		},
		useCollectingStatementInspector = true
)
public class SQLServerDialectCompositeTest {
	@Test
	public void testCompositeQueryWithInPredicate(EntityManagerFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();

		List<CompositeId> compositeIds = new ArrayList<>();
		compositeIds.add( new CompositeId( 1,2 ) );
		compositeIds.add( new CompositeId( 3,4 ) );

		scope.inTransaction( entityManager -> {
					entityManager.createQuery( "SELECT e FROM EntityWithCompositeId e WHERE e.id IN (:ids)" )
							.setParameter( "ids", compositeIds )
							.getResultList();
				}
		);

		var query = sqlStatementInterceptor.getSqlQueries().get( 0 );
		assertTrue( query.endsWith( "where exists (select 1 from (values (?,?), (?,?)) as v(id1, id2) where ewci1_0.id1 = v.id1 and ewci1_0.id2 = v.id2)" ) );
	}

	@Test
	public void testCompositeQueryWithNotInPredicate(EntityManagerFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();

		List<CompositeId> compositeIds = new ArrayList<>();
		compositeIds.add( new CompositeId( 1,2 ) );
		compositeIds.add( new CompositeId( 3,4 ) );

		scope.inTransaction( entityManager -> {
					entityManager.createQuery( "SELECT e FROM EntityWithCompositeId e WHERE e.id NOT IN (:ids)" )
							.setParameter( "ids", compositeIds )
							.getResultList();
				}
		);

		var query = sqlStatementInterceptor.getSqlQueries().get( 0 );
		assertTrue( query.endsWith( "where not exists (select 1 from (values (?,?), (?,?)) as v(id1, id2) where ewci1_0.id1 = v.id1 and ewci1_0.id2 = v.id2)" ) );
	}

	@Test
	public void testCompositeQueryWithMultiplePredicatesIncludingIn(EntityManagerFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();

		List<CompositeId> compositeIds = new ArrayList<>();
		compositeIds.add( new CompositeId( 1,2 ) );
		compositeIds.add( new CompositeId( 3,4 ) );

		scope.inTransaction( entityManager -> {
					entityManager.createQuery( "SELECT e FROM EntityWithCompositeId e WHERE e.description = :description AND e.id IN (:ids)" )
							.setParameter( "ids", compositeIds )
							.setParameter( "description", "test" )
							.getResultList();
				}
		);

		var query = sqlStatementInterceptor.getSqlQueries().get( 0 );
		assertTrue( query.endsWith( "where ewci1_0.description=? and exists (select 1 from (values (?,?), (?,?)) as v(id1, id2) where ewci1_0.id1 = v.id1 and ewci1_0.id2 = v.id2)" ) );
	}
}
