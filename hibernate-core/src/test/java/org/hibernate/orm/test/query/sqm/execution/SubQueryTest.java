/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.sqm.execution;

import java.util.List;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class SubQueryTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				BasicEntity.class
		};
	}

	@BeforeAll
	public void prepareData() {
		sessionFactoryScope().inTransaction(
				session -> {
					BasicEntity entity1 = new BasicEntity( 1, "e1" );
					BasicEntity entity2 = new BasicEntity( 2, "e2" );
					BasicEntity entity3 = new BasicEntity( 3, "e1" );
					session.save( entity1 );
					session.save( entity2 );
					session.save( entity3 );
				}
		);
	}

	@Test
	public void testSubQueryWithMaxFunction() {
		sessionFactoryScope().inTransaction(
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
	public void testSubQueryWithMinFunction() {
		sessionFactoryScope().inTransaction(
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
}
