/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 * @author Nathan Xu
 */
@JiraKey( value = "HHH-14201" )
@DomainModel( annotatedClasses = { JoinOrderTest.EntityA.class, JoinOrderTest.EntityB.class, JoinOrderTest.EntityC.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class JoinOrderTest {

	@Test
	public void testJoinOrder(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SQLStatementInspector sqlStatementInspector = scope.getCollectingStatementInspector();
			sqlStatementInspector.clear();

			final String hql = "select 1"
					+ " from EntityA a"
					+ " join EntityB b on b.a = a "
					+ " join a.c c on c.b = b";
			session.createQuery( hql ).getResultList();

			assertThat( sqlStatementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlStatementInspector.getSqlQueries().get( 0 ) ).matches( "^.+(?: join EntityB ).+(?: join EntityC ).+$" );
		} );
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		int id;

		@ManyToOne
		EntityC c;
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		int id;

		@ManyToOne
		EntityA a;
	}

	@Entity(name = "EntityC")
	public static class EntityC {
		@Id
		int id;

		@ManyToOne
		EntityB b;
	}
}
