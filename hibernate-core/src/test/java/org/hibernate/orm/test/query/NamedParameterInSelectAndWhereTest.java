/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = BasicEntity.class)
@JiraKey("HHH-16137")
public class NamedParameterInSelectAndWhereTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			BasicEntity be1 = new BasicEntity( 1, "one" );
			session.persist( be1 );
			BasicEntity be2 = new BasicEntity( 2, "two" );
			session.persist( be2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testSelectAndWhere(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals(
				1,
				session.createQuery( "SELECT :param FROM BasicEntity be WHERE be.id > :param", Integer.class )
						.setParameter( "param", 1 )
						.getSingleResult()
		) );
	}

	@Test
	public void testSelectAndWhereIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals(
				1,
				session.createQuery(
								"SELECT :param FROM BasicEntity be WHERE :param is null or be.id > :param",
								Integer.class
						)
						.setParameter( "param", 1 )
						.getSingleResult()
		) );
	}
}
