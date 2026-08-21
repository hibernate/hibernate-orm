/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = CriteriaMixedTreeExceptionTest.TestEntity.class)
@SessionFactory
@JiraKey("HHH-19380")
public class CriteriaMixedTreeExceptionTest {

	@Test
	public void mixingCriteriaRootsGivesHelpfulException(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteriaQuery = builder.createQuery( TestEntity.class );
			final var rootFromOtherQuery = criteriaQuery.from( TestEntity.class );
			final var criteriaUpdate = builder.createCriteriaUpdate( TestEntity.class );
			final var updateRoot = criteriaUpdate.from( TestEntity.class );
			criteriaUpdate.set( updateRoot.get( "name" ), "updated" );
			criteriaUpdate.where( builder.equal( rootFromOtherQuery.get( "name" ), "initial" ) );

			final var exception = assertThrows(
					IllegalStateException.class,
					() -> session.createMutationQuery( criteriaUpdate ).executeUpdate()
			);
			assertTrue( exception.getMessage()
					.contains( ".name'" ) );
		} );
	}

	@Entity(name = "CriteriaMixedTreeExceptionTestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;
	}
}
