/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
@Jira( "https://hibernate.atlassian.net/browse/HHH-18450" )
public class SingleSelectionArrayResultTest {
	@Test
	public void testArrayResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select 1",
					Object[].class
			).getSingleResult() ).containsExactly( 1 );
			assertThat( session.createQuery(
					"select cast(1 as integer)",
					Object[].class
			).getSingleResult() ).containsExactly( 1 );
			assertThat( session.createSelectionQuery(
					"select id from BasicEntity",
					Object[].class
			).getSingleResult() ).containsExactly( 1 );
			assertThat( session.createSelectionQuery(
					"select cast(id as integer) from BasicEntity",
					Object[].class
			).getSingleResult() ).containsExactly( 1 );
			assertThat( session.createSelectionQuery(
					"select ?1",
					Object[].class
			).setParameter( 1, 1 ).getSingleResult() ).containsExactly( 1 );
			assertThat( session.createQuery(
					"select cast(:p1 as integer)",
					Object[].class
			).setParameter( "p1", 1 ).getSingleResult() ).containsExactly( 1 );
		} );
	}

	@Test
	public void testNormalResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select 1",
					Object.class
			).getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
			assertThat( session.createQuery(
					"select cast(1 as integer)",
					Object.class
			).getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
			assertThat( session.createSelectionQuery(
					"select id from BasicEntity",
					Object.class
			).getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
			assertThat( session.createSelectionQuery(
					"select cast(id as integer) from BasicEntity",
					Object.class
			).getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
			assertThat( session.createSelectionQuery(
					"select ?1",
					Object.class
			).setParameter( 1, 1 ).getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
			assertThat( session.createQuery(
					"select cast(:p1 as integer)",
					Object.class
			).setParameter( "p1", 1 ).getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "entity_1" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
