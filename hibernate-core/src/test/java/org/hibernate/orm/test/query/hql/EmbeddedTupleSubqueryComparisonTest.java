/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.domain.gambit.EmbeddedIdEntity;
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
@DomainModel( annotatedClasses = EmbeddedIdEntity.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17283" )
public class EmbeddedTupleSubqueryComparisonTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EmbeddedIdEntity entity = new EmbeddedIdEntity();
			entity.setId( new EmbeddedIdEntity.EmbeddedIdEntityId( 1, "1" ) );
			session.persist( entity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from EmbeddedIdEntity" ).executeUpdate() );
	}

	@Test
	public void testIdSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EmbeddedIdEntity result = session.createQuery(
					"from EmbeddedIdEntity e1 where e1.id in " +
							"(select e2.id from EmbeddedIdEntity e2)",
					EmbeddedIdEntity.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	@Test
	public void testJoinSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EmbeddedIdEntity result = session.createQuery(
					"from EmbeddedIdEntity e1 where e1.id in " +
							"(select e2_id from EmbeddedIdEntity e2 join e2.id as e2_id)",
					EmbeddedIdEntity.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	@Test
	public void testExplicitTupleSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EmbeddedIdEntity result = session.createQuery(
					"from EmbeddedIdEntity e1 where (e1.id.value1, e1.id.value2) in " +
							"(select e2.id.value1, e2.id.value2 from EmbeddedIdEntity e2)",
					EmbeddedIdEntity.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}

	@Test
	public void testEntitySubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EmbeddedIdEntity result = session.createQuery(
					"from EmbeddedIdEntity e1 where e1 in " +
							"(select e2 from EmbeddedIdEntity e2)",
					EmbeddedIdEntity.class
			).getSingleResult();
			assertThat( result ).isNotNull();
		} );
	}
}
