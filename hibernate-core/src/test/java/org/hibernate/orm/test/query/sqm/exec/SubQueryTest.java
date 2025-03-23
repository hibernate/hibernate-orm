/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.exec;

import java.util.List;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
public class SubQueryTest {
	@Test
	public void testSubQueryWithMaxFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String hql = "SELECT e FROM BasicEntity e WHERE e.id = " +
							"(SELECT max(e1.id) FROM BasicEntity e1 WHERE e1.data = :data)";

					List<BasicEntity> results = session
							.createQuery( hql, BasicEntity.class )
							.setParameter( "data", "e1" )
							.getResultList();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ).getId(), is( 3 ) );
					assertThat( results.get( 0 ).getData(), is( "e1" ) );
				}
		);
	}

	@Test
	public void testSubQueryWithMinFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String hql = "SELECT e FROM BasicEntity e WHERE e.id = " +
							"(SELECT min(e1.id) FROM BasicEntity e1 WHERE e1.data = :data)";

					List<BasicEntity> results = session
							.createQuery( hql, BasicEntity.class )
							.setParameter( "data", "e1" )
							.getResultList();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ).getId(), is( 1 ) );
					assertThat( results.get( 0 ).getData(), is( "e1" ) );
				}
		);
	}

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					BasicEntity entity1 = new BasicEntity( 1, "e1" );
					BasicEntity entity2 = new BasicEntity( 2, "e2" );
					BasicEntity entity3 = new BasicEntity( 3, "e1" );
					session.persist( entity1 );
					session.persist( entity2 );
					session.persist( entity3 );
				}
		);
	}

	@AfterAll
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete BasicEntity" ).executeUpdate()
		);
	}
}
