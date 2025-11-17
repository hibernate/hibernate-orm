/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = BasicEntity.class)
@SessionFactory
@JiraKey("HHH-15901")
public class EmptyPredicateTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			BasicEntity entity = new BasicEntity( 1, "test" );
			session.persist( entity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BasicEntity" ).executeUpdate() );
	}

	@Test
	public void testEmptyPredicateArray(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<BasicEntity> query = cb.createQuery( BasicEntity.class );
			final Root<BasicEntity> root = query.from( BasicEntity.class );
			query.select( root ).where( cb.equal( cb.literal( 1 ), 2 ) );
			query.where( new Predicate[] {} ); // this should remove previous restrictions
			assertEquals( 1, session.createQuery( query ).getResultList().size() );
		} );
	}
}
