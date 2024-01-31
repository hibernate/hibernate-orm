/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.hql;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17332" )
public class InSubqueryPredicateAnonymousTupleTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "test" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testSimpleInSubqueryPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String result = session.createQuery(
					"select sub.data from (select e.id id, e.data data from BasicEntity e) sub" +
							" where sub.data in (select e.data from BasicEntity e)",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "test" );
		} );
	}

	@Test
	public void testTupleInSubqueryPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// note : without cast(sub.data as string) Sybase jTDS fails with "TDS Protocol error: Invalid TDS data type"
			final String result = session.createQuery(
					"select cast(sub.data as string) from (select e.id id, e.data data from BasicEntity e) sub" +
							" where (sub.id, sub.data) in (select e.id, e.data from BasicEntity e)",
					String.class
			).getSingleResult();
			assertThat( result ).isEqualTo( "test" );
		} );
	}
}
