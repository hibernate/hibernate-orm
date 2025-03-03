/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.time.LocalDate;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = EntityOfBasics.class )
public class NamedParameterInSelectAndWhereTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics e1 = new EntityOfBasics( 1 );
			final EntityOfBasics e2 = new EntityOfBasics( 2 );
			e2.setTheLocalDate( LocalDate.EPOCH );
			e2.setTheInt( 1 );
			session.persist( e1 );
			session.persist( e2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from EntityOfBasics" ).executeUpdate() );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16137" )
	public void testSelectAndWhere(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1, session.createQuery(
						"select :param from EntityOfBasics e where e.id > :param",
						Integer.class
				)
				.setParameter( "param", 1 )
				.getSingleResult() ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16137" )
	public void testSelectAndWhereIsNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1, session.createQuery(
						"select :param from EntityOfBasics be where :param is null or be.id > :param",
						Integer.class
				).setParameter( "param", 1 )
				.getSingleResult() ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16305" )
	@SkipForDialect( dialectClass = PostgreSQLDialect.class, reason = "PostgreSQL doesn't support parameters as arguments for timestampdiff" )
	@SkipForDialect( dialectClass = CockroachDialect.class, reason = "CockroachDB doesn't support parameters as arguments for timestampdiff" )
	public void testSelectFunctionAndWhere(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 0, session.createQuery(
						"select timestampdiff(year, e.theLocalDate, :date) from EntityOfBasics e where e.theLocalDate <= :date",
						Long.class
				)
				.setParameter( "date", LocalDate.EPOCH )
				.getSingleResult() ) );
	}
}
